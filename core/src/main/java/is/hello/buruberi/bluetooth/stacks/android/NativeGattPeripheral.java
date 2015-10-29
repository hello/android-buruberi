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
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import is.hello.buruberi.bluetooth.errors.BondException;
import is.hello.buruberi.bluetooth.errors.ConnectionStateException;
import is.hello.buruberi.bluetooth.errors.GattException;
import is.hello.buruberi.bluetooth.errors.OperationTimeoutException;
import is.hello.buruberi.bluetooth.errors.ServiceDiscoveryException;
import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;
import is.hello.buruberi.bluetooth.stacks.PeripheralService;
import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.util.Rx;
import is.hello.buruberi.util.SerialQueue;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action2;
import rx.functions.Action3;
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
    @VisibleForTesting final @NonNull BluetoothDevice bluetoothDevice;
    private final int scannedRssi;
    private final @NonNull AdvertisingData advertisingData;
    @VisibleForTesting final GattDispatcher gattDispatcher;
    @VisibleForTesting final SerialQueue serialQueue;

    @VisibleForTesting boolean suspendDisconnectBroadcasts = false;
    @VisibleForTesting @Nullable BluetoothGatt gatt;
    private @Nullable Subscription bluetoothStateSubscription;

    NativeGattPeripheral(final @NonNull NativeBluetoothStack stack,
                         @NonNull BluetoothDevice bluetoothDevice,
                         int scannedRssi,
                         @NonNull AdvertisingData advertisingData) {
        this.stack = stack;
        this.logger = stack.getLogger();
        this.bluetoothDevice = bluetoothDevice;
        this.scannedRssi = scannedRssi;
        this.advertisingData = advertisingData;

        this.gattDispatcher = new GattDispatcher(logger);
        gattDispatcher.addConnectionStateListener(new GattDispatcher.ConnectionStateListener() {
            @Override
            public void onConnectionStateChanged(@NonNull BluetoothGatt gatt, int gattStatus,
                                                 int newState, @NonNull Action0 removeThisListener) {
                if (newState == STATUS_DISCONNECTED) {
                    NativeGattPeripheral.this.closeGatt(gatt);

                    if (!suspendDisconnectBroadcasts) {
                        Intent disconnect = new Intent(ACTION_DISCONNECTED);
                        disconnect.putExtra(EXTRA_NAME, NativeGattPeripheral.this.getName());
                        disconnect.putExtra(EXTRA_ADDRESS, NativeGattPeripheral.this.getAddress());
                        LocalBroadcastManager.getInstance(stack.applicationContext)
                                .sendBroadcast(disconnect);
                    }
                }
            }
        });
        
        this.serialQueue = new SerialQueue();
    }


    //region Attributes

    @NonNull
    @Override
    public OperationTimeout createOperationTimeout(@NonNull String name, long duration, @NonNull TimeUnit timeUnit) {
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
            Log.i(LOG_TAG, "Closing gatt layer");

            gatt.close();
            if (gatt == this.gatt) {
                gattDispatcher.dispatchDisconnect();
                this.gatt = null;

                if (bluetoothStateSubscription != null) {
                    bluetoothStateSubscription.unsubscribe();
                    this.bluetoothStateSubscription = null;
                }
            }
        }
    }

    @NonNull
    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public Observable<GattPeripheral> connect(final @NonNull OperationTimeout timeout) {
        return createObservable(new Observable.OnSubscribe<GattPeripheral>() {
            @Override
            public void call(final Subscriber<? super GattPeripheral> subscriber) {
                if (getConnectionStatus() == STATUS_CONNECTED) {
                    logger.warn(LOG_TAG, "Redundant call to connect(), ignoring.");

                    subscriber.onNext(NativeGattPeripheral.this);
                    subscriber.onCompleted();
                    return;
                } else if (getConnectionStatus() == STATUS_CONNECTING || getConnectionStatus() == STATUS_DISCONNECTING) {
                    subscriber.onError(new ConnectionStateException("Peripheral is changing connection status."));
                    return;
                }

                final AtomicBoolean hasRetried = new AtomicBoolean(false);
                final GattDispatcher.ConnectionStateListener listener = new GattDispatcher.ConnectionStateListener() {
                    @Override
                    public void onConnectionStateChanged(final @NonNull BluetoothGatt gatt, int gattStatus, int newState, @NonNull Action0 removeThisListener) {
                        // The first connection attempt made after a user has power cycled their radio,
                        // or the connection to a device is unexpectedly lost, will seemingly fail 100%
                        // of the time. The error code varies by manufacturer. Retrying silently resolves
                        // the issue.
                        if (GattException.isRecoverableConnectError(gattStatus) && !hasRetried.get()) {
                            logger.warn(LOG_TAG, "First connection attempt failed due to stack error, retrying.");

                            hasRetried.set(true);
                            gatt.close();
                            NativeGattPeripheral.this.gatt = bluetoothDevice.connectGatt(stack.applicationContext, false, gattDispatcher);
                            //noinspection ConstantConditions
                            if (gatt != null) {
                                timeout.reschedule();
                            } else {
                                timeout.unschedule();

                                NativeGattPeripheral.this.suspendDisconnectBroadcasts = false;
                                subscriber.onError(new GattException(GattException.GATT_INTERNAL_ERROR, GattException.Operation.CONNECT));
                            }
                        } else if (gattStatus != BluetoothGatt.GATT_SUCCESS) {
                            timeout.unschedule();

                            logger.error(LOG_TAG, "Could not connect. " + GattException.statusToString(gattStatus), null);
                            NativeGattPeripheral.this.suspendDisconnectBroadcasts = false;
                            subscriber.onError(new GattException(gattStatus, GattException.Operation.CONNECT));

                            removeThisListener.call();
                        } else if (newState == STATUS_CONNECTED) {
                            timeout.unschedule();

                            logger.info(LOG_TAG, "Connected " + NativeGattPeripheral.this.toString());

                            Observable<Intent> bluetoothStateObserver = Rx.fromBroadcast(stack.applicationContext, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
                            NativeGattPeripheral.this.bluetoothStateSubscription = bluetoothStateObserver.subscribe(new Subscriber<Intent>() {
                                @Override
                                public void onCompleted() {
                                }

                                @Override
                                public void onError(Throwable e) {
                                    logger.error(LOG_TAG, "Bluetooth state observation error", e);
                                }

                                @Override
                                public void onNext(Intent intent) {
                                    logger.info(LOG_TAG, "User disabled bluetooth radio, abandoning connection");

                                    if (!stack.getAdapter().isEnabled()) {
                                        gatt.disconnect();
                                    }
                                }
                            });

                            NativeGattPeripheral.this.suspendDisconnectBroadcasts = false;
                            subscriber.onNext(NativeGattPeripheral.this);
                            subscriber.onCompleted();

                            removeThisListener.call();
                        }
                    }
                };
                gattDispatcher.addConnectionStateListener(listener);

                timeout.setTimeoutAction(new Action0() {
                    @Override
                    public void call() {
                        timeout.unschedule();

                        gattDispatcher.removeConnectionStateListener(listener);
                        if (bluetoothStateSubscription != null) {
                            bluetoothStateSubscription.unsubscribe();
                            NativeGattPeripheral.this.bluetoothStateSubscription = null;
                        }

                        NativeGattPeripheral.this.suspendDisconnectBroadcasts = false;
                        subscriber.onError(new OperationTimeoutException(OperationTimeoutException.Operation.CONNECT));
                    }
                }, stack.scheduler);

                logger.info(LOG_TAG, "Connecting " + NativeGattPeripheral.this.toString());

                if (gatt != null) {
                    if (gatt.connect()) {
                        NativeGattPeripheral.this.suspendDisconnectBroadcasts = true;
                        timeout.schedule();
                    } else {
                        subscriber.onError(new GattException(GattException.GATT_INTERNAL_ERROR, GattException.Operation.CONNECT));
                    }
                } else {
                    NativeGattPeripheral.this.gatt = bluetoothDevice.connectGatt(stack.applicationContext, false, gattDispatcher);
                    if (gatt != null) {
                        NativeGattPeripheral.this.suspendDisconnectBroadcasts = true;
                        timeout.schedule();
                    } else {
                        subscriber.onError(new GattException(GattException.GATT_INTERNAL_ERROR, GattException.Operation.CONNECT));
                    }
                }
            }
        });
    }

    @NonNull
    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public Observable<GattPeripheral> disconnect() {
        return createObservable(new Observable.OnSubscribe<GattPeripheral>() {
            @Override
            public void call(final Subscriber<? super GattPeripheral> subscriber) {
                int connectionStatus = getConnectionStatus();
                if (connectionStatus == STATUS_DISCONNECTED || connectionStatus == STATUS_DISCONNECTING || gatt == null) {
                    subscriber.onNext(NativeGattPeripheral.this);
                    subscriber.onCompleted();
                    return;
                } else if (connectionStatus == STATUS_CONNECTING) {
                    subscriber.onError(new ConnectionStateException("Peripheral is connecting"));
                    return;
                }

                GattDispatcher.ConnectionStateListener listener = new GattDispatcher.ConnectionStateListener() {
                    @Override
                    public void onConnectionStateChanged(@NonNull BluetoothGatt gatt, int gattStatus, int newState, @NonNull Action0 removeThisListener) {
                        if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            if (gattStatus != BluetoothGatt.GATT_SUCCESS) {
                                logger.info(LOG_TAG, "Could not disconnect " + NativeGattPeripheral.this.toString() + "; " + GattException.statusToString(gattStatus));

                                subscriber.onError(new GattException(gattStatus, GattException.Operation.DISCONNECT));
                            } else {
                                logger.info(LOG_TAG, "Disconnected " + NativeGattPeripheral.this.toString());

                                subscriber.onNext(NativeGattPeripheral.this);
                                subscriber.onCompleted();
                            }

                            removeThisListener.call();
                        }
                    }
                };
                gattDispatcher.addConnectionStateListener(listener);

                logger.info(LOG_TAG, "Disconnecting " + NativeGattPeripheral.this.toString());

                gatt.disconnect();
            }
        });
    }

    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public @ConnectivityStatus int getConnectionStatus() {
        if (gatt != null) {
            @ConnectivityStatus int status = stack.bluetoothManager.getConnectionState(bluetoothDevice, BluetoothProfile.GATT);
            return status;
        } else {
            return STATUS_DISCONNECTED;
        }
    }

    //endregion


    //region Internal

    private <T> Observable<T> createObservable(@NonNull Observable.OnSubscribe<T> onSubscribe) {
        return Rx.serialize(stack.newConfiguredObservable(onSubscribe), serialQueue);
    }
    
    private <T> void setupTimeout(@NonNull final OperationTimeoutException.Operation operation,
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
        return Rx.fromBroadcast(stack.applicationContext, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
                .subscribeOn(stack.scheduler)
                .filter(new Func1<Intent, Boolean>() {
                    @Override
                    public Boolean call(Intent intent) {
                        BluetoothDevice bondedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        return (bondedDevice != null && bondedDevice.getAddress().equals(bluetoothDevice.getAddress()));
                    }
                });
    }

    private boolean tryCreateBond() {
        try {
            Method method = bluetoothDevice.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(bluetoothDevice, (Object[]) null);
            return true;
        } catch (Exception e) {
            logger.error(LOG_TAG, "Could not invoke `createBond` on BluetoothDevice.", e);
            return false;
        }
    }

    private boolean tryRemoveBond() {
        try {
            Method method = bluetoothDevice.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(bluetoothDevice, (Object[]) null);
            return true;
        } catch (Exception e) {
            logger.error(LOG_TAG, "Could not invoke `createBond` on BluetoothDevice.", e);
            return false;
        }
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
                if (NativeGattPeripheral.this.getBondStatus() == BOND_BONDED) {
                    logger.info(GattPeripheral.LOG_TAG, "Device already bonded, skipping.");

                    subscriber.onNext(NativeGattPeripheral.this);
                    subscriber.onCompleted();
                    return;
                }

                Subscription subscription = createBondReceiver().subscribe(new Subscriber<Intent>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        subscriber.onError(e);
                    }

                    @Override
                    public void onNext(Intent intent) {
                        int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                        int previousState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                        logger.info(GattPeripheral.LOG_TAG, "Bond status changed from " + BondException.getBondStateString(previousState) +
                                " to " + BondException.getBondStateString(state));

                        if (state == BluetoothDevice.BOND_BONDED) {
                            logger.info(LOG_TAG, "Bonding succeeded.");

                            subscriber.onNext(NativeGattPeripheral.this);
                            subscriber.onCompleted();

                            unsubscribe();
                        } else if (state == BluetoothDevice.ERROR || state == BOND_NONE && previousState == BOND_CHANGING) {
                            int reason = intent.getIntExtra(BondException.EXTRA_REASON, BondException.REASON_UNKNOWN_FAILURE);
                            logger.error(LOG_TAG, "Bonding failed for reason " + BondException.getReasonString(reason), null);
                            subscriber.onError(new BondException(reason));

                            unsubscribe();
                        }
                    }
                });

                if (!tryCreateBond()) {
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
                if (NativeGattPeripheral.this.getBondStatus() != BOND_BONDED) {
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

                        int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                        int previousState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

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

                            int reason = intent.getIntExtra(BondException.EXTRA_REASON,
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
                        if (NativeGattPeripheral.this.getBondStatus() == BOND_NONE) {
                            subscriber.onNext(NativeGattPeripheral.this);
                            subscriber.onCompleted();
                        } else {
                            subscriber.onError(new OperationTimeoutException(OperationTimeoutException.Operation.REMOVE_BOND));
                        }
                    }
                }, stack.getScheduler());

                if (!tryRemoveBond()) {
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
    public Observable<Map<UUID, PeripheralService>> discoverServices(final @NonNull OperationTimeout timeout) {
        Observable<Map<UUID, PeripheralService>> discoverServices = createObservable(new Observable.OnSubscribe<Map<UUID, PeripheralService>>() {
            @Override
            public void call(final Subscriber<? super Map<UUID, PeripheralService>> subscriber) {
                if (getConnectionStatus() != STATUS_CONNECTED || gatt == null) {
                    subscriber.onError(new ConnectionStateException());
                    return;
                }

                final Action0 onDisconnect = gattDispatcher.addTimeoutDisconnectListener(subscriber, timeout);
                setupTimeout(OperationTimeoutException.Operation.DISCOVER_SERVICES, timeout, subscriber, onDisconnect);

                gattDispatcher.onServicesDiscovered = new Action2<BluetoothGatt, Integer>() {
                    @Override
                    public void call(BluetoothGatt gatt, Integer status) {
                        timeout.unschedule();

                        gattDispatcher.removeDisconnectListener(onDisconnect);

                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            final Map<UUID, PeripheralService> services =
                                    NativePeripheralService.wrap(gatt.getServices(),
                                                                 NativeGattPeripheral.this);
                            subscriber.onNext(services);
                            subscriber.onCompleted();

                            gattDispatcher.onServicesDiscovered = null;
                        } else {
                            logger.error(LOG_TAG, "Could not discover services. " + GattException.statusToString(status), null);

                            subscriber.onError(new GattException(status, GattException.Operation.DISCOVER_SERVICES));

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
    public Observable<PeripheralService> discoverService(final @NonNull UUID serviceIdentifier, @NonNull OperationTimeout timeout) {
        return discoverServices(timeout).flatMap(new Func1<Map<UUID, PeripheralService>, Observable<? extends PeripheralService>>() {
            @Override
            public Observable<? extends PeripheralService> call(Map<UUID, PeripheralService> services) {
                PeripheralService service = services.get(serviceIdentifier);
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

    private @NonNull BluetoothGattService getGattService(@NonNull PeripheralService onPeripheralService) {
        return ((NativePeripheralService) onPeripheralService).service;
    }

    @NonNull
    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public Observable<UUID> enableNotification(final @NonNull PeripheralService onPeripheralService,
                                               final @NonNull UUID characteristicIdentifier,
                                               final @NonNull UUID descriptorIdentifier,
                                               final @NonNull OperationTimeout timeout) {
        return createObservable(new Observable.OnSubscribe<UUID>() {
            @Override
            public void call(final Subscriber<? super UUID> subscriber) {
                if (getConnectionStatus() != STATUS_CONNECTED || gatt == null) {
                    subscriber.onError(new ConnectionStateException());
                    return;
                }

                logger.info(GattPeripheral.LOG_TAG, "Subscribing to " + characteristicIdentifier);

                final BluetoothGattService service = getGattService(onPeripheralService);
                final BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicIdentifier);
                if (gatt.setCharacteristicNotification(characteristic, true)) {
                    final Action0 onDisconnect = gattDispatcher.addTimeoutDisconnectListener(subscriber, timeout);
                    setupTimeout(OperationTimeoutException.Operation.ENABLE_NOTIFICATION, timeout, subscriber, onDisconnect);

                    gattDispatcher.onDescriptorWrite = new Action3<BluetoothGatt, BluetoothGattDescriptor, Integer>() {
                        @Override
                        public void call(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, Integer status) {
                            if (!Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                                return;
                            }

                            timeout.unschedule();

                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                subscriber.onNext(characteristicIdentifier);
                                subscriber.onCompleted();
                            } else {
                                logger.error(LOG_TAG, "Could not subscribe to characteristic. " + GattException.statusToString(status), null);
                                subscriber.onError(new GattException(status, GattException.Operation.ENABLE_NOTIFICATION));
                            }

                            gattDispatcher.onDescriptorWrite = null;
                            gattDispatcher.removeDisconnectListener(onDisconnect);
                        }
                    };

                    BluetoothGattDescriptor descriptorToWrite = characteristic.getDescriptor(descriptorIdentifier);
                    descriptorToWrite.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    if (gatt.writeDescriptor(descriptorToWrite)) {
                        timeout.schedule();
                    } else {
                        gattDispatcher.onDescriptorWrite = null;
                        gattDispatcher.removeDisconnectListener(onDisconnect);

                        subscriber.onError(new GattException(BluetoothGatt.GATT_FAILURE, GattException.Operation.ENABLE_NOTIFICATION));
                    }
                } else {
                    subscriber.onError(new GattException(BluetoothGatt.GATT_WRITE_NOT_PERMITTED, GattException.Operation.ENABLE_NOTIFICATION));
                }
            }
        });
    }

    @NonNull
    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public Observable<UUID> disableNotification(final @NonNull PeripheralService onPeripheralService,
                                                final @NonNull UUID characteristicIdentifier,
                                                final @NonNull UUID descriptorIdentifier,
                                                final @NonNull OperationTimeout timeout) {
        return createObservable(new Observable.OnSubscribe<UUID>() {
            @Override
            public void call(final Subscriber<? super UUID> subscriber) {
                if (getConnectionStatus() != STATUS_CONNECTED || gatt == null) {
                    subscriber.onError(new ConnectionStateException());
                    return;
                }

                logger.info(GattPeripheral.LOG_TAG, "Unsubscribing from " + characteristicIdentifier);

                final BluetoothGattService service = getGattService(onPeripheralService);
                final BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicIdentifier);

                final Action0 onDisconnect = gattDispatcher.addTimeoutDisconnectListener(subscriber, timeout);
                setupTimeout(OperationTimeoutException.Operation.ENABLE_NOTIFICATION, timeout, subscriber, onDisconnect);

                gattDispatcher.onDescriptorWrite = new Action3<BluetoothGatt, BluetoothGattDescriptor, Integer>() {
                    @Override
                    public void call(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, Integer status) {
                        if (!Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                            return;
                        }

                        timeout.unschedule();

                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            if (gatt.setCharacteristicNotification(characteristic, false)) {
                                subscriber.onNext(characteristicIdentifier);
                                subscriber.onCompleted();
                            } else {
                                logger.error(LOG_TAG, "Could not unsubscribe from characteristic. " + GattException.statusToString(status), null);
                                subscriber.onError(new GattException(BluetoothGatt.GATT_FAILURE, GattException.Operation.DISABLE_NOTIFICATION));
                            }
                        } else {
                            subscriber.onError(new GattException(status, GattException.Operation.DISABLE_NOTIFICATION));
                        }

                        gattDispatcher.removeDisconnectListener(onDisconnect);
                        gattDispatcher.onDescriptorWrite = null;
                    }
                };

                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorIdentifier);
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                if (gatt.writeDescriptor(descriptor)) {
                    timeout.schedule();
                } else {
                    gattDispatcher.removeDisconnectListener(onDisconnect);
                    gattDispatcher.onDescriptorWrite = null;

                    subscriber.onError(new GattException(BluetoothGatt.GATT_WRITE_NOT_PERMITTED, GattException.Operation.DISABLE_NOTIFICATION));
                }
            }
        });
    }

    @NonNull
    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public Observable<Void> writeCommand(final @NonNull PeripheralService onPeripheralService,
                                         final @NonNull UUID identifier,
                                         final @NonNull WriteType writeType,
                                         final @NonNull byte[] payload,
                                         final @NonNull OperationTimeout timeout) {
        if (payload.length > PacketHandler.PACKET_LENGTH) {
            return Observable.error(new IllegalArgumentException("Payload length " + payload.length +
                    " greater than " + PacketHandler.PACKET_LENGTH));
        }

        return createObservable(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> subscriber) {
                if (getConnectionStatus() != STATUS_CONNECTED || gatt == null) {
                    subscriber.onError(new ConnectionStateException());
                    return;
                }

                final Action0 onDisconnect = gattDispatcher.addTimeoutDisconnectListener(subscriber, timeout);
                setupTimeout(OperationTimeoutException.Operation.ENABLE_NOTIFICATION, timeout, subscriber, onDisconnect);

                gattDispatcher.onCharacteristicWrite = new Action3<BluetoothGatt, BluetoothGattCharacteristic, Integer>() {
                    @Override
                    public void call(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, Integer status) {
                        timeout.unschedule();

                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            logger.error(LOG_TAG, "Could not write command " + identifier + ", " + GattException.statusToString(status), null);
                            subscriber.onError(new GattException(status, GattException.Operation.WRITE_COMMAND));
                        } else {
                            subscriber.onNext(null);
                            subscriber.onCompleted();
                        }

                        gattDispatcher.removeDisconnectListener(onDisconnect);
                        gattDispatcher.onCharacteristicWrite = null;
                    }
                };

                BluetoothGattService service = getGattService(onPeripheralService);
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(identifier);
                // Looks like write type might need to be specified for some phones. See
                // <http://stackoverflow.com/questions/25888817/android-bluetooth-status-133-in-oncharacteristicwrite>
                characteristic.setWriteType(writeType.value);
                characteristic.setValue(payload);
                if (gatt.writeCharacteristic(characteristic)) {
                    timeout.schedule();
                } else {
                    gattDispatcher.removeDisconnectListener(onDisconnect);
                    gattDispatcher.onCharacteristicWrite = null;

                    subscriber.onError(new GattException(BluetoothGatt.GATT_WRITE_NOT_PERMITTED, GattException.Operation.WRITE_COMMAND));
                }
            }
        });
    }

    @Override
    public void setPacketHandler(@Nullable PacketHandler dataHandler) {
        gattDispatcher.packetHandler = dataHandler;
    }

    //endregion


    @Override
    public String toString() {
        return "{AndroidPeripheral " +
                "name=" + getName() +
                ", address=" + getAddress() +
                ", connectionStatus=" + getConnectionStatus() +
                ", bondStatus=" + getBondStatus() +
                ", scannedRssi=" + getScanTimeRssi() +
                '}';
    }
}
