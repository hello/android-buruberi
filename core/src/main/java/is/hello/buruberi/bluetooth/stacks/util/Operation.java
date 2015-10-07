package is.hello.buruberi.bluetooth.stacks.util;

import java.util.UUID;

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;

/**
 * Provided for clients of the Buruberi library who want to
 * perform multiple steps in a single rx.Observable, and
 * want subscribers to be able to react to changes in status.
 */
public enum Operation {
    /**
     * The client is connecting to a Peripheral.
     *
     * @see GattPeripheral#connect(OperationTimeout)
     */
    CONNECTING,

    /**
     * The client is bonding to a Peripheral.
     *
     * @see GattPeripheral#createBond()
     */
    BONDING,

    /**
     * The client is performing service discovery on a Peripheral.
     *
     * @see GattPeripheral#discoverServices(OperationTimeout)
     * @see GattPeripheral#discoverService(UUID, OperationTimeout)
     */
    DISCOVERING_SERVICES,

    /**
     * The client has connected to a Peripheral.
     */
    CONNECTED,
}
