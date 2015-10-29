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

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.os.Build;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
@Implements(BluetoothLeScanner.class)
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ShadowBluetoothLeScanner {
    private final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    private final Set<ScanCallback> scanCallbacks = new HashSet<>();

    public Set<ScanCallback> getScanCallbacks() {
        return Collections.unmodifiableSet(scanCallbacks);
    }

    @Implementation
    public void startScan(List<ScanFilter> filters,
                          ScanSettings settings,
                          ScanCallback callback) {
        if (adapter.getState() != BluetoothAdapter.STATE_ON) {
            throw new IllegalStateException("BT Adapter is not turned ON");
        }

        scanCallbacks.add(callback);
    }

    @Implementation
    public void startScan(final ScanCallback callback) {
        startScan(null, new ScanSettings.Builder().build(), callback);
    }

    @Implementation
    public void stopScan(ScanCallback callback) {
        if (adapter.getState() != BluetoothAdapter.STATE_ON) {
            throw new IllegalStateException("BT Adapter is not turned ON");
        }

        scanCallbacks.remove(callback);
    }
}
