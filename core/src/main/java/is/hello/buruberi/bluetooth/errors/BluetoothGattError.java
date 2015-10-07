/*
 * Copyright 2015 Hello, Inc
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

/**
 * Used to report errors from the gatt layer of the Android bluetooth stack.
 * <p/>
 * This error type generally should not be used outside of direct interactions
 * with a {@see is.hello.buruberi.bluetooth.stacks.Peripheral} object.
 */
public class BluetoothGattError extends BluetoothError {
    //region Undocumented error codes

    public static final int GATT_ILLEGAL_PARAMETER = 0x0087;
    public static final int GATT_NO_RESOURCES = 0x0080;
    public static final int GATT_INTERNAL_ERROR = 0x0081;
    public static final int GATT_WRONG_STATE = 0x0082;
    public static final int GATT_DB_FULL = 0x0083;
    public static final int GATT_BUSY = 0x0084;
    public static final int GATT_AUTH_FAIL = 0x0089;
    public static final int GATT_INVALID_CFG = 0x008b;

    /**
     * This error code shows up if you turn off the Bluetooth radio,
     * and a device has an open gatt layer <em>and</em> is bonded.
     * Retrying your connection after receiving this error will work
     * seemingly 100% of the time.
     */
    public static final int GATT_STACK_ERROR = 0x0085;

    /**
     * Connection terminated by local host.
     */
    public static final int GATT_CONN_TERMINATE_LOCAL_HOST = 0x16;

    /**
     * Connection terminate by peer user.
     */
    public static final int GATT_CONN_TERMINATE_PEER_USER = 0x13;

    /**
     * Connection timeout.
     */
    public static final int GATT_CONN_TIMEOUT = 0x08;

    /**
     * Connection failed to establish.
     */
    public static final int GATT_CONN_FAIL_ESTABLISH = 0x03E;

    //endregion


    public final int statusCode;
    public final @Nullable Operation operation;

    public static boolean isRecoverableConnectError(int status) {
        return (status == GATT_CONN_TERMINATE_LOCAL_HOST || // Nexus devices
                status == GATT_STACK_ERROR); // Samsung devices
    }

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

    public BluetoothGattError(int statusCode, @Nullable Operation operation) {
        super(statusToString(statusCode));

        this.statusCode = statusCode;
        this.operation = operation;
    }

    @Override
    public boolean isFatal() {
        // If GATT_STACK_ERROR/133 is reported more than once, the gatt
        // layer is unstable, and won't be fixed until the user
        // power cycles their phone's wireless radios.
        return (statusCode == BluetoothGattError.GATT_STACK_ERROR);
    }

    public enum Operation {
        CONNECT,
        DISCONNECT,
        DISCOVER_SERVICES,
        SUBSCRIBE_NOTIFICATION,
        UNSUBSCRIBE_NOTIFICATION,
        WRITE_COMMAND,
    }
}
