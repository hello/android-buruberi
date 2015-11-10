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
package is.hello.buruberi.bluetooth.errors;

import android.bluetooth.BluetoothGatt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.UUID;

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.GattService;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;
import is.hello.buruberi.util.NonGuaranteed;

/**
 * Indicates low level gatt errors from the Android SDK.
 * <p>
 * Very few gatt error codes are properly documented by the Android SDK,
 * and some vary by device vendor. The error codes documented in this class
 * are a combination of trial and error, and combing the low level Bluetooth
 * implementation in the AoSP. These codes can and will change, and should
 * only be considered a hint.
 */
public class GattException extends BuruberiException {
    //region Error codes

    @NonGuaranteed
    public static final int GATT_ILLEGAL_PARAMETER = 0x0087;
    
    @NonGuaranteed
    public static final int GATT_NO_RESOURCES = 0x0080;

    /**
     * Generic internal error code from the low level Bluetooth implementation.
     * <p>
     * @see NonGuaranteed Undocumented status code from AoSP project.
     */
    @NonGuaranteed
    public static final int GATT_INTERNAL_ERROR = 0x0081;

    @NonGuaranteed
    public static final int GATT_WRONG_STATE = 0x0082;

    @NonGuaranteed
    public static final int GATT_DB_FULL = 0x0083;

    @NonGuaranteed
    public static final int GATT_BUSY = 0x0084;

    @NonGuaranteed
    public static final int GATT_AUTH_FAIL = 0x0089;

    @NonGuaranteed
    public static final int GATT_INVALID_CFG = 0x008b;

    /**
     * Generic internal error code from the low level Bluetooth implementation.
     * <p>
     * This error code shows up if you turn off the Bluetooth radio,
     * and a device has an open gatt layer <em>and</em> is bonded.
     * Retrying your connection after receiving this error will work
     * seemingly 100% of the time.
     * <p>
     * If this error is reported more than once, the phone's Bluetooth has become unstable,
     * and won't be fixed until the user power cycles their phone's Wi-Fi and Bluetooth radios.
     * <p>
     * @see NonGuaranteed Undocumented status code from AoSP project.
     *
     * @see #isRecoverableConnectError(int)
     * @see #isInstabilityLikely()
     */
    @NonGuaranteed
    public static final int GATT_STACK_ERROR = 0x0085;

    /**
     * Connection terminated by local host.
     * <p>
     * @see NonGuaranteed Undocumented status code from AoSP project.
     */
    @NonGuaranteed
    public static final int GATT_CONN_TERMINATE_LOCAL_HOST = 0x16;

    /**
     * Connection terminate by peer user.
     * <p>
     * @see NonGuaranteed Undocumented status code from AoSP project.
     */
    @NonGuaranteed
    public static final int GATT_CONN_TERMINATE_PEER_USER = 0x13;

    /**
     * Connection timeout.
     * <p>
     * @see NonGuaranteed Undocumented status code from AoSP project.
     */
    @NonGuaranteed
    public static final int GATT_CONN_TIMEOUT = 0x08;

    /**
     * Connection failed to establish.
     * <p>
     * @see NonGuaranteed Undocumented status code from AoSP project.
     */
    @NonGuaranteed
    public static final int GATT_CONN_FAIL_ESTABLISH = 0x03E;


    /**
     * Returns whether or not a given error code encountered during a first
     * connection attempt can be resolved by retrying the connection.
     *
     * @param statusCode  The error code.
     * @return {@code true} if the error is likely recoverable; {@code false} otherwise.
     *
     * @see GattPeripheral#connect(OperationTimeout)
     */
    public static boolean isRecoverableConnectError(int statusCode) {
        return (statusCode == GATT_CONN_TERMINATE_LOCAL_HOST || // Nexus devices
                statusCode == GATT_STACK_ERROR); // Samsung devices
    }

    /**
     * Returns the corresponding constant name for a given {@code GATT_*} value.
     *
     */
    public static @NonNull String statusToString(int status) {
        switch (status) {
            case BluetoothGatt.GATT_SUCCESS:
                return "GATT_SUCCESS";

            case BluetoothGatt.GATT_READ_NOT_PERMITTED:
                return "GATT_READ_NOT_PERMITTED";

            case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
                return "GATT_WRITE_NOT_PERMITTED";

            case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
                return "GATT_INSUFFICIENT_AUTHENTICATION";

            case BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED:
                return "GATT_REQUEST_NOT_SUPPORTED";

            case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
                return "GATT_INSUFFICIENT_ENCRYPTION";

            case BluetoothGatt.GATT_INVALID_OFFSET:
                return "GATT_INVALID_OFFSET";

            case BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH:
                return "GATT_INVALID_ATTRIBUTE_LENGTH";

            case BluetoothGatt.GATT_FAILURE:
                return "GATT_FAILURE";

            case GATT_CONN_TERMINATE_LOCAL_HOST:
                return "GATT_CONN_TERMINATE_LOCAL_HOST";

            case GATT_CONN_TERMINATE_PEER_USER:
                return "GATT_CONN_TERMINATE_PEER_USER";

            case GATT_CONN_TIMEOUT:
                return "GATT_CONN_TIMEOUT";

            case GATT_STACK_ERROR:
                return "GATT_STACK_ERROR";

            case GATT_CONN_FAIL_ESTABLISH:
                return "GATT_CONN_FAIL_ESTABLISH";

            case GATT_ILLEGAL_PARAMETER:
                return "GATT_ILLEGAL_PARAMETER";

            case GATT_NO_RESOURCES:
                return "GATT_NO_RESOURCES";

            case GATT_INTERNAL_ERROR:
                return "GATT_INTERNAL_ERROR";

            case GATT_WRONG_STATE:
                return "GATT_WRONG_STATE";

            case GATT_DB_FULL:
                return "GATT_DB_FULL";

            case GATT_BUSY:
                return "GATT_BUSY";

            case GATT_AUTH_FAIL:
                return "GATT_AUTH_FAIL";

            case GATT_INVALID_CFG:
                return "GATT_INVALID_CFG";

            default:
                return "UNKNOWN: " + status;
        }
    }

    //endregion


    /**
     * The status code given by the gatt layer.
     */
    public final int statusCode;

    /**
     * The operation where the gatt exception was encountered, if applicable.
     */
    public final @Nullable Operation operation;

    public GattException(int statusCode, @Nullable Operation operation) {
        super(statusToString(statusCode));

        this.statusCode = statusCode;
        this.operation = operation;
    }

    /**
     * @return {@link true} if {@link #GATT_STACK_ERROR} is reported; {@code false} otherwise.
     *
     * @see #GATT_STACK_ERROR for more info.
     */
    @Override
    public boolean isInstabilityLikely() {
        return (statusCode == GattException.GATT_STACK_ERROR);
    }


    /**
     * The operation on which the gatt layer encountered an error. Corresponds
     * rough to all of the operations possible on a {@link GattPeripheral} object.
     */
    public enum Operation {
        /**
         * Corresponds to {@link GattPeripheral#connect(OperationTimeout)}.
         */
        CONNECT,

        /**
         * Corresponds to {@link GattPeripheral#disconnect()}.
         */
        DISCONNECT,

        /**
         * Corresponds to {@link GattPeripheral#discoverServices(OperationTimeout)}.
         */
        DISCOVER_SERVICES,

        /**
         * Corresponds to {@link GattPeripheral#enableNotification(GattService, UUID, UUID, OperationTimeout)}.
         */
        ENABLE_NOTIFICATION,

        /**
         * Corresponds to {@link GattPeripheral#disableNotification(GattService, UUID, UUID, OperationTimeout)}.
         */
        DISABLE_NOTIFICATION,

        /**
         * Corresponds to {@link GattPeripheral#writeCommand(GattService, UUID, GattPeripheral.WriteType, byte[], OperationTimeout)}.
         */
        WRITE_COMMAND,
    }
}
