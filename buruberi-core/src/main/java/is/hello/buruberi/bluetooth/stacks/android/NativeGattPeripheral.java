/*
 * Copyright 2015 Hello Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package is.hello.buruberi.bluetooth.stacks.android;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.LocalBroadcastManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import is.hello.buruberi.bluetooth.errors.BondException;
import is.hello.buruberi.bluetooth.errors.ConnectionStateException;
import is.hello.buruberi.bluetooth.errors.GattException;
import is.hello.buruberi.bluetooth.errors.OperationTimeoutException;
import is.hello.buruberi.bluetooth.errors.ServiceDiscoveryException;
import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.GattCharacteristic;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.GattService;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;
import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.util.Rx;
import is.hello.buruberi.util.SerialQueue;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action2;
import rx.functions.Func1;

public class NativeGattPeripheral implements GattPeripheral {
    /**
     * How long to delay response after a successful service discovery.
     * <p>
     * Settled on 5 seconds after experimenting with Jackson. Idea for delay from
     * <a href="https://code.google.com/p/android/issues/detail?id=58381">here</a>.
     */
    private static final int SERVICES_DELAY_S = 5;

    private final @NonNull NativeBluetoothStack stack;
    private final @NonNull LoggerFacade logger;
    private final SerialQueue serialQueue;

    @VisibleForTesting final @NonNull BluetoothDevice bluetoothDevice;
    private final int scannedRssi;
    private final @NonNull AdvertisingData advertisingData;

    /*package*/ final GattDispatcher gattDispatcher;
    private final DisconnectForwarder disconnectForwarder;

    /*package*/ @Nullable BluetoothGatt gatt;
    private @Nullable BroadcastReceiver bluetoothStateReceiver;

    NativeGattPeripheral(final @NonNull NativeBluetoothStack stack,
                         @NonNull BluetoothDevice bluetoothDevice,
                         int scannedRssi,
                         @NonNull AdvertisingData advertisingData) {
        this.stack = stack;
        this.logger = stack.getLogger();
        this.serialQueue = new SerialQueue();

        this.bluetoothDevice = bluetoothDevice;
        this.scannedRssi = scannedRssi;
        this.advertisingData = advertisingData;

        this.gattDispatcher = new GattDispatcher(logger);
        this.disconnectForwarder = new DisconnectForwarder();
        gattDispatcher.addConnectionListener(disconnectForwarder);
    }


    //region Attributes

    @NonNull
    @Override
    public OperationTimeout createOperationTimeout(@NonNull String name,
                                                   long duration,
                                                   @NonNull TimeUnit timeUnit) {
        return new SchedulerOperationTimeout(name, duration, timeUnit, logger);
    }

    @Override
    public int getScanTimeRssi() {
        return scannedRssi;
    }

    @Override
    public String getAddress() {
        return bluetoothDevice.getAddress();
    }

    @Override
    public String getName() {
        return bluetoothDevice.getName();
    }

    @NonNull
    @Override
    public AdvertisingData getAdvertisingData() {
        return advertisingData;
    }

    @Override
    @NonNull
    public BluetoothStack getStack() {
        return stack;
    }

    //endregion


    //region Connectivity

    void closeGatt(@Nullable BluetoothGatt gatt) {
        if (gatt != null) {
            logger.info(LOG_TAG, "Closing gatt layer");

            gatt.close();
            if (gatt == this.gatt) {
                gattDispatcher.dispatchDisconnect();
                this.gatt = null;

                stopObservingBluetoothState();
            }
        }
    }

    int getTransportFromConnectFlags(@ConnectFlags final int flags) {
        if ((flags & CONNECT_FLAG_TRANSPORT_AUTO) == CONNECT_FLAG_TRANSPORT_AUTO) {
            return BluetoothDeviceCompat.TRANSPORT_AUTO;
        } else if ((flags & CONNECT_FLAG_TRANSPORT_BREDR) == CONNECT_FLAG_TRANSPORT_BREDR) {
            return BluetoothDeviceCompat.TRANSPORT_BREDR;
        } else if ((flags & CONNECT_FLAG_TRANSPORT_LE) == CONNECT_FLAG_TRANSPORT_LE) {
            return BluetoothDeviceCompat.TRANSPORT_LE;
        } else {
            logger.warn(LOG_TAG, "ConnectFlags missing transport mask " +
                    Integer.toHexString(flags));
            return BluetoothDeviceCompat.TRANSPORT_AUTO;
        }
    }

    void startObservingBluetoothState() {
        this.bluetoothStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                logger.info(LOG_TAG, "User disabled bluetooth radio, abandoning connection");

                if (!stack.getAdapter().isEnabled() && gatt != null) {
                    gatt.disconnect();
                }
            }
        };
        final IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        stack.applicationContext.registerReceiver(bluetoothStateReceiver, filter);
    }

    void stopObservingBluetoothState() {
        if (bluetoothStateReceiver != null) {
            stack.applicationContext.unregisterReceiver(bluetoothStateReceiver);
            this.bluetoothStateReceiver = null;
        }
    }

    @NonNull
    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public Observable<GattPeripheral> connect(@ConnectFlags final int flags,
                                              @NonNull final OperationTimeout timeout) {
        return createObservable(new Observable.OnSubscribe<GattPeripheral>() {
            @Override
            public void call(final Subscriber<? super GattPeripheral> subscriber) {
                if (getConnectionStatus() == STATUS_CONNECTED) {
                    logger.warn(LOG_TAG, "Redundant call to connect(), ignoring.");

                    subscriber.onNext(NativeGattPeripheral.this);
                    subscriber.onCompleted();
                    return;
                } else if (getConnectionStatus() == STATUS_CONNECTING ||
                        getConnectionStatus() == STATUS_DISCONNECTING) {
                    subscriber.onError(new ConnectionStateException("Peripheral is changing connection status."));
                    return;
                }

                final boolean autoConnect = ((flags & CONNECT_FLAG_WAIT_AVAILABLE) == CONNECT_FLAG_WAIT_AVAILABLE);
                final int transport = getTransportFromConnectFlags(flags);

                final GattDispatcher.ConnectionListener listener = new GattDispatcher.ConnectionListener() {
                    boolean hasRetried = false;

                    @Override
                    boolean onConnected(@NonNull final BluetoothGatt gatt, int status) {
                        timeout.unschedule();

                        logger.info(LOG_TAG, "Connected " + NativeGattPeripheral.this.toString());

                        startObservingBluetoothState();

                        disconnectForwarder.setEnabled(true);
                        subscriber.onNext(NativeGattPeripheral.this);
                        subscriber.onCompleted();

                        return false;
                    }

                    @Override
                    boolean onError(@NonNull BluetoothGatt gatt, int status, int state) {
                        // The first connection attempt made after a user has power cycled their radio,
                        // or the connection to a device is unexpectedly lost, will seemingly fail 100%
                        // of the time. The error code varies by manufacturer. Retrying silently resolves
                        // the issue.
                        if (GattException.isRecoverableConnectError(status) && !hasRetried) {
                            logger.warn(LOG_TAG, "First connection attempt failed due to stack error, retrying.");

                            this.hasRetried = true;
                            gatt.close();
                            NativeGattPeripheral.this.gatt =
                                    BluetoothDeviceCompat.connectGatt(bluetoothDevice,
                                                                      stack.applicationContext,
                                                                      autoConnect,
                                                                      gattDispatcher,
                                                                      transport);
                            if (NativeGattPeripheral.this.gatt != null) {
                                timeout.reschedule();
                            } else {
                                timeout.unschedule();

                                disconnectForwarder.setEnabled(true);
                                subscriber.onError(new GattException(GattException.GATT_INTERNAL_ERROR,
                                                                     GattException.Operation.CONNECT));
                            }

                            return true;
                        } else {
                            timeout.unschedule();

                            logger.error(LOG_TAG,
                                         "Could not connect. " + GattException.statusToString(status),
                                         null);
                            disconnectForwarder.setEnabled(true);
                            subscriber.onError(new GattException(status,
                                                                 GattException.Operation.CONNECT));

                            return false;
                        }
                    }
                };
                gattDispatcher.addConnectionListener(listener);

                timeout.setTimeoutAction(new Action0() {
                    @Override
                    public void call() {
                        timeout.unschedule();

                        gattDispatcher.removeConnectionListener(listener);
                        stopObservingBluetoothState();

                        disconnectForwarder.setEnabled(true);
                        subscriber.onError(new OperationTimeoutException(OperationTimeoutException.Operation.CONNECT));
                    }
                }, stack.scheduler);

                logger.info(LOG_TAG, "Connecting " + NativeGattPeripheral.this.toString());

                if (gatt != null) {
                    if (gatt.connect()) {
                        disconnectForwarder.setEnabled(false);
                        timeout.schedule();
                    } else {
                        subscriber.onError(new GattException(GattException.GATT_INTERNAL_ERROR,
                                                             GattException.Operation.CONNECT));
                    }
                } else {
                    NativeGattPeripheral.this.gatt =
                            BluetoothDeviceCompat.connectGatt(bluetoothDevice,
                                                              stack.applicationContext,
                                                              autoConnect,
                                                              gattDispatcher,
                                                              transport);
                    if (gatt != null) {
                        disconnectForwarder.setEnabled(false);
                        timeout.schedule();
                    } else {
                        subscriber.onError(new GattException(GattException.GATT_INTERNAL_ERROR,
                                                             GattException.Operation.CONNECT));
                    }
                }
            }
        });
    }

    @NonNull
    @Override
    @Deprecated
    public Observable<GattPeripheral> connect(@NonNull OperationTimeout timeout) {
        return connect(CONNECT_FLAG_DEFAULTS, timeout);
    }

    @NonNull
    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public Observable<GattPeripheral> disconnect() {
        return createObservable(new Observable.OnSubscribe<GattPeripheral>() {
            @Override
            public void call(final Subscriber<? super GattPeripheral> subscriber) {
                final int connectionStatus = getConnectionStatus();
                if (connectionStatus == STATUS_DISCONNECTED ||
                        connectionStatus == STATUS_DISCONNECTING || gatt == null) {
                    subscriber.onNext(NativeGattPeripheral.this);
                    subscriber.onCompleted();
                    return;
                } else if (connectionStatus == STATUS_CONNECTING) {
                    subscriber.onError(new ConnectionStateException("Peripheral is connecting"));
                    return;
                }

                final GattDispatcher.ConnectionListener listener = new GattDispatcher.ConnectionListener() {
                    @Override
                    boolean onDisconnected(@NonNull BluetoothGatt gatt, int status) {
                        logger.info(LOG_TAG, "Disconnected " + NativeGattPeripheral.this.toString());

                        stopObservingBluetoothState();
                        subscriber.onNext(NativeGattPeripheral.this);
                        subscriber.onCompleted();

                        return false;
                    }

                    @Override
                    boolean onError(@NonNull BluetoothGatt gatt, int status, int state) {
                        logger.info(LOG_TAG, "Could not disconnect " +
                                NativeGattPeripheral.this.toString() + "; " +
                                GattException.statusToString(status));

                        subscriber.onError(new GattException(status, GattException.Operation.DISCONNECT));

                        return false;
                    }
                };
                gattDispatcher.addConnectionListener(listener);

                logger.info(LOG_TAG, "Disconnecting " + NativeGattPeripheral.this.toString());

                gatt.disconnect();
            }
        });
    }

    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public @ConnectivityStatus int getConnectionStatus() {
        if (gatt != null) {
            final @ConnectivityStatus int status =
                    stack.bluetoothManager.getConnectionState(bluetoothDevice,
                                                              BluetoothProfile.GATT);
            return status;
        } else {
            return STATUS_DISCONNECTED;
        }
    }

    //endregion


    //region Internal

    /*package*/ <T> Observable<T> createObservable(@NonNull Observable.OnSubscribe<T> onSubscribe) {
        return Rx.serialize(stack.newConfiguredObservable(onSubscribe), serialQueue);
    }

    /*package*/ <T> void setupTimeout(@NonNull final OperationTimeoutException.Operation operation,
                                      @NonNull final OperationTimeout timeout,
                                      @NonNull final Subscriber<T> subscriber,
                                      @Nullable final Action0 disconnectListener) {
        timeout.setTimeoutAction(new Action0() {
            @Override
            public void call() {
                switch (operation) {
                    case DISCOVER_SERVICES:
                        gattDispatcher.onServicesDiscovered = null;
                        break;

                    case ENABLE_NOTIFICATION:
                        gattDispatcher.onDescriptorWrite = null;
                        break;

                    case DISABLE_NOTIFICATION:
                        gattDispatcher.onDescriptorWrite = null;
                        break;

                    case WRITE_COMMAND:
                        gattDispatcher.onCharacteristicWrite = null;
                        break;
                }
                if (disconnectListener != null) {
                    gattDispatcher.removeDisconnectListener(disconnectListener);
                }

                disconnect().subscribe(new Subscriber<GattPeripheral>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        subscriber.onError(new OperationTimeoutException(operation, e));
                    }

                    @Override
                    public void onNext(GattPeripheral gattPeripheral) {
                        subscriber.onError(new OperationTimeoutException(operation));
                    }
                });

                timeout.unschedule();
            }
        }, stack.scheduler);
    }

    //endregion


    //region Bonding

    @RequiresPermission(Manifest.permission.BLUETOOTH)
    private Observable<Intent> createBondReceiver() {
        return Rx.fromBroadcast(stack.applicationContext,
                                new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
                 .subscribeOn(stack.scheduler)
                 .filter(new Func1<Intent, Boolean>() {
                     @Override
                     public Boolean call(Intent intent) {
                         final BluetoothDevice bondedDevice =
                                 intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                         return (bondedDevice != null &&
                                 bondedDevice.getAddress().equals(bluetoothDevice.getAddress()));
                     }
                 });
    }

    @NonNull
    @Override
    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    })
    public Observable<GattPeripheral> createBond() {
        return createObservable(new Observable.OnSubscribe<GattPeripheral>() {
            @Override
            public void call(final Subscriber<? super GattPeripheral> subscriber) {
                if (getBondStatus() == BOND_BONDED) {
                    logger.info(GattPeripheral.LOG_TAG, "Device already bonded, skipping.");

                    subscriber.onNext(NativeGattPeripheral.this);
                    subscriber.onCompleted();
                    return;
                }

                final Subscription subscription = createBondReceiver().subscribe(new Subscriber<Intent>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        subscriber.onError(e);
                    }

                    @Override
                    public void onNext(Intent intent) {
                        final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                                                             BluetoothDevice.ERROR);
                        final int previousState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                                                                     BluetoothDevice.ERROR);

                        logger.info(GattPeripheral.LOG_TAG, "Bond status changed from " +
                                BondException.getBondStateString(previousState) +
                                " to " + BondException.getBondStateString(state));

                        if (state == BluetoothDevice.BOND_BONDED) {
                            logger.info(LOG_TAG, "Bonding succeeded.");

                            subscriber.onNext(NativeGattPeripheral.this);
                            subscriber.onCompleted();

                            unsubscribe();
                        } else if (state == BluetoothDevice.ERROR || state == BOND_NONE &&
                                previousState == BOND_CHANGING) {
                            final int reason = intent.getIntExtra(BondException.EXTRA_REASON,
                                                                  BondException.REASON_UNKNOWN_FAILURE);
                            logger.error(LOG_TAG, "Bonding failed for reason " +
                                    BondException.getReasonString(reason), null);
                            subscriber.onError(new BondException(reason));

                            unsubscribe();
                        }
                    }
                });

                if (!BluetoothDeviceCompat.createBond(bluetoothDevice)) {
                    subscription.unsubscribe();
                    subscriber.onError(new BondException(BondException.REASON_ANDROID_API_CHANGED));
                }
            }
        });
    }

    @NonNull
    @Override
    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    })
    public Observable<GattPeripheral> removeBond(final @NonNull OperationTimeout timeout) {
        return createObservable(new Observable.OnSubscribe<GattPeripheral>() {
            @Override
            public void call(final Subscriber<? super GattPeripheral> subscriber) {
                if (getBondStatus() != BOND_BONDED) {
                    logger.info(GattPeripheral.LOG_TAG, "Device not bonded, skipping.");

                    subscriber.onNext(NativeGattPeripheral.this);
                    subscriber.onCompleted();
                    return;
                }

                final Subscription subscription = createBondReceiver().subscribe(new Subscriber<Intent>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        subscriber.onError(e);
                    }

                    @Override
                    public void onNext(Intent intent) {
                        timeout.reschedule();

                        final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                                                             BluetoothDevice.ERROR);
                        final int previousState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                                                                     BluetoothDevice.ERROR);

                        logger.info(GattPeripheral.LOG_TAG, "Bond status changed from " +
                                BondException.getBondStateString(previousState) +
                                " to " + BondException.getBondStateString(state));

                        if (state == BluetoothDevice.BOND_NONE) {
                            logger.info(LOG_TAG, "Removing bond succeeded.");
                            timeout.unschedule();

                            subscriber.onNext(NativeGattPeripheral.this);
                            subscriber.onCompleted();

                            unsubscribe();
                        } else if (state == BluetoothDevice.ERROR) {
                            timeout.unschedule();

                            final int reason = intent.getIntExtra(BondException.EXTRA_REASON,
                                                                  BondException.REASON_UNKNOWN_FAILURE);
                            logger.error(LOG_TAG, "Removing bond failed for reason " +
                                    BondException.getReasonString(reason), null);
                            subscriber.onError(new BondException(reason));

                            unsubscribe();
                        }
                    }
                });

                timeout.setTimeoutAction(new Action0() {
                    @Override
                    public void call() {
                        subscription.unsubscribe();

                        // This can happen in Lollipop
                        if (getBondStatus() == BOND_NONE) {
                            subscriber.onNext(NativeGattPeripheral.this);
                            subscriber.onCompleted();
                        } else {
                            subscriber.onError(new OperationTimeoutException(OperationTimeoutException.Operation.REMOVE_BOND));
                        }
                    }
                }, stack.getScheduler());

                if (!BluetoothDeviceCompat.removeBond(bluetoothDevice)) {
                    subscription.unsubscribe();
                    subscriber.onError(new BondException(BondException.REASON_ANDROID_API_CHANGED));
                } else {
                    timeout.schedule();
                }
            }
        });
    }

    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public @BondStatus int getBondStatus() {
        @BondStatus int bondStatus = bluetoothDevice.getBondState();
        return bondStatus;
    }

    //endregion


    //region Discovering Services

    @NonNull
    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public Observable<Map<UUID, GattService>> discoverServices(final @NonNull OperationTimeout timeout) {
        final Observable<Map<UUID, GattService>> discoverServices = createObservable(new Observable.OnSubscribe<Map<UUID, GattService>>() {
            @Override
            public void call(final Subscriber<? super Map<UUID, GattService>> subscriber) {
                if (getConnectionStatus() != STATUS_CONNECTED || gatt == null) {
                    subscriber.onError(new ConnectionStateException());
                    return;
                }

                final Action0 onDisconnect = gattDispatcher.addTimeoutDisconnectListener(subscriber,
                                                                                         timeout);
                setupTimeout(OperationTimeoutException.Operation.DISCOVER_SERVICES,
                             timeout, subscriber, onDisconnect);

                gattDispatcher.onServicesDiscovered = new Action2<BluetoothGatt, Integer>() {
                    @Override
                    public void call(BluetoothGatt gatt, Integer status) {
                        timeout.unschedule();

                        gattDispatcher.removeDisconnectListener(onDisconnect);

                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            final Map<UUID, GattService> services =
                                    NativeGattService.wrap(gatt.getServices(),
                                                           NativeGattPeripheral.this);
                            subscriber.onNext(services);
                            subscriber.onCompleted();

                            gattDispatcher.onServicesDiscovered = null;
                        } else {
                            logger.error(LOG_TAG, "Could not discover services. " +
                                    GattException.statusToString(status), null);

                            subscriber.onError(new GattException(status,
                                                                 GattException.Operation.DISCOVER_SERVICES));

                            gattDispatcher.onServicesDiscovered = null;
                        }
                    }
                };

                if (gatt.discoverServices()) {
                    timeout.schedule();
                } else {
                    gattDispatcher.onServicesDiscovered = null;

                    subscriber.onError(new ServiceDiscoveryException());
                }
            }
        });


        // See <https://code.google.com/p/android/issues/detail?id=58381>
        return discoverServices.delay(SERVICES_DELAY_S, TimeUnit.SECONDS, stack.getScheduler());
    }

    @NonNull
    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public Observable<GattService> discoverService(final @NonNull UUID serviceIdentifier, @NonNull OperationTimeout timeout) {
        return discoverServices(timeout).flatMap(new Func1<Map<UUID, GattService>, Observable<? extends GattService>>() {
            @Override
            public Observable<? extends GattService> call(Map<UUID, GattService> services) {
                final GattService service = services.get(serviceIdentifier);
                if (service != null) {
                    return Observable.just(service);
                } else {
                    return Observable.error(new ServiceDiscoveryException());
                }
            }
        });
    }


    //endregion


    //region Characteristics

    @NonNull
    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public Observable<UUID> enableNotification(final @NonNull GattService onGattService,
                                               final @NonNull UUID characteristicIdentifier,
                                               final @NonNull UUID descriptorIdentifier,
                                               final @NonNull OperationTimeout timeout) {
        final GattCharacteristic characteristic =
                onGattService.getCharacteristic(characteristicIdentifier);
        if (characteristic != null) {
            return characteristic.enableNotification(descriptorIdentifier, timeout);
        } else {
            return Observable.error(new IllegalArgumentException("Unknown characteristic " + characteristicIdentifier));
        }
    }

    @NonNull
    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public Observable<UUID> disableNotification(final @NonNull GattService onGattService,
                                                final @NonNull UUID characteristicIdentifier,
                                                final @NonNull UUID descriptorIdentifier,
                                                final @NonNull OperationTimeout timeout) {
        final GattCharacteristic characteristic =
                onGattService.getCharacteristic(characteristicIdentifier);
        if (characteristic != null) {
            return characteristic.disableNotification(descriptorIdentifier, timeout);
        } else {
            return Observable.error(new IllegalArgumentException("Unknown characteristic " + characteristicIdentifier));
        }
    }

    @NonNull
    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public Observable<Void> writeCommand(final @NonNull GattService onGattService,
                                         final @NonNull UUID identifier,
                                         final @NonNull WriteType writeType,
                                         final @NonNull byte[] payload,
                                         final @NonNull OperationTimeout timeout) {
        final GattCharacteristic characteristic = onGattService.getCharacteristic(identifier);
        if (characteristic != null) {
            return characteristic.write(writeType, payload, timeout);
        } else {
            return Observable.error(new IllegalArgumentException("Unknown characteristic " + identifier));
        }
    }

    @Override
    public void setPacketHandler(@Nullable PacketHandler dataHandler) {
        gattDispatcher.packetHandler = dataHandler;
    }

    //endregion


    @Override
    public String toString() {
        return "{NativeGattPeripheral " +
                "name=" + getName() +
                ", address=" + getAddress() +
                ", connectionStatus=" + getConnectionStatus() +
                ", bondStatus=" + getBondStatus() +
                ", scannedRssi=" + getScanTimeRssi() +
                '}';
    }


    class DisconnectForwarder extends GattDispatcher.ConnectionListener {
        private boolean enabled = true;

        void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        private void broadcast() {
            closeGatt(gatt);

            if (enabled) {
                final Intent disconnect = new Intent(ACTION_DISCONNECTED);
                disconnect.putExtra(EXTRA_NAME, getName());
                disconnect.putExtra(EXTRA_ADDRESS, getAddress());
                LocalBroadcastManager.getInstance(stack.applicationContext)
                                     .sendBroadcast(disconnect);
            }
        }

        @Override
        boolean onDisconnected(@NonNull BluetoothGatt gatt, int status) {
            broadcast();
            return true;
        }

        @Override
        boolean onError(@NonNull BluetoothGatt gatt, int status, int state) {
            if (state == STATUS_DISCONNECTED) {
                broadcast();
            }
            return true;
        }
    }
}
