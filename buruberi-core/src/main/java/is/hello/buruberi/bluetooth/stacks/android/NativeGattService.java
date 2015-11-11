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

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import is.hello.buruberi.bluetooth.stacks.GattCharacteristic;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.GattService;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;
import rx.Observable;

public final class NativeGattService implements GattService {
    final @NonNull BluetoothGattService service;
    final @NonNull NativeGattPeripheral peripheral;


    static @NonNull Map<UUID, GattService> wrap(@NonNull List<BluetoothGattService> services,
                                                      @NonNull NativeGattPeripheral peripheral) {
        final Map<UUID, GattService> peripheralServices = new HashMap<>();

        for (final BluetoothGattService nativeService : services) {
            peripheralServices.put(nativeService.getUuid(),
                                   new NativeGattService(nativeService, peripheral));
        }

        return peripheralServices;
    }

    NativeGattService(@NonNull BluetoothGattService service,
                      @NonNull NativeGattPeripheral peripheral) {
        this.service = service;
        this.peripheral = peripheral;
    }


    @Override
    public UUID getUuid() {
        return service.getUuid();
    }

    @Override
    @Type
    public int getType() {
        final @Type int type = service.getType();
        return type;
    }

    @Override
    public List<UUID> getCharacteristics() {
        final List<UUID> identifiers = new ArrayList<>();
        for (final BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
            identifiers.add(characteristic.getUuid());
        }
        return identifiers;
    }

    @Override
    public GattCharacteristic getCharacteristic(@NonNull UUID identifier) {
        final BluetoothGattCharacteristic characteristic = service.getCharacteristic(identifier);
        if (characteristic != null) {
            return new NativeGattCharacteristic(characteristic);
        } else {
            return null;
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NativeGattService that = (NativeGattService) o;

        return service.equals(that.service);
    }

    @Override
    public int hashCode() {
        return service.hashCode();
    }


    @Override
    public String toString() {
        return "AndroidPeripheralService{" +
                "service=" + service +
                '}';
    }


    class NativeGattCharacteristic implements GattCharacteristic {
        private final BluetoothGattCharacteristic nativeCharacteristic;

        NativeGattCharacteristic(@NonNull BluetoothGattCharacteristic nativeCharacteristic) {
            this.nativeCharacteristic = nativeCharacteristic;
        }

        @Override
        public UUID getUuid() {
            return nativeCharacteristic.getUuid();
        }

        @Override
        @Properties
        public int getProperties() {
            final @Properties int properties = nativeCharacteristic.getProperties();
            return properties;
        }

        @Override
        @Permissions
        public int getPermissions() {
            final @Permissions int permissions = nativeCharacteristic.getPermissions();
            return permissions;
        }

        @NonNull
        @Override
        public GattService getService() {
            return NativeGattService.this;
        }

        @NonNull
        @Override
        public List<UUID> getDescriptors() {
            final List<UUID> identifiers = new ArrayList<>();
            for (final BluetoothGattDescriptor descriptor : nativeCharacteristic.getDescriptors()) {
                identifiers.add(descriptor.getUuid());
            }
            return identifiers;
        }

        @Permissions
        @Override
        public int getDescriptorPermissions(@NonNull UUID identifier) {
            final BluetoothGattDescriptor descriptor = nativeCharacteristic.getDescriptor(identifier);
            if (descriptor != null) {
                final @Permissions int permissions = descriptor.getPermissions();
                return permissions;
            } else {
                return PERMISSION_NULL;
            }
        }

        @NonNull
        @Override
        public Observable<UUID> enableNotification(@NonNull UUID descriptor,
                                                   @NonNull OperationTimeout timeout) {
            // TODO: Re-implement this operation in terms of new API.

            return peripheral.enableNotification(getService(),
                                                 getUuid(),
                                                 descriptor,
                                                 timeout);
        }

        @NonNull
        @Override
        public Observable<UUID> disableNotification(@NonNull UUID descriptor,
                                                    @NonNull OperationTimeout timeout) {
            // TODO: Re-implement this operation in terms of new API.

            return peripheral.disableNotification(getService(),
                                                  getUuid(),
                                                  descriptor,
                                                  timeout);
        }

        @NonNull
        @Override
        public Observable<Void> write(@NonNull GattPeripheral.WriteType writeType,
                                      @NonNull byte[] payload,
                                      @NonNull OperationTimeout timeout) {
            // TODO: Re-implement this operation in terms of new API.

            return peripheral.writeCommand(getService(),
                                           getUuid(),
                                           writeType,
                                           payload,
                                           timeout);
        }

        @Override
        public String toString() {
            return "NativeCharacteristic{" +
                    "uuid=" + getUuid() +
                    '}';
        }
    }
}
