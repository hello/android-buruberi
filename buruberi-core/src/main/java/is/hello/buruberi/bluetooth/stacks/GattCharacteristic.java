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
import android.support.annotation.CheckResult;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.UUID;

import rx.Observable;

/**
 * Represents a gatt characteristic from a {@link GattService}.
 * <p>
 * An instance of the {@code GattCharacteristic} class is only valid for the duration
 * of one connection to a remote peripheral. When client code detects that a peripheral
 * connection has ended, it should clear any references it has to {@code GattCharacteristic}s.
 */
public interface GattCharacteristic {
    /**
     * The maximum length of a Bluetooth Low Energy packet.
     */
    int PACKET_LENGTH = 20;


    //region Properties

    /**
     * Characteristic is broadcastable.
     */
    int PROPERTY_BROADCAST = BluetoothGattCharacteristic.PROPERTY_BROADCAST;

    /**
     * Characteristic is readable.
     */
    int PROPERTY_READ = BluetoothGattCharacteristic.PROPERTY_READ;

    /**
     * Characteristic can be written without response.
     */
    int PROPERTY_WRITE_NO_RESPONSE = BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;

    /**
     * Characteristic can be written.
     */
    int PROPERTY_WRITE = BluetoothGattCharacteristic.PROPERTY_WRITE;

    /**
     * Characteristic supports notification
     */
    int PROPERTY_NOTIFY = BluetoothGattCharacteristic.PROPERTY_NOTIFY;

    /**
     * Characteristic supports indication
     */
    int PROPERTY_INDICATE = BluetoothGattCharacteristic.PROPERTY_INDICATE;

    /**
     * Characteristic supports write with signature
     */
    int PROPERTY_SIGNED_WRITE = BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE;

    /**
     * Characteristic has extended properties
     */
    int PROPERTY_EXTENDED_PROPS = BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS;


    /**
     * Marks an {@code int} as containing one of the
     * property constants from {@code GattCharacteristic}.
     *
     * @see #PROPERTY_BROADCAST
     * @see #PROPERTY_READ
     * @see #PROPERTY_WRITE_NO_RESPONSE
     * @see #PROPERTY_WRITE
     * @see #PROPERTY_NOTIFY
     * @see #PROPERTY_INDICATE
     * @see #PROPERTY_SIGNED_WRITE
     * @see #PROPERTY_EXTENDED_PROPS
     */
    @Target({
            ElementType.FIELD,
            ElementType.PARAMETER,
            ElementType.METHOD,
            ElementType.LOCAL_VARIABLE
    })
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @IntDef({PROPERTY_BROADCAST, PROPERTY_READ, PROPERTY_WRITE_NO_RESPONSE,
            PROPERTY_WRITE, PROPERTY_NOTIFY, PROPERTY_INDICATE,
            PROPERTY_SIGNED_WRITE, PROPERTY_EXTENDED_PROPS})
    @interface Properties {}

    //endregion


    //region Permissions

    /**
     * Indicates no permissions could be found.
     */
    int PERMISSION_NULL = 0;

    /**
     * Characteristic read permission
     */
    int PERMISSION_READ = BluetoothGattCharacteristic.PERMISSION_READ;

    /**
     * Allow encrypted read operations
     */
    int PERMISSION_READ_ENCRYPTED = BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED;

    /**
     * Allow reading with man-in-the-middle protection
     */
    int PERMISSION_READ_ENCRYPTED_MITM = BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM;

    /**
     * Characteristic write permission
     */
    int PERMISSION_WRITE = BluetoothGattCharacteristic.PERMISSION_WRITE;

    /**
     * Allow encrypted writes
     */
    int PERMISSION_WRITE_ENCRYPTED = BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED;

    /**
     * Allow encrypted writes with man-in-the-middle protection
     */
    int PERMISSION_WRITE_ENCRYPTED_MITM = BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM;

    /**
     * Allow signed write operations
     */
    int PERMISSION_WRITE_SIGNED = BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED;

    /**
     * Allow signed write operations with man-in-the-middle protection
     */
    int PERMISSION_WRITE_SIGNED_MITM = BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM;

    /**
     * Marks an {@code int} as containing one of the
     * permission constants from {@code GattCharacteristic}.
     *
     * @see #PERMISSION_READ
     * @see #PERMISSION_READ_ENCRYPTED
     * @see #PERMISSION_READ_ENCRYPTED_MITM
     * @see #PERMISSION_WRITE
     * @see #PERMISSION_WRITE_ENCRYPTED
     * @see #PERMISSION_WRITE_ENCRYPTED_MITM
     * @see #PERMISSION_WRITE_SIGNED
     * @see #PERMISSION_WRITE_SIGNED_MITM
     */
    @Target({
            ElementType.FIELD,
            ElementType.PARAMETER,
            ElementType.METHOD,
            ElementType.LOCAL_VARIABLE
    })
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @IntDef({PERMISSION_READ, PERMISSION_READ_ENCRYPTED, PERMISSION_READ_ENCRYPTED_MITM,
            PERMISSION_WRITE, PERMISSION_WRITE_ENCRYPTED, PERMISSION_WRITE_ENCRYPTED_MITM,
            PERMISSION_WRITE_SIGNED, PERMISSION_WRITE_SIGNED_MITM, PERMISSION_NULL})
    @interface Permissions {}

    //endregion


    //region Properties

    /**
     * Returns the identifier of the characteristic.
     */
    @NonNull UUID getUuid();

    /**
     * Returns the properties of this characteristic.
     */
    @Properties int getProperties();

    /**
     * Returns the permissions of this characteristic.
     */
    @Permissions int getPermissions();

    /**
     * Returns the service that owns this characteristic.
     */
    @NonNull GattService getService();

    /**
     * Returns the descriptors of the characteristic.
     */
    @NonNull List<UUID> getDescriptors();

    /**
     * Returns the permissions associated with a given descriptor.
     * @param descriptor    The descriptor identifier.
     * @return The permissions of the descriptor if it can be found; {@code 0} otherwise.
     */
    @Permissions int getDescriptorPermissions(@NonNull UUID descriptor);

    /**
     * Sets the packet listener for the characteristic.
     * <p>
     * The packet listener of the characteristic will be cleared if
     * the peripheral the characteristic is tied to disconnects.
     * Clients should set the packet listener after every connect
     * attempt and service discovery run on your peripherals.
     *
     * @param packetListener    The listener.
     */
    void setPacketListener(@NonNull PacketListener packetListener);

    //endregion


    //region Operations

    /**
     * Reads the characteristic's value from the remote peripheral.
     *
     * @param timeout   The timeout to apply to the operation.
     * @return The operation, waiting to be subscribed to.
     */
    @CheckResult
    @NonNull Observable<byte[]> read(@NonNull OperationTimeout timeout);

    /**
     * Enable notification events for a given descriptor.
     *
     * @param descriptor    The descriptor to enable notifications for.
     * @param timeout       The timeout to apply to the operation.
     * @return The operation, waiting to be subscribed to.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @CheckResult
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
    @CheckResult
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
    @CheckResult
    @NonNull Observable<Void> write(@NonNull GattPeripheral.WriteType writeType,
                                    @NonNull byte[] payload,
                                    @NonNull OperationTimeout timeout);

    //endregion


    /**
     * Allows client code to act on incoming characteristic reads and  notifications.
     */
    interface PacketListener {
        /**
         * Notifies the listener that a characteristic has been notified.
         * @param characteristic    The characteristic affected.
         * @param payload           The bytes received.
         */
        void onCharacteristicNotify(@NonNull UUID characteristic, @NonNull byte[] payload);

        /**
         * Informs the listener that the underlying peripheral connection was lost.
         */
        void onPeripheralDisconnected();
    }
}
