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
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import is.hello.buruberi.bluetooth.stacks.GattCharacteristic;
import is.hello.buruberi.bluetooth.stacks.GattService;

public final class NativeGattService implements GattService {
    final BluetoothGattService service;
    private final NativeGattPeripheral peripheral;
    private final Map<UUID, NativeGattCharacteristic> characteristics = new HashMap<>();


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
        NativeGattCharacteristic nativeCharacteristic = characteristics.get(identifier);
        if (nativeCharacteristic == null) {
            final BluetoothGattCharacteristic characteristic = service.getCharacteristic(identifier);
            if (characteristic != null) {
                nativeCharacteristic = new NativeGattCharacteristic(characteristic, this, peripheral);
                characteristics.put(identifier, nativeCharacteristic);
            }
        }
        return nativeCharacteristic;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final NativeGattService that = (NativeGattService) o;
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
}
