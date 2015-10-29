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
