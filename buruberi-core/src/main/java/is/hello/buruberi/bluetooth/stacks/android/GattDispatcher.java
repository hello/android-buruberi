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

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;

class GattDispatcher extends BluetoothGattCallback {
    private final LoggerFacade logger;
    private final CharacteristicChangeListener characteristicChangeListener;
    private final Handler dispatcher = new Handler(Looper.getMainLooper());

    private final List<ConnectionListener> connectionStateListeners = new ArrayList<>();
    /*package*/ @Nullable CharacteristicReadListener characteristicRead;
    /*package*/ @Nullable ServicesDiscoveredListener servicesDiscovered;
    /*package*/ @Nullable CharacteristicWriteListener oharacteristicWrite;
    /*package*/ @Nullable DescriptorWriteListener descriptorWrite;


    /*package*/ GattDispatcher(@NonNull LoggerFacade logger,
                               @NonNull CharacteristicChangeListener characteristicChangeListener) {
        this.logger = logger;
        this.characteristicChangeListener = characteristicChangeListener;
    }

    /*package*/ void addConnectionListener(@NonNull ConnectionListener changeHandler) {
        connectionStateListeners.add(changeHandler);
    }

    /*package*/ void removeConnectionListener(@NonNull ConnectionListener changeHandler) {
        connectionStateListeners.remove(changeHandler);
    }

    /*package*/ void clearListeners() {
        this.characteristicRead = null;
        this.servicesDiscovered = null;
        this.oharacteristicWrite = null;
        this.descriptorWrite = null;
    }

    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
        logger.info(GattPeripheral.LOG_TAG, "onConnectionStateChange('" + gatt + "', " +
                status + ", " + newState + ")");

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
        logger.info(GattPeripheral.LOG_TAG, "onServicesDiscovered('" + gatt + "', " + status + ")");

        dispatcher.post(new Runnable() {
            @Override
            public void run() {
                if (servicesDiscovered != null) {
                    servicesDiscovered.onServicesDiscovered(gatt, status);
                } else {
                    logger.warn(GattPeripheral.LOG_TAG, "unhandled call to onServicesDiscovered");
                }
            }
        });
    }

    @Override
    public void onCharacteristicRead(final BluetoothGatt gatt,
                                     final BluetoothGattCharacteristic characteristic,
                                     final int status) {
        logger.info(GattPeripheral.LOG_TAG, "onCharacteristicRead('" + gatt + "', " +
                characteristic + ", " + status + ")");

        dispatcher.post(new Runnable() {
            @Override
            public void run() {
                if (characteristicRead != null) {
                    characteristicRead.onCharacteristicRead(gatt, characteristic, status);
                } else {
                    logger.warn(GattPeripheral.LOG_TAG, "unhandled call to onCharacteristicRead");
                }
            }
        });
    }

    @Override
    public void onCharacteristicWrite(final BluetoothGatt gatt,
                                      final BluetoothGattCharacteristic characteristic,
                                      final int status) {
        logger.info(GattPeripheral.LOG_TAG, "onCharacteristicWrite('" + gatt + "', " +
                characteristic + ", " + status + ")");

        dispatcher.post(new Runnable() {
            @Override
            public void run() {
                if (oharacteristicWrite != null) {
                    oharacteristicWrite.onCharacteristicWrite(gatt, characteristic, status);
                } else {
                    logger.warn(GattPeripheral.LOG_TAG, "unhandled call to onCharacteristicWrite");
                }
            }
        });
    }

    @Override
    public void onCharacteristicChanged(final BluetoothGatt gatt,
                                        final BluetoothGattCharacteristic characteristic) {
        logger.info(GattPeripheral.LOG_TAG, "onCharacteristicChanged('" + gatt + "', " +
                characteristic + ", " + ")");

        dispatcher.post(new Runnable() {
            @Override
            public void run() {
                characteristicChangeListener.onCharacteristicChanged(gatt, characteristic);
            }
        });
    }

    @Override
    public void onDescriptorWrite(final BluetoothGatt gatt,
                                  final BluetoothGattDescriptor descriptor,
                                  final int status) {
        logger.info(GattPeripheral.LOG_TAG, "onDescriptorWrite('" + gatt + "', " + descriptor + ", " + ")");

        dispatcher.post(new Runnable() {
            @Override
            public void run() {
                if (descriptorWrite != null) {
                    descriptorWrite.onDescriptorWrite(gatt, descriptor, status);
                } else {
                    logger.warn(GattPeripheral.LOG_TAG, "unhandled call to onDescriptorWrite");
                }
            }
        });
    }


    interface CharacteristicChangeListener {
        void onCharacteristicChanged(@NonNull BluetoothGatt gatt,
                                     @NonNull BluetoothGattCharacteristic characteristic);
    }

    interface CharacteristicReadListener {
        void onCharacteristicRead(@NonNull BluetoothGatt gatt,
                                  @NonNull BluetoothGattCharacteristic characteristic,
                                  int status);
    }

    interface ServicesDiscoveredListener {
        void onServicesDiscovered(@NonNull BluetoothGatt gatt, int status);
    }

    interface CharacteristicWriteListener {
        void onCharacteristicWrite(@NonNull BluetoothGatt gatt,
                                   @NonNull BluetoothGattCharacteristic characteristic,
                                   int status);
    }
    interface DescriptorWriteListener {
        void onDescriptorWrite(@NonNull BluetoothGatt gatt,
                               @NonNull BluetoothGattDescriptor descriptor,
                               int status);
    }

    @SuppressWarnings("UnusedParameters")
    static abstract class ConnectionListener {
        /*package*/ boolean onConnected(@NonNull BluetoothGatt gatt, int status) {
            // Do nothing.
            return false;
        }

        /*package*/ boolean onConnecting(@NonNull BluetoothGatt gatt, int status) {
            // Do nothing.
            return true;
        }

        /*package*/ boolean onDisconnecting(@NonNull BluetoothGatt gatt, int status) {
            // Do nothing.
            return true;
        }

        /*package*/ boolean onDisconnected(@NonNull BluetoothGatt gatt, int status) {
            // Do nothing.
            return false;
        }

        /*package*/ boolean onError(@NonNull BluetoothGatt gatt, int status, int state) {
            // Do nothing.
            return false;
        }

        /*package*/ final boolean dispatch(@NonNull BluetoothGatt gatt, int status, int state) {
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
