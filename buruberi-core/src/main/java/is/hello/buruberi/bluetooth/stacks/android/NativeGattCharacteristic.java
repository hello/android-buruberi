package is.hello.buruberi.bluetooth.stacks.android;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import is.hello.buruberi.bluetooth.errors.ConnectionStateException;
import is.hello.buruberi.bluetooth.errors.GattException;
import is.hello.buruberi.bluetooth.errors.OperationTimeoutException;
import is.hello.buruberi.bluetooth.stacks.GattCharacteristic;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.GattService;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action3;

class NativeGattCharacteristic implements GattCharacteristic {
    private final BluetoothGattCharacteristic wrappedCharacteristic;
    private final NativeGattService service;
    private final NativeGattPeripheral peripheral;

    private final LoggerFacade logger;
    private final GattDispatcher gattDispatcher;

    NativeGattCharacteristic(@NonNull BluetoothGattCharacteristic wrappedCharacteristic,
                             @NonNull NativeGattService service,
                             @NonNull NativeGattPeripheral peripheral) {
        this.wrappedCharacteristic = wrappedCharacteristic;
        this.service = service;
        this.peripheral = peripheral;

        this.logger = peripheral.getStack().getLogger();
        this.gattDispatcher = peripheral.gattDispatcher;
    }

    @Override
    public UUID getUuid() {
        return wrappedCharacteristic.getUuid();
    }

    @Override
    @Properties
    public int getProperties() {
        final @Properties int properties = wrappedCharacteristic.getProperties();
        return properties;
    }

    @Override
    @Permissions
    public int getPermissions() {
        final @Permissions int permissions = wrappedCharacteristic.getPermissions();
        return permissions;
    }

    @NonNull
    @Override
    public GattService getService() {
        return service;
    }

    @NonNull
    @Override
    public List<UUID> getDescriptors() {
        final List<UUID> identifiers = new ArrayList<>();
        for (final BluetoothGattDescriptor descriptor : wrappedCharacteristic.getDescriptors()) {
            identifiers.add(descriptor.getUuid());
        }
        return identifiers;
    }

    @Permissions
    @Override
    public int getDescriptorPermissions(@NonNull UUID identifier) {
        final BluetoothGattDescriptor descriptor = wrappedCharacteristic.getDescriptor(identifier);
        if (descriptor != null) {
            final @Permissions int permissions = descriptor.getPermissions();
            return permissions;
        } else {
            return PERMISSION_NULL;
        }
    }

    @NonNull
    @Override
    public Observable<UUID> enableNotification(@NonNull final UUID descriptor,
                                               @NonNull final OperationTimeout timeout) {
        return peripheral.createObservable(new Observable.OnSubscribe<UUID>() {
            @Override
            public void call(final Subscriber<? super UUID> subscriber) {
                if (peripheral.getConnectionStatus() != GattPeripheral.STATUS_CONNECTED ||
                        peripheral.gatt == null) {
                    subscriber.onError(new ConnectionStateException());
                    return;
                }

                logger.info(GattPeripheral.LOG_TAG, "Subscribing to " + descriptor);

                if (peripheral.gatt.setCharacteristicNotification(wrappedCharacteristic, true)) {
                    final Action0 onDisconnect =
                            gattDispatcher.addTimeoutDisconnectListener(subscriber, timeout);
                    peripheral.setupTimeout(OperationTimeoutException.Operation.ENABLE_NOTIFICATION,
                                            timeout, subscriber, onDisconnect);

                    gattDispatcher.onDescriptorWrite = new Action3<BluetoothGatt, BluetoothGattDescriptor, Integer>() {
                        @Override
                        public void call(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, Integer status) {
                            if (!Arrays.equals(descriptor.getValue(),
                                               BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                                return;
                            }

                            timeout.unschedule();

                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                subscriber.onNext(getUuid());
                                subscriber.onCompleted();
                            } else {
                                logger.error(GattPeripheral.LOG_TAG,
                                             "Could not subscribe to characteristic. " +
                                                     GattException.statusToString(status), null);
                                subscriber.onError(new GattException(status,
                                                                     GattException.Operation.ENABLE_NOTIFICATION));
                            }

                            gattDispatcher.onDescriptorWrite = null;
                            gattDispatcher.removeDisconnectListener(onDisconnect);
                        }
                    };

                    final BluetoothGattDescriptor descriptorToWrite =
                            wrappedCharacteristic.getDescriptor(descriptor);
                    descriptorToWrite.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    if (peripheral.gatt.writeDescriptor(descriptorToWrite)) {
                        timeout.schedule();
                    } else {
                        gattDispatcher.onDescriptorWrite = null;
                        gattDispatcher.removeDisconnectListener(onDisconnect);

                        subscriber.onError(new GattException(BluetoothGatt.GATT_FAILURE,
                                                             GattException.Operation.ENABLE_NOTIFICATION));
                    }
                } else {
                    subscriber.onError(new GattException(BluetoothGatt.GATT_WRITE_NOT_PERMITTED,
                                                         GattException.Operation.ENABLE_NOTIFICATION));
                }
            }
        });
    }

    @NonNull
    @Override
    public Observable<UUID> disableNotification(@NonNull final UUID descriptor,
                                                @NonNull final OperationTimeout timeout) {
        return peripheral.createObservable(new Observable.OnSubscribe<UUID>() {
            @Override
            public void call(final Subscriber<? super UUID> subscriber) {
                if (peripheral.getConnectionStatus() != GattPeripheral.STATUS_CONNECTED ||
                        peripheral.gatt == null) {
                    subscriber.onError(new ConnectionStateException());
                    return;
                }

                logger.info(GattPeripheral.LOG_TAG, "Unsubscribing from " + getUuid());

                final Action0 onDisconnect = gattDispatcher.addTimeoutDisconnectListener(subscriber,
                                                                                         timeout);
                peripheral.setupTimeout(OperationTimeoutException.Operation.ENABLE_NOTIFICATION,
                                        timeout, subscriber, onDisconnect);

                gattDispatcher.onDescriptorWrite = new Action3<BluetoothGatt, BluetoothGattDescriptor, Integer>() {
                    @Override
                    public void call(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, Integer status) {
                        if (!Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                            return;
                        }

                        timeout.unschedule();

                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            if (gatt.setCharacteristicNotification(wrappedCharacteristic, false)) {
                                subscriber.onNext(getUuid());
                                subscriber.onCompleted();
                            } else {
                                logger.error(GattPeripheral.LOG_TAG,
                                             "Could not unsubscribe from characteristic. " +
                                                     GattException.statusToString(status), null);
                                subscriber.onError(new GattException(BluetoothGatt.GATT_FAILURE,
                                                                     GattException.Operation.DISABLE_NOTIFICATION));
                            }
                        } else {
                            subscriber.onError(new GattException(status,
                                                                 GattException.Operation.DISABLE_NOTIFICATION));
                        }

                        gattDispatcher.removeDisconnectListener(onDisconnect);
                        gattDispatcher.onDescriptorWrite = null;
                    }
                };

                final BluetoothGattDescriptor descriptorToWrite =
                        wrappedCharacteristic.getDescriptor(descriptor);
                descriptorToWrite.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                if (peripheral.gatt.writeDescriptor(descriptorToWrite)) {
                    timeout.schedule();
                } else {
                    gattDispatcher.removeDisconnectListener(onDisconnect);
                    gattDispatcher.onDescriptorWrite = null;

                    subscriber.onError(new GattException(BluetoothGatt.GATT_WRITE_NOT_PERMITTED,
                                                         GattException.Operation.DISABLE_NOTIFICATION));
                }
            }
        });
    }

    @NonNull
    @Override
    public Observable<Void> write(@NonNull final GattPeripheral.WriteType writeType,
                                  @NonNull final byte[] payload,
                                  @NonNull final OperationTimeout timeout) {
        if (payload.length > GattPeripheral.PacketHandler.PACKET_LENGTH) {
            return Observable.error(new IllegalArgumentException("Payload length " + payload.length +
                                                                         " greater than " + GattPeripheral.PacketHandler.PACKET_LENGTH));
        }

        return peripheral.createObservable(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> subscriber) {
                if (peripheral.getConnectionStatus() != GattPeripheral.STATUS_CONNECTED ||
                        peripheral.gatt == null) {
                    subscriber.onError(new ConnectionStateException());
                    return;
                }

                final Action0 onDisconnect = gattDispatcher.addTimeoutDisconnectListener(subscriber,
                                                                                         timeout);
                peripheral.setupTimeout(OperationTimeoutException.Operation.ENABLE_NOTIFICATION,
                                        timeout, subscriber, onDisconnect);

                gattDispatcher.onCharacteristicWrite = new Action3<BluetoothGatt, BluetoothGattCharacteristic, Integer>() {
                    @Override
                    public void call(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, Integer status) {
                        timeout.unschedule();

                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            logger.error(GattPeripheral.LOG_TAG, "Could not write command " +
                                    getUuid() + ", " + GattException.statusToString(status), null);
                            subscriber.onError(new GattException(status,
                                                                 GattException.Operation.WRITE_COMMAND));
                        } else {
                            subscriber.onNext(null);
                            subscriber.onCompleted();
                        }

                        gattDispatcher.removeDisconnectListener(onDisconnect);
                        gattDispatcher.onCharacteristicWrite = null;
                    }
                };

                final BluetoothGattCharacteristic characteristic =
                        service.wrappedService.getCharacteristic(getUuid());
                // Looks like write type might need to be specified for some phones. See
                // <http://stackoverflow.com/questions/25888817/android-bluetooth-status-133-in-oncharacteristicwrite>
                characteristic.setWriteType(writeType.value);
                characteristic.setValue(payload);
                if (peripheral.gatt.writeCharacteristic(characteristic)) {
                    timeout.schedule();
                } else {
                    gattDispatcher.removeDisconnectListener(onDisconnect);
                    gattDispatcher.onCharacteristicWrite = null;

                    subscriber.onError(new GattException(BluetoothGatt.GATT_WRITE_NOT_PERMITTED,
                                                         GattException.Operation.WRITE_COMMAND));
                }
            }
        });
    }

    @Override
    public String toString() {
        return "NativeCharacteristic{" +
                "uuid=" + getUuid() +
                '}';
    }
}
