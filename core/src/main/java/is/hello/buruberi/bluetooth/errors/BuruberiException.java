/*
 * Copyright 2015 Hello, Inc
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

import android.support.annotation.Nullable;

/**
 * The superclass of all errors propagated by Buruberi.
 * <p>
 * May be subclassed by client code if having a single superclass
 * for all Bluetooth-related errors is a desirable attribute.
 *
 * @see BondException for bond manager related problems.
 * @see GattException for BLE operation related problems.
 */
public class BuruberiException extends RuntimeException {
    /**
     * Convenience method provided to call {@link #isInstabilityLikely()}
     * without repeatedly performing a class check and cast.
     *
     * @param e The object to check against.
     * @return {@code true} if the object is a {@code BluetoothException},
     *         and it indicates that instability is likely.
     */
    public static boolean isInstabilityLikely(@Nullable Throwable e) {
        return ((e != null) &&
                (e instanceof BuruberiException) &&
                ((BuruberiException) e).isInstabilityLikely());
    }

    public BuruberiException(String detailMessage) {
        super(detailMessage);
    }

    public BuruberiException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    /**
     * Several failures reported by Buruberi can either be caused by,
     * or can cause as a side effect, Bluetooth driver instability.
     * <p>
     * Bluetooth driver instability varies by vendor, OS Version, and
     * device chip-set. The return value of this method should be
     * considered a guideline, not a hard and fast rule.
     * <p>
     * If this method returns {@code true}, it is generally a good idea
     * to power cycle Bluetooth on the host device before trying
     * subsequent operations.
     *
     * @return Whether or not Bluetooth driver instability is suspected.
     */
    public boolean isInstabilityLikely() {
        return false;
    }
}
