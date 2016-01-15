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
package is.hello.buruberi.bluetooth.errors;

import java.util.UUID;

import is.hello.buruberi.bluetooth.stacks.GattCharacteristic;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;

/**
 * Indicates that a Bluetooth operation timed out before completion.
 * <p>
 * Client code should use the {@link Operation#COMMAND_RESPONSE} constant.
 */
public class OperationTimeoutException extends BuruberiException {
    /**
     * The operation that timed out.
     */
    public final Operation operation;

    public OperationTimeoutException(Operation operation, Throwable cause) {
        super("Operation " + operation + " timed out", cause);
        this.operation = operation;
    }

    public OperationTimeoutException(Operation operation) {
        this(operation, null);
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
         * Corresponds to {@link GattCharacteristic#enableNotification(UUID, OperationTimeout)}.
         */
        ENABLE_NOTIFICATION,

        /**
         * Corresponds to {@link GattCharacteristic#disableNotification(UUID, OperationTimeout)}.
         */
        DISABLE_NOTIFICATION,

        /**
         * Corresponds to {@link GattPeripheral#removeBond(OperationTimeout)}
         */
        REMOVE_BOND,

        /**
         * Corresponds to {@link GattCharacteristic#write(GattPeripheral.WriteType, byte[], OperationTimeout)}.
         */
        WRITE_COMMAND,

        /**
         * Indicates a client code timeout.
         */
        COMMAND_RESPONSE,
    }
}
