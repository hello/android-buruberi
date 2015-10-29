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

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import is.hello.buruberi.bluetooth.stacks.android.DeviceSupport;
import rx.functions.Func1;

/**
 * Describes what a {@see is.hello.buruberi.bluetooth.stacks.BluetoothStack} should look for when
 * performing a BLE scan. Provides simple device address matching, maximum match count and
 * duration constraints, and a free-form system for matching against advertising data scanned
 * from available BLE peripherals.
 */
public final class PeripheralCriteria {
    /**
     * The default timeout duration.
     */
    public static final int DEFAULT_DURATION_MS = 10000;

    /**
     * Device addresses to match against.
     * <p>
     * This list being empty has the effect of all addresses being acceptable.
     */
    public final List<String> peripheralAddresses = new ArrayList<>();

    /**
     * A list of predicate functions that will be called to determine
     * whether or not a collection of advertising data matches this
     * criteria object. <em>All predicates must evaluate to true
     * for the criteria to match a potential peripheral.</em>
     */
    public final List<Func1<AdvertisingData, Boolean>> predicates = new ArrayList<>();

    /**
     * The maximum number of peripherals to scan before stopping.
     */
    public int limit = Integer.MAX_VALUE;

    /**
     * The maximum amount of time that can pass before the scan is ended.
     */
    public long duration = DEFAULT_DURATION_MS;

    /**
     * Whether or not to perform a high power scan before performing a low energy scan.
     * Required for some devices to be able to detect advertising data.
     *
     * @see is.hello.buruberi.bluetooth.stacks.android.DeviceSupport#isHighPowerPreScanNeeded()
     */
    public boolean wantsHighPowerPreScan = DeviceSupport.isHighPowerPreScanNeeded();

    /**
     * Returns a configured criteria that will search for one instance of a given address.
     */
    public static @NonNull PeripheralCriteria forAddress(@NonNull String address) {
        PeripheralCriteria criteria = new PeripheralCriteria();
        criteria.addPeripheralAddress(address);
        criteria.setLimit(1);
        return criteria;
    }


    //region Builder

    /**
     * Adds a peripheral address to match against when scanning.
     * <p>
     * The first address changes the scanning behavior from
     * matching any address, to just matching the address added.
     */
    public PeripheralCriteria addPeripheralAddress(@NonNull String address) {
        peripheralAddresses.add(address);
        return this;
    }


    /**
     * Adds a predicate functor to invoke when determining if the criteria
     * matches a given advertising data collection. All predicates must return
     * true for the criteria to be considered satisfied.
     */
    public PeripheralCriteria addPredicate(@NonNull Func1<AdvertisingData, Boolean> predicate) {
        predicates.add(predicate);
        return this;
    }

    /**
     * Adds a predicate to check that an advertising data record exactly matches a given byte payload.
     */
    public PeripheralCriteria addExactMatchPredicate(final int type, final @NonNull byte[] toMatch) {
        return addPredicate(new Func1<AdvertisingData, Boolean>() {
            @Override
            public Boolean call(AdvertisingData advertisingData) {
                return advertisingData.anyRecordMatches(type, new Func1<byte[], Boolean>() {
                    @Override
                    public Boolean call(byte[] match) {
                        return Arrays.equals(match, toMatch);
                    }
                });
            }
        });
    }

    public PeripheralCriteria addExactMatchPredicate(final int type, final @NonNull String toMatch) {
        return addExactMatchPredicate(type, Bytes.fromString(toMatch));
    }

    /**
     * Adds a predicate to check that an advertising data record starts with a given byte payload.
     */
    public PeripheralCriteria addStartsWithPredicate(final int type, final @NonNull byte[] prefix) {
        return addPredicate(new Func1<AdvertisingData, Boolean>() {
            @Override
            public Boolean call(AdvertisingData advertisingData) {
                return advertisingData.anyRecordMatches(type, new Func1<byte[], Boolean>() {
                    @Override
                    public Boolean call(byte[] match) {
                        return Bytes.startWith(match, prefix);
                    }
                });
            }
        });
    }

    public PeripheralCriteria addStartsWithPredicate(int type, @NonNull String prefix) {
        return addStartsWithPredicate(type, Bytes.fromString(prefix));
    }


    /**
     * Sets the maximum number of peripherals to scan for before stopping. Defaults to unbounded.
     */
    public PeripheralCriteria setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Sets the maximum duration of the scan.
     *
     * @see #DEFAULT_DURATION_MS
     */
    public PeripheralCriteria setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    /**
     * Sets whether or not to perform a high power scan before
     * performing a lower power discovery.
     * <p>
     * A high power scan is required by some phones to properly
     * detect BLE advertising data. However, high power scans
     * are both slow and power intensive, so they should not
     * be used without the user requesting it.
     *
     * @see is.hello.buruberi.bluetooth.stacks.android.DeviceSupport#isHighPowerPreScanNeeded()
     */
    public PeripheralCriteria setWantsHighPowerPreScan(boolean wantsHighPowerPreScan) {
        this.wantsHighPowerPreScan = wantsHighPowerPreScan;
        return this;
    }

    //endregion

    /**
     * Used by bluetooth stack implementations. Returns whether or not
     * the criteria is satisfied by a given advertising data collection.
     */
    public boolean matches(@NonNull AdvertisingData scanResponses) {
        for (Func1<AdvertisingData, Boolean> predicate : predicates) {
            if (!predicate.call(scanResponses)) {
                return false;
            }
        }

        return true;
    }
}
