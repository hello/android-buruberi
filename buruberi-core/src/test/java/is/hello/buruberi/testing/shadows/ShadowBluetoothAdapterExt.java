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
package is.hello.buruberi.testing.shadows;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.os.Build;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowBluetoothAdapter;
import org.robolectric.util.ReflectionHelpers;

@SuppressWarnings("unused")
@Implements(
        value = BluetoothAdapter.class,
        inheritImplementationMethods = true
)
public class ShadowBluetoothAdapterExt extends ShadowBluetoothAdapter {
    private BluetoothLeScanner bluetoothLeScanner;

    @Implementation
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BluetoothLeScanner getBluetoothLeScanner() {
        if (bluetoothLeScanner == null) {
            this.bluetoothLeScanner = ReflectionHelpers.newInstance(BluetoothLeScanner.class);
        }

        return bluetoothLeScanner;
    }
}
