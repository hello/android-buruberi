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
package is.hello.buruberi.testing;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.support.annotation.NonNull;

import org.robolectric.Shadows;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.ShadowContextImpl;
import org.robolectric.util.ReflectionHelpers;

/**
 * Convenience methods for extracting shadows specific to the Buruberi project.
 * <p>
 * Project local copy of {@link org.robolectric.Shadows}.
 */
public class BuruberiShadows {
    public static void setUpSystemServices(@NonNull Context context) {
        final BluetoothManager bluetoothManager = ReflectionHelpers.newInstance(BluetoothManager.class);
        final ShadowContextImpl shadowContext = (ShadowContextImpl) Shadows.shadowOf(context);
        shadowContext.setSystemService(Context.BLUETOOTH_SERVICE, bluetoothManager);
    }

    public static ShadowBluetoothManager shadowOf(@NonNull BluetoothManager bluetoothManager) {
        return (ShadowBluetoothManager) ShadowExtractor.extract(bluetoothManager);
    }

    public static ShadowBluetoothDeviceExt shadowOf(@NonNull BluetoothDevice bluetoothDevice) {
        return (ShadowBluetoothDeviceExt) ShadowExtractor.extract(bluetoothDevice);
    }

    public static ShadowBluetoothGatt shadowOf(@NonNull BluetoothGatt bluetoothGatt) {
        return (ShadowBluetoothGatt) ShadowExtractor.extract(bluetoothGatt);
    }

    public static ShadowBluetoothAdapterExt shadowOf(@NonNull BluetoothAdapter bluetoothAdapter) {
        return (ShadowBluetoothAdapterExt) ShadowExtractor.extract(bluetoothAdapter);
    }

    public static ShadowBluetoothLeScanner shadowOf(@NonNull BluetoothLeScanner bluetoothAdapter) {
        return (ShadowBluetoothLeScanner) ShadowExtractor.extract(bluetoothAdapter);
    }
}
