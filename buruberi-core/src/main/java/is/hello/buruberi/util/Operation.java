package is.hello.buruberi.util;

import java.util.UUID;

import is.hello.buruberi.bluetooth.stacks.GattCharacteristic;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;

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
     * Corresponds to {@link GattCharacteristic#read(OperationTimeout)}.
     */
    READ,

    /**
     * Indicates a client code timeout.
     */
    COMMAND_RESPONSE,
}
