package is.hello.buruberi.bluetooth.stacks.android;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import is.hello.buruberi.bluetooth.errors.BluetoothConnectionLostError;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action2;
import rx.functions.Action3;

class GattDispatcher extends BluetoothGattCallback {
    private final LoggerFacade logger;
    private final List<ConnectionStateListener> connectionStateListeners = new ArrayList<>();
    private final List<Action0> disconnectListeners = new ArrayList<>();
    private final Handler dispatcher = new Handler(Looper.getMainLooper());

    @Nullable GattPeripheral.PacketHandler packetHandler;
    @Nullable Action2<BluetoothGatt, Integer> onServicesDiscovered;
    @Nullable Action3<BluetoothGatt, BluetoothGattCharacteristic, Integer> onCharacteristicWrite;
    @Nullable Action3<BluetoothGatt, BluetoothGattDescriptor, Integer> onDescriptorWrite;


    GattDispatcher(@NonNull LoggerFacade logger) {
        this.logger = logger;
    }

    void addConnectionStateListener(@NonNull ConnectionStateListener changeHandler) {
        connectionStateListeners.add(changeHandler);
    }

    void removeConnectionStateListener(@NonNull ConnectionStateListener changeHandler) {
        connectionStateListeners.remove(changeHandler);
    }

    Action0 addDisconnectListener(@NonNull Action0 disconnectListener) {
        disconnectListeners.add(disconnectListener);
        return disconnectListener;
    }

    <T> Action0 addTimeoutDisconnectListener(final @NonNull Subscriber<T> subscriber, final @NonNull OperationTimeout timeout) {
        return addDisconnectListener(new Action0() {
            @Override
            public void call() {
                logger.info(GattPeripheral.LOG_TAG, "onDisconnectListener(" + subscriber.hashCode() + ")");

                timeout.unschedule();

                subscriber.onError(new BluetoothConnectionLostError());
            }
        });
    }

    void removeDisconnectListener(@NonNull Action0 disconnectListener) {
        disconnectListeners.remove(disconnectListener);
    }

    void dispatchDisconnect() {
        dispatcher.post(new Runnable() {
            @Override
            public void run() {
                logger.info(GattPeripheral.LOG_TAG, "dispatchDisconnect()");

                GattDispatcher.this.onServicesDiscovered = null;
                GattDispatcher.this.onCharacteristicWrite = null;
                GattDispatcher.this.onDescriptorWrite = null;

                for (Action0 onDisconnect : disconnectListeners) {
                    onDisconnect.call();
                }
                disconnectListeners.clear();

                if (packetHandler != null) {
                    packetHandler.transportDisconnected();
                }
            }
        });
    }

    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        logger.info(GattPeripheral.LOG_TAG, "onConnectionStateChange('" + gatt + "', " + status + ", " + newState + ")");

        dispatcher.post(new Runnable() {
            @Override
            public void run() {
                if (connectionStateListeners.isEmpty()) {
                    logger.warn(GattPeripheral.LOG_TAG, "unhandled call to onConnectionStateChange");
                } else {
                    final Iterator<ConnectionStateListener> iterator = connectionStateListeners.iterator();
                    Action0 removeListener = new Action0() {
                        @Override
                        public void call() {
                            iterator.remove();
                        }
                    };
                    while (iterator.hasNext()) {
                        ConnectionStateListener listener = iterator.next();
                        listener.onConnectionStateChanged(gatt, status, newState, removeListener);
                    }
                }
            }
        });
    }

    @Override
    public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
        super.onServicesDiscovered(gatt, status);
        logger.info(GattPeripheral.LOG_TAG, "onServicesDiscovered('" + gatt + "', " + status + ")");

        dispatcher.post(new Runnable() {
            @Override
            public void run() {
                if (onServicesDiscovered != null) {
                    onServicesDiscovered.call(gatt, status);
                } else {
                    logger.warn(GattPeripheral.LOG_TAG, "unhandled call to onServicesDiscovered");
                }
            }
        });
    }

    @Override
    public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

        logger.info(GattPeripheral.LOG_TAG, "onCharacteristicRead('" + gatt + "', " + characteristic + ", " + status + ")");

        dispatcher.post(new Runnable() {
            @Override
            public void run() {
                if (packetHandler != null) {
                    if (!packetHandler.processIncomingPacket(characteristic.getUuid(), characteristic.getValue())) {
                        logger.warn(GattPeripheral.LOG_TAG, "No packet handler for characteristic read " + characteristic.getUuid());
                    }
                }
            }
        });
    }

    @Override
    public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);

        logger.info(GattPeripheral.LOG_TAG, "onCharacteristicWrite('" + gatt + "', " + characteristic + ", " + status + ")");

        dispatcher.post(new Runnable() {
            @Override
            public void run() {
                if (onCharacteristicWrite != null) {
                    onCharacteristicWrite.call(gatt, characteristic, status);
                } else {
                    logger.warn(GattPeripheral.LOG_TAG, "unhandled call to onCharacteristicWrite");
                }
            }
        });
    }

    @Override
    public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        logger.info(GattPeripheral.LOG_TAG, "onCharacteristicChanged('" + gatt + "', " + characteristic + ", " + ")");

        dispatcher.post(new Runnable() {
            @Override
            public void run() {
                if (packetHandler != null) {
                    if (!packetHandler.processIncomingPacket(characteristic.getUuid(), characteristic.getValue())) {
                        logger.warn(GattPeripheral.LOG_TAG, "No packet handler for characteristic changed " + characteristic.getUuid());
                    }
                }
            }
        });
    }

    @Override
    public void onDescriptorWrite(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
        super.onDescriptorWrite(gatt, descriptor, status);

        logger.info(GattPeripheral.LOG_TAG, "onDescriptorWrite('" + gatt + "', " + descriptor + ", " + ")");

        dispatcher.post(new Runnable() {
            @Override
            public void run() {
                if (onDescriptorWrite != null) {
                    onDescriptorWrite.call(gatt, descriptor, status);
                } else {
                    logger.warn(GattPeripheral.LOG_TAG, "unhandled call to onDescriptorWrite");
                }
            }
        });
    }


    interface ConnectionStateListener {
        void onConnectionStateChanged(@NonNull BluetoothGatt gatt,
                                      int status,
                                      int newState,
                                      @NonNull Action0 removeListener);
    }
}
