package is.hello.buruberi.bluetooth.stacks.android;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import is.hello.buruberi.bluetooth.errors.ConnectionStateException;
import is.hello.buruberi.bluetooth.errors.GattException;
import is.hello.buruberi.bluetooth.stacks.GattCharacteristic;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.GattService;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;
import is.hello.buruberi.bluetooth.stacks.android.GattDispatcher.CharacteristicReadListener;
import is.hello.buruberi.bluetooth.stacks.android.GattDispatcher.CharacteristicWriteListener;
import is.hello.buruberi.bluetooth.stacks.android.GattDispatcher.DescriptorWriteListener;
import is.hello.buruberi.bluetooth.stacks.android.NativeGattPeripheral.ConnectedOnSubscribe;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.util.Operation;
import rx.Observable;
import rx.Subscriber;

class NativeGattCharacteristic implements GattCharacteristic {
    /*package*/ final BluetoothGattCharacteristic wrappedCharacteristic;
    private final NativeGattService service;
    private final NativeGattPeripheral peripheral;

    private final LoggerFacade logger;
    private final GattDispatcher gattDispatcher;

    /*package*/ @Nullable PacketListener packetListener;

    /*package*/ NativeGattCharacteristic(@NonNull BluetoothGattCharacteristic characteristic,
                                         @NonNull NativeGattService service,
                                         @NonNull NativeGattPeripheral peripheral) {
        this.wrappedCharacteristic = characteristic;
        this.service = service;
        this.peripheral = peripheral;

        this.logger = peripheral.getStack().getLogger();
        this.gattDispatcher = peripheral.gattDispatcher;
    }

    @NonNull
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

    @Override
    public void setPacketListener(@NonNull PacketListener packetListener) {
        this.packetListener = packetListener;
    }

    @Override
    @NonNull
    public Observable<byte[]> read(@NonNull final OperationTimeout timeout) {
        return peripheral.createObservable(new ConnectedOnSubscribe<byte[]>(peripheral) {
            @Override
            public void onSubscribe(@NonNull BluetoothGatt gatt,
                                    @NonNull final Subscriber<? super byte[]> subscriber) {
                logger.info(GattPeripheral.LOG_TAG, "Reading characteristic " + getUuid());

                final Runnable onDisconnect =
                        peripheral.addTimeoutDisconnectListener(subscriber, timeout);
                peripheral.setupTimeout(Operation.READ,
                                        timeout, subscriber, onDisconnect);

                gattDispatcher.characteristicRead = new CharacteristicReadListener() {
                    @Override
                    public void onCharacteristicRead(@NonNull BluetoothGatt gatt,
                                                     @NonNull BluetoothGattCharacteristic characteristic,
                                                     int status) {
                        timeout.unschedule();

                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            final byte[] value = characteristic.getValue();
                            subscriber.onNext(value);
                            subscriber.onCompleted();
                        } else {
                            logger.error(GattPeripheral.LOG_TAG,
                                         "Could not read characteristic. " +
                                                 GattException.statusToString(status), null);
                            subscriber.onError(new GattException(status,
                                                                 Operation.READ));
                        }

                        gattDispatcher.characteristicRead = null;
                        peripheral.removeDisconnectListener(onDisconnect);
                    }
                };
                if (gatt.readCharacteristic(wrappedCharacteristic)) {
                    timeout.schedule();
                } else {
                    gattDispatcher.characteristicRead = null;
                    peripheral.removeDisconnectListener(onDisconnect);

                    subscriber.onError(new GattException(BluetoothGatt.GATT_FAILURE,
                                                         Operation.READ));
                }
            }
        });
    }

    @NonNull
    @Override
    public Observable<UUID> enableNotification(@NonNull final UUID descriptor,
                                               @NonNull final OperationTimeout timeout) {
        return peripheral.createObservable(new ConnectedOnSubscribe<UUID>(peripheral) {
            @Override
            public void onSubscribe(@NonNull BluetoothGatt gatt,
                                    @NonNull final Subscriber<? super UUID> subscriber) {
                logger.info(GattPeripheral.LOG_TAG, "Subscribing to " + descriptor);

                if (gatt.setCharacteristicNotification(wrappedCharacteristic, true)) {
                    final Runnable onDisconnect =
                            peripheral.addTimeoutDisconnectListener(subscriber, timeout);
                    peripheral.setupTimeout(Operation.ENABLE_NOTIFICATION,
                                            timeout, subscriber, onDisconnect);

                    gattDispatcher.descriptorWrite = new DescriptorWriteListener() {
                        @Override
                        public void onDescriptorWrite(@NonNull BluetoothGatt gatt,
                                                      @NonNull BluetoothGattDescriptor descriptor,
                                                      int status) {
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
                                                                     Operation.ENABLE_NOTIFICATION));
                            }

                            gattDispatcher.descriptorWrite = null;
                            peripheral.removeDisconnectListener(onDisconnect);
                        }
                    };

                    final BluetoothGattDescriptor descriptorToWrite =
                            wrappedCharacteristic.getDescriptor(descriptor);
                    descriptorToWrite.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    if (gatt.writeDescriptor(descriptorToWrite)) {
                        timeout.schedule();
                    } else {
                        gattDispatcher.descriptorWrite = null;
                        peripheral.removeDisconnectListener(onDisconnect);

                        subscriber.onError(new GattException(BluetoothGatt.GATT_FAILURE,
                                                             Operation.ENABLE_NOTIFICATION));
                    }
                } else {
                    subscriber.onError(new GattException(BluetoothGatt.GATT_WRITE_NOT_PERMITTED,
                                                         Operation.ENABLE_NOTIFICATION));
                }
            }
        });
    }

    @NonNull
    @Override
    public Observable<UUID> disableNotification(@NonNull final UUID descriptor,
                                                @NonNull final OperationTimeout timeout) {
        return peripheral.createObservable(new ConnectedOnSubscribe<UUID>(peripheral) {
            @Override
            public void onSubscribe(@NonNull BluetoothGatt gatt,
                                    @NonNull final Subscriber<? super UUID> subscriber) {
                logger.info(GattPeripheral.LOG_TAG, "Unsubscribing from " + getUuid());

                final Runnable onDisconnect = peripheral.addTimeoutDisconnectListener(subscriber,
                                                                                      timeout);
                peripheral.setupTimeout(Operation.ENABLE_NOTIFICATION,
                                        timeout, subscriber, onDisconnect);

                gattDispatcher.descriptorWrite = new DescriptorWriteListener() {
                    @Override
                    public void onDescriptorWrite(@NonNull BluetoothGatt gatt,
                                                  @NonNull BluetoothGattDescriptor descriptor,
                                                  int status) {
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
                                                                     Operation.DISABLE_NOTIFICATION));
                            }
                        } else {
                            subscriber.onError(new GattException(status,
                                                                 Operation.DISABLE_NOTIFICATION));
                        }

                        peripheral.removeDisconnectListener(onDisconnect);
                        gattDispatcher.descriptorWrite = null;
                    }
                };

                final BluetoothGattDescriptor descriptorToWrite =
                        wrappedCharacteristic.getDescriptor(descriptor);
                descriptorToWrite.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                if (gatt.writeDescriptor(descriptorToWrite)) {
                    timeout.schedule();
                } else {
                    peripheral.removeDisconnectListener(onDisconnect);
                    gattDispatcher.descriptorWrite = null;

                    subscriber.onError(new GattException(BluetoothGatt.GATT_WRITE_NOT_PERMITTED,
                                                         Operation.DISABLE_NOTIFICATION));
                }
            }
        });
    }

    @NonNull
    @Override
    public Observable<Void> write(@NonNull final GattPeripheral.WriteType writeType,
                                  @NonNull final byte[] payload,
                                  @NonNull final OperationTimeout timeout) {
        if (payload.length > PACKET_LENGTH) {
            return Observable.error(new IllegalArgumentException("Payload length " + payload.length +
                                                                         " greater than " + PACKET_LENGTH));
        }

        return peripheral.createObservable(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> subscriber) {
                if (peripheral.getConnectionStatus() != GattPeripheral.STATUS_CONNECTED ||
                        peripheral.gatt == null) {
                    subscriber.onError(new ConnectionStateException());
                    return;
                }

                final Runnable onDisconnect = peripheral.addTimeoutDisconnectListener(subscriber,
                                                                                         timeout);
                peripheral.setupTimeout(Operation.ENABLE_NOTIFICATION,
                                        timeout, subscriber, onDisconnect);

                gattDispatcher.characteristicWrite = new CharacteristicWriteListener() {
                    @Override
                    public void onCharacteristicWrite(@NonNull BluetoothGatt gatt,
                                                      @NonNull BluetoothGattCharacteristic characteristic,
                                                      int status) {
                        timeout.unschedule();

                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            logger.error(GattPeripheral.LOG_TAG, "Could not write command " +
                                    getUuid() + ", " + GattException.statusToString(status), null);
                            subscriber.onError(new GattException(status,
                                                                 Operation.WRITE_COMMAND));
                        } else {
                            subscriber.onNext(null);
                            subscriber.onCompleted();
                        }

                        peripheral.removeDisconnectListener(onDisconnect);
                        gattDispatcher.characteristicWrite = null;
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
                    peripheral.removeDisconnectListener(onDisconnect);
                    gattDispatcher.characteristicWrite = null;

                    subscriber.onError(new GattException(BluetoothGatt.GATT_WRITE_NOT_PERMITTED,
                                                         Operation.WRITE_COMMAND));
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
