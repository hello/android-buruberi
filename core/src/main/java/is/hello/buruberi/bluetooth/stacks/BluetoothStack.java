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
package is.hello.buruberi.bluetooth.stacks;

import android.Manifest;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;

import java.util.List;

import is.hello.buruberi.bluetooth.errors.ChangePowerStateException;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import rx.Observable;
import rx.Scheduler;

/**
 * A semi-opaque interface intended to contain all of the necessary logic to interact
 * with a platform Bluetooth stack. Responsible for scanning and vending Peripherals.
 *
 * @see GattPeripheral
 */
public interface BluetoothStack {
    /**
     * The logging tag that implementations of BluetoothStack should use.
     */
    String LOG_TAG = "Bluetooth." + BluetoothStack.class.getSimpleName();


    /**
     * Performs a scan for peripherals matching a given set of criteria.
     * <p>
     * Yields {@link is.hello.buruberi.bluetooth.errors.UserDisabledBuruberiException}
     * if the device's Bluetooth radio is currently disabled.
     *
     * @see is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria
     */
    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    })
    @NonNull Observable<List<GattPeripheral>> discoverPeripherals(@NonNull PeripheralCriteria peripheralCriteria);

    /**
     * Returns the Rx scheduler used for all stack operations.
     */
    @NonNull Scheduler getScheduler();

    /**
     * Vends an observable configured appropriately for use with the BluetoothStack.
     */
    <T> Observable<T> newConfiguredObservable(Observable.OnSubscribe<T> onSubscribe);

    /**
     * Returns an observable that will continuously report the enabled state of the bluetooth stack.
     * <p>
     * This seems like something that would work predictably outside of the context of the wrapper,
     * but it's not. On some (all?) devices, the broadcast for this state change reports the wrong
     * values, so we provide a nice predictable interface for clients.
     *
     * @see #isEnabled() for instantaneous values.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    Observable<Boolean> enabled();

    /**
     * Returns whether or not Bluetooth is currently enabled.
     *
     * @see #enabled() for observations over time.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    boolean isEnabled();

    /**
     * Turns on the device's Bluetooth radio.
     *
     * @see ChangePowerStateException
     */
    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    })
    Observable<Void> turnOn();

    /**
     * Turns off the device's Bluetooth radio.
     *
     * @see ChangePowerStateException
     */
    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    })
    Observable<Void> turnOff();


    /**
     * Returns a boolean indicating whether or not a given error is
     * fatal in the context of the bluetooth stack implementation,
     * and the client code should disconnect and perform a rediscovery.
     */
    boolean errorRequiresReconnect(@Nullable Throwable e);


    /**
     * Returns the logger facade associated with the BluetoothStack.
     */
    @NonNull LoggerFacade getLogger();


    /**
     * Returns the level of support the stack has for the current device.
     */
    SupportLevel getDeviceSupportLevel();

    /**
     * Describes the level of support the current device has in the implementation.
     */
    enum SupportLevel {
        /**
         * The device is unsupported, one or more core operations are known to fail.
         */
        UNSUPPORTED,

        /**
         * The device has not been tested, so one or more core operations may not work.
         */
        UNTESTED,

        /**
         * The device is tested and known to work.
         */
        TESTED
    }
}
