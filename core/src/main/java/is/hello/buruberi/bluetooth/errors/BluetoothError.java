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
 * Super type of all errors yielded by the low level Bluetooth stack.
 *
 * @see PeripheralBondAlterationError
 * @see BluetoothGattError
 * @see is.hello.buruberi.bluetooth.errors.PeripheralConnectionError
 * @see is.hello.buruberi.bluetooth.errors.PeripheralServiceDiscoveryFailedError
 */
public class BluetoothError extends RuntimeException {
    public static boolean isFatal(@Nullable Throwable e) {
        return ((e != null) &&
                (e instanceof BluetoothError) &&
                ((BluetoothError) e).isFatal());
    }

    public BluetoothError(String detailMessage) {
        super(detailMessage);
    }

    public BluetoothError(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public BluetoothError(Throwable throwable) {
        super(throwable);
    }

    public boolean isFatal() {
        return false;
    }
}
