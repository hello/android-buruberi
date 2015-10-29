/*
 * Copyright 2015 Hello Inc.
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package is.hello.buruberi.bluetooth.stacks;

import android.Manifest;
import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;

import java.util.List;
import java.util.UUID;

import rx.Observable;

public interface GattCharacteristic {
    //region Properties

    int PROPERTY_BROADCAST = BluetoothGattCharacteristic.PROPERTY_BROADCAST;
    int PROPERTY_READ = BluetoothGattCharacteristic.PROPERTY_READ;
    int PROPERTY_WRITE_NO_RESPONSE = BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
    int PROPERTY_WRITE = BluetoothGattCharacteristic.PROPERTY_WRITE;
    int PROPERTY_NOTIFY = BluetoothGattCharacteristic.PROPERTY_NOTIFY;
    int PROPERTY_INDICATE = BluetoothGattCharacteristic.PROPERTY_INDICATE;
    int PROPERTY_SIGNED_WRITE = BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE;
    int PROPERTY_EXTENDED_PROPS = BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS;

    //endregion


    //region Permissions

    int PERMISSION_READ = BluetoothGattCharacteristic.PERMISSION_READ;
    int PERMISSION_READ_ENCRYPTED = BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED;
    int PERMISSION_READ_ENCRYPTED_MITM = BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM;
    int PERMISSION_WRITE = BluetoothGattCharacteristic.PERMISSION_WRITE;
    int PERMISSION_WRITE_ENCRYPTED = BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED;
    int PERMISSION_WRITE_ENCRYPTED_MITM = BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM;
    int PERMISSION_WRITE_SIGNED = BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED;
    int PERMISSION_WRITE_SIGNED_MITM = BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM;

    //endregion


    //region Properties

    /**
     * Returns the identifier of the characteristic.
     */
    UUID getUuid();

    /**
     * Returns the properties of this characteristic.
     */
    int getProperties();

    /**
     * Returns the permissions of this characteristic.
     */
    int getPermissions();

    /**
     * Returns the service that owns this characteristic.
     */
    @NonNull PeripheralService getService();

    /**
     * Returns the descriptors of the characteristic.
     */
    @NonNull List<UUID> getDescriptors();

    /**
     * Returns the permissions associated with a given descriptor.
     * @param descriptor    The descriptor identifier.
     * @return The permissions of the descriptor if it can be found; {@code 0} otherwise.
     */
    int getDescriptorPermissions(@NonNull UUID descriptor);

    //endregion


    //region Operations

    /**
     * Enable notification events for a given descriptor.
     *
     * @param descriptor    The descriptor to enable notifications for.
     * @param timeout       The timeout to apply to the operation.
     * @return The operation, waiting to be subscribed to.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @NonNull Observable<UUID> enableNotification(@NonNull UUID descriptor,
                                                 @NonNull OperationTimeout timeout);

    /**
     * Disable notification events for a given descriptor.
     *
     * @param descriptor    The descriptor to enable notifications for.
     * @param timeout       The timeout to apply to the operation.
     * @return The operation, waiting to be subscribed to.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @NonNull Observable<UUID> disableNotification(@NonNull UUID descriptor,
                                                  @NonNull OperationTimeout timeout);


    /**
     * Writes a given payload on the characteristic.
     * <p>
     * <em>Important:</em> it appears on some devices that writes are asynchronous on multiple
     * levels. Although the stack may report the write was successful, it doesn't mean the
     * write has actually completed. If you're going to disconnect after a write operation,
     * it's a good idea to add a delay of a few seconds.
     * <p>
     * The value of {@literal writeType} is supposed to be automatically inferred during service
     * discovery, but some devices do not consistently populate the value (Verizon Note 4).
     * As such, this value must be provided for every write command call.
     *
     * @param writeType         The type of write to perform.
     * @param payload           The payload to write. Must be 20 <code>bytes</code> or less.
     * @param timeout           The timeout to wrap the operation within.
     * @return An observable that will emit a single null value, then complete upon success.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @NonNull Observable<Void> write(@NonNull GattPeripheral.WriteType writeType,
                                    @NonNull byte[] payload,
                                    @NonNull OperationTimeout timeout);

    //endregion
}
