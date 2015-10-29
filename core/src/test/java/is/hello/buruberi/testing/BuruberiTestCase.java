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
import android.bluetooth.BluetoothManager;
import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContextImpl;
import org.robolectric.util.ReflectionHelpers;

import is.hello.buruberi.BuildConfig;
import is.hello.buruberi.testing.shadows.BuruberiShadows;
import is.hello.buruberi.testing.shadows.ShadowBluetoothAdapterExt;
import is.hello.buruberi.testing.shadows.ShadowBluetoothDeviceExt;
import is.hello.buruberi.testing.shadows.ShadowBluetoothGatt;
import is.hello.buruberi.testing.shadows.ShadowBluetoothLeScanner;
import is.hello.buruberi.testing.shadows.ShadowBluetoothManager;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class,
        sdk = 21,
        shadows = {
                ShadowBluetoothManager.class,
                ShadowBluetoothAdapterExt.class,
                ShadowBluetoothLeScanner.class,
                ShadowBluetoothGatt.class,
                ShadowBluetoothDeviceExt.class,
        })
public abstract class BuruberiTestCase {
    protected BuruberiTestCase() {
        try {
            final Context baseContext = RuntimeEnvironment.application.getBaseContext();
            final BluetoothManager bluetoothManager = ReflectionHelpers.newInstance(BluetoothManager.class);
            final ShadowContextImpl shadowContext = (ShadowContextImpl) Shadows.shadowOf(baseContext);
            shadowContext.setSystemService(Context.BLUETOOTH_SERVICE, bluetoothManager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setUp() {
        ShadowBluetoothAdapterExt shadowBluetoothAdapter = getShadowBluetoothAdapter();
        shadowBluetoothAdapter.setState(BluetoothAdapter.STATE_ON);
        shadowBluetoothAdapter.setEnabled(true);
    }

    @After
    public void tearDown() {
        ShadowBluetoothManager shadowBluetoothManager = getShadowBluetoothManager();
        shadowBluetoothManager.clearConnectionStates();
        shadowBluetoothManager.setAdapter(BluetoothAdapter.getDefaultAdapter());
    }

    protected Context getContext() {
        return RuntimeEnvironment.application.getApplicationContext();
    }

    protected ShadowBluetoothManager getShadowBluetoothManager() {
        BluetoothManager bluetoothManager = (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        return BuruberiShadows.shadowOf(bluetoothManager);
    }

    protected ShadowBluetoothAdapterExt getShadowBluetoothAdapter() {
        return BuruberiShadows.shadowOf(BluetoothAdapter.getDefaultAdapter());
    }
}
