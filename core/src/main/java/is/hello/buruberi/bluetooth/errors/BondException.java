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

import android.support.annotation.NonNull;

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;
import is.hello.buruberi.util.NonGuaranteed;

/**
 * Indicates that the state of a peripheral bond could not be altered by Buruberi.
 */
public class BondException extends BuruberiException {
    /**
     * The reason for the bond alteration's failure.
     * <p>
     * This value may change between OS versions.
     */
    public final int reason;

    public BondException(int reason) {
        super(getReasonString(reason));

        this.reason = reason;
    }

    /**
     * @return {@link true} if {@link #REASON_REMOVED} is reported; {@code false} otherwise.
     *
     * @see #REASON_REMOVED for more info.
     */
    @Override
    public boolean isInstabilityLikely() {
        return (reason == BondException.REASON_REMOVED);
    }


    //region Bonding Errors

    /**
     * The reason for a bond state change to have occurred.
     * <p>
     * This extra is not publicly exposed before Android Lollipop / API Level 21,
     * and is only partially public in API Level 21. See the SDK Android Lollipop
     * source for the BluetoothDevice class for more info.
     *
     * @see android.bluetooth.BluetoothDevice#ACTION_BOND_STATE_CHANGED
     */
    @NonGuaranteed
    public static final String EXTRA_REASON = "android.bluetooth.device.extra.REASON";

    /**
     * A bonding attempt failed for unknown reasons. Specific to Buruberi.
     */
    public static final int REASON_UNKNOWN_FAILURE = -1;

    /**
     * A bonding attempt failed up front because the bond alteration APIs
     * changed between OS versions. Specific to Buruberi.
     *
     * @see GattPeripheral#removeBond(OperationTimeout) for more info.
     */
    public static final int REASON_ANDROID_API_CHANGED = -2;

    /**
     * A bond alternation succeeded.
     */
    @NonGuaranteed
    public static final int BOND_SUCCESS = 0;

    /**
     * A bond attempt failed because pins did not match, or remote device did
     * not respond to pin request in time
     */
    @NonGuaranteed
    public static final int REASON_AUTH_FAILED = 1;

    /**
     * A bond attempt failed because the other side explicitly rejected
     * bonding
     */
    @NonGuaranteed
    public static final int REASON_AUTH_REJECTED = 2;

    /**
     * A bond attempt failed because we canceled the bonding process
     */
    @NonGuaranteed
    public static final int REASON_AUTH_CANCELED = 3;

    /**
     * A bond attempt failed because we could not contact the remote device
     */
    @NonGuaranteed
    public static final int REASON_REMOTE_DEVICE_DOWN = 4;

    /**
     * A bond attempt failed because a discovery is in progress
     */
    @NonGuaranteed
    public static final int REASON_DISCOVERY_IN_PROGRESS = 5;

    /**
     * A bond attempt failed because of authentication timeout
     */
    @NonGuaranteed
    public static final int REASON_AUTH_TIMEOUT = 6;

    /**
     * A bond attempt failed because of repeated attempts
     */
    @NonGuaranteed
    public static final int REASON_REPEATED_ATTEMPTS = 7;

    /**
     * A bond attempt failed because we received an Authentication Cancel
     * by remote end
     */
    @NonGuaranteed
    public static final int REASON_REMOTE_AUTH_CANCELED = 8;

    /**
     * A bond attempt failed because the bond state of the peripheral is
     * different from what the phone expected. On some devices, encountering
     * this error will result in the bluetooth drivers breaking until restart.
     */
    @NonGuaranteed
    public static final int REASON_REMOVED = 9;


    /**
     * Returns the corresponding constant name for a given {@code REASON_*} value.
     *
     * @see #REASON_UNKNOWN_FAILURE
     * @see #REASON_ANDROID_API_CHANGED
     * @see #BOND_SUCCESS
     * @see #REASON_AUTH_FAILED
     * @see #REASON_AUTH_REJECTED
     * @see #REASON_AUTH_CANCELED
     * @see #REASON_REMOTE_DEVICE_DOWN
     * @see #REASON_DISCOVERY_IN_PROGRESS
     * @see #REASON_AUTH_TIMEOUT
     * @see #REASON_REPEATED_ATTEMPTS
     * @see #REASON_REMOTE_AUTH_CANCELED
     * @see #REASON_REMOVED
     */
    public static @NonNull String getReasonString(int reason) {
        switch (reason) {
            case BOND_SUCCESS:
                return "BOND_SUCCESS";

            case REASON_AUTH_FAILED:
                return "REASON_AUTH_FAILED";

            case REASON_AUTH_REJECTED:
                return "REASON_AUTH_REJECTED";

            case REASON_AUTH_CANCELED:
                return "REASON_AUTH_CANCELED";

            case REASON_REMOTE_DEVICE_DOWN:
                return "REASON_REMOTE_DEVICE_DOWN";

            case REASON_DISCOVERY_IN_PROGRESS:
                return "REASON_DISCOVERY_IN_PROGRESS";

            case REASON_AUTH_TIMEOUT:
                return "REASON_AUTH_TIMEOUT";

            case REASON_REPEATED_ATTEMPTS:
                return "REASON_REPEATED_ATTEMPTS";

            case REASON_REMOTE_AUTH_CANCELED:
                return "REASON_REMOTE_AUTH_CANCELED";

            case REASON_REMOVED:
                return "REASON_REMOVED";

            case REASON_ANDROID_API_CHANGED:
                return "REASON_ANDROID_API_CHANGED";

            case REASON_UNKNOWN_FAILURE:
            default:
                return "REASON_UNKNOWN_FAILURE (" + reason + ")";
        }
    }

    /**
     * Returns the corresponding constant name for a given {@code GattPeripheral#BOND_*} value.
     *
     * @see GattPeripheral#BOND_NONE
     * @see GattPeripheral#BOND_CHANGING
     * @see GattPeripheral#BOND_BONDED
     */
    public static @NonNull String getBondStateString(int bondState) {
        switch (bondState) {
            case GattPeripheral.BOND_NONE:
                return "BOND_NONE";

            case GattPeripheral.BOND_CHANGING:
                return "BOND_CHANGING";

            case GattPeripheral.BOND_BONDED:
                return "BOND_BONDED";

            default:
                return "UNKNOWN (" + bondState + ")";
        }
    }

    //endregion
}
