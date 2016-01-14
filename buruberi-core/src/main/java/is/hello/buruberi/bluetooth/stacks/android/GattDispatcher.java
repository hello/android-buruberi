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

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import is.hello.buruberi.bluetooth.errors.LostConnectionException;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action2;
import rx.functions.Action3;

class GattDispatcher extends BluetoothGattCallback {
    private final LoggerFacade logger;
    private final List<ConnectionListener> connectionStateListeners = new ArrayList<>();
    private final List<Action0> disconnectListeners = new ArrayList<>();
    private final Handler dispatcher = new Handler(Looper.getMainLooper());

    @Nullable GattPeripheral.PacketHandler packetHandler;
    @Nullable Action2<BluetoothGatt, Integer> onServicesDiscovered;
    @Nullable Action3<BluetoothGatt, BluetoothGattCharacteristic, Integer> onCharacteristicWrite;
    @Nullable Action3<BluetoothGatt, BluetoothGattDescriptor, Integer> onDescriptorWrite;


    GattDispatcher(@NonNull LoggerFacade logger) {
        this.logger = logger;
    }

    void addConnectionListener(@NonNull ConnectionListener changeHandler) {
        connectionStateListeners.add(changeHandler);
    }

    void removeConnectionListener(@NonNull ConnectionListener changeHandler) {
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

                subscriber.onError(new LostConnectionException());
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
                    packetHandler.peripheralDisconnected();
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
                    final Iterator<ConnectionListener> iterator = connectionStateListeners.iterator();
                    while (iterator.hasNext()) {
                        final ConnectionListener listener = iterator.next();
                        if (!listener.dispatch(gatt, status, newState)) {
                            iterator.remove();
                        }
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
                    packetHandler.processIncomingPacket(characteristic.getUuid(),
                                                        characteristic.getValue());
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
                    packetHandler.processIncomingPacket(characteristic.getUuid(),
                                                        characteristic.getValue());
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


    @SuppressWarnings("UnusedParameters")
    static abstract class ConnectionListener {
        boolean onConnected(@NonNull BluetoothGatt gatt, int status) {
            // Do nothing.
            return false;
        }

        boolean onConnecting(@NonNull BluetoothGatt gatt, int status) {
            // Do nothing.
            return true;
        }

        boolean onDisconnecting(@NonNull BluetoothGatt gatt, int status) {
            // Do nothing.
            return true;
        }

        boolean onDisconnected(@NonNull BluetoothGatt gatt, int status) {
            // Do nothing.
            return false;
        }

        boolean onError(@NonNull BluetoothGatt gatt, int status, int state) {
            // Do nothing.
            return false;
        }

        final boolean dispatch(@NonNull BluetoothGatt gatt, int status, int state) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                switch (state) {
                    case BluetoothProfile.STATE_CONNECTED:
                        return onConnected(gatt, status);
                    case BluetoothProfile.STATE_CONNECTING:
                        return onConnecting(gatt, status);
                    case BluetoothProfile.STATE_DISCONNECTING:
                        return onDisconnecting(gatt, status);
                    case BluetoothProfile.STATE_DISCONNECTED:
                        return onDisconnected(gatt, status);
                    default:
                        throw new IllegalArgumentException("Unknown connection state " + state);
                }
            } else {
                return onError(gatt, status, state);
            }
        }
    }
}
