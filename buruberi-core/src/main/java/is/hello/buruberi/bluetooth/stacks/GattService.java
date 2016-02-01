/*
 * Copyright 2015 Hello Inc.
 * Copyright (C) 2013 The Android Open Source Project
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
package is.hello.buruberi.bluetooth.stacks;

import android.bluetooth.BluetoothGattService;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.UUID;

/**
 * Represents a gatt service associated with a {@link GattPeripheral}.
 * <p>
 * An instance of the {@code GattService} class is only valid for the duration of
 * one connection to a remote peripheral. When client code detects that a peripheral
 * connection has ended, it should clear any references it has to {@code GattService}s.
 * <p>
 * {@code GattService} currently does not support included services.
 */
public interface GattService {
    /**
     * Primary service
     */
    int TYPE_PRIMARY = BluetoothGattService.SERVICE_TYPE_PRIMARY;

    /**
     * Secondary service (included by primary services)
     */
    int TYPE_SECONDARY = BluetoothGattService.SERVICE_TYPE_SECONDARY;

    /**
     * Marks an {@code int} as containing one of the
     * type constants from {@code GattService}.
     *
     * @see #TYPE_PRIMARY
     * @see #TYPE_SECONDARY
     */
    @Target({
            ElementType.FIELD,
            ElementType.PARAMETER,
            ElementType.METHOD,
            ElementType.LOCAL_VARIABLE
    })
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @IntDef({TYPE_PRIMARY, TYPE_SECONDARY})
    @interface Type {}

    /**
     * Returns the identifier of the service.
     */
    @NonNull UUID getUuid();

    /**
     * Returns the type of the service.
     */
    @Type int getType();

    /**
     * Returns the identifiers of the characteristics contained in the service.
     */
    @NonNull List<UUID> getCharacteristics();

    /**
     * Returns the characteristic associated with a given identifier.
     * <p>
     * Guaranteed to return the same object for the duration
     * of a connection to a remote peripheral.
     */
    GattCharacteristic getCharacteristic(@NonNull UUID identifier);
}
