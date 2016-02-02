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
import android.support.annotation.RequiresPermission;

import java.util.List;

import is.hello.buruberi.bluetooth.errors.ChangePowerStateException;
import is.hello.buruberi.bluetooth.errors.UserDisabledBuruberiException;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import rx.Observable;
import rx.Scheduler;

/**
 * Provides methods for discovering Bluetooth Low Energy peripherals, inspecting user
 * device support level, and controlling power state of the device's Bluetooth radio.
 *
 * @see GattPeripheral
 */
public interface BluetoothStack {
    /**
     * The log tag that of BluetoothStack use.
     */
    String LOG_TAG = "Bluetooth." + BluetoothStack.class.getSimpleName();


    /**
     * Performs a scan for peripherals matching a given set of criteria.
     * <p>
     * Yields {@link UserDisabledBuruberiException} if
     * the device's Bluetooth radio is currently disabled.
     *
     * @see PeripheralCriteria
     */
    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    })
    @NonNull Observable<List<GattPeripheral>> discoverPeripherals(@NonNull PeripheralCriteria peripheralCriteria);

    /**
     * Returns the RxJava {@code Scheduler} used for all stack operations.
     */
    @NonNull Scheduler getScheduler();

    /**
     * Vends an observable configured appropriately for use with this {@code BluetoothStack}.
     * @return A configured {@code Observable}.
     * @see #getScheduler()
     */
    <T> Observable<T> newConfiguredObservable(@NonNull Observable.OnSubscribe<T> onSubscribe);

    /**
     * Returns an observable that will continuously report the power state
     * of the device's bluetooth radio.
     *
     * @see #isEnabled() for one time values.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    Observable<Boolean> enabled();

    /**
     * Returns the current power state of the device's bluetooth radio.
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
     * Returns the logger facade associated with the {@code BluetoothStack}.
     */
    @NonNull LoggerFacade getLogger();
}
