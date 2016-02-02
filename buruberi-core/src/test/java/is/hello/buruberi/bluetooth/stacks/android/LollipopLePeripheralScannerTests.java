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
package is.hello.buruberi.bluetooth.stacks.android;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.SystemClock;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import is.hello.buruberi.bluetooth.errors.UserDisabledBuruberiException;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;
import is.hello.buruberi.bluetooth.stacks.util.ErrorListener;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.buruberi.testing.BuruberiShadows;
import is.hello.buruberi.testing.BuruberiTestCase;
import is.hello.buruberi.testing.ShadowBluetoothAdapterExt;
import is.hello.buruberi.testing.ShadowBluetoothLeScanner;
import is.hello.buruberi.testing.Testing;
import is.hello.buruberi.util.AdvertisingDataBuilder;
import is.hello.buruberi.util.Defaults;
import rx.Subscriber;
import rx.observers.Subscribers;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LollipopLePeripheralScannerTests extends BuruberiTestCase {
    private final ErrorListener errorListener = Defaults.createEmptyErrorListener();
    private final LoggerFacade loggerFacade = Defaults.createLogcatFacade();
    private NativeBluetoothStack stack;

    @Before
    public void setUp() {
        super.setUp();

        this.stack = spy(new NativeBluetoothStack(getContext(),
                                                  errorListener,
                                                  loggerFacade));
        doReturn(Testing.getNoOpScheduler()).when(stack).getScheduler();
    }

    @Test
    public void coalescesResults() {
        final ShadowBluetoothAdapterExt shadowAdapter = getShadowBluetoothAdapter();
        final ShadowBluetoothLeScanner shadowScanner = BuruberiShadows.shadowOf(shadowAdapter.getBluetoothLeScanner());
        final PeripheralCriteria criteria = new PeripheralCriteria();
        final LollipopLePeripheralScanner scanner = new LollipopLePeripheralScanner(stack, criteria);

        scanner.call(Subscribers.empty());
        assertThat(shadowScanner.getScanCallbacks(), hasItem(scanner));

        final BluetoothDevice device = Testing.createMockDevice();
        final ScanResult scanResult1 = new ScanResult(device,
                                                     Testing.EMPTY_SCAN_RECORD,
                                                     Testing.RSSI_DECENT,
                                                     SystemClock.elapsedRealtimeNanos());
        scanner.onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, scanResult1);

        final ScannedPeripheral scannedPeripheral = scanner.results.get(device.getAddress());
        assertThat(scannedPeripheral, is(notNullValue()));
        assertThat(scannedPeripheral.rssi, is(equalTo(Testing.RSSI_DECENT)));

        final ScanResult scanResult2 = new ScanResult(device,
                                                     Testing.EMPTY_SCAN_RECORD,
                                                     Testing.RSSI_BETTER,
                                                     SystemClock.elapsedRealtimeNanos());
        scanner.onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, scanResult2);

        final ScannedPeripheral scannedPeripheralAgain = scanner.results.get(device.getAddress());
        assertThat(scannedPeripheralAgain, is(notNullValue()));
        assertThat(scannedPeripheralAgain.rssi, is(equalTo(Testing.RSSI_BETTER)));
        assertThat(scannedPeripheralAgain, is(sameInstance(scannedPeripheral)));

        scanner.onConcludeScan();

        assertThat(shadowScanner.getScanCallbacks(), not(hasItem(scanner)));
    }

    @Test
    public void filtersByAdvertisingData() {
        final String serviceIdentifier = "23D1BCEA5F782315DEEF1212E1FE0000";
        final ShadowBluetoothAdapterExt shadowAdapter = getShadowBluetoothAdapter();
        final ShadowBluetoothLeScanner shadowScanner = BuruberiShadows.shadowOf(shadowAdapter.getBluetoothLeScanner());
        final PeripheralCriteria criteria = new PeripheralCriteria();
        criteria.addExactMatchPredicate(AdvertisingData.TYPE_SERVICE_DATA_128_BIT_UUID,
                                        serviceIdentifier);
        final LollipopLePeripheralScanner scanner = new LollipopLePeripheralScanner(stack, criteria);

        scanner.call(Subscribers.empty());
        assertThat(shadowScanner.getScanCallbacks(), hasItem(scanner));

        final BluetoothDevice device1 = Testing.createMockDevice();
        final byte[] advertisingData1 = new AdvertisingDataBuilder()
                .add(AdvertisingData.TYPE_SERVICE_DATA_128_BIT_UUID, serviceIdentifier)
                .buildRaw();
        final ScanResult scanResult1 = new ScanResult(device1,
                                                      Testing.parseScanRecordFromBytes(advertisingData1),
                                                      Testing.RSSI_DECENT,
                                                      SystemClock.elapsedRealtimeNanos());
        scanner.onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, scanResult1);

        final BluetoothDevice device2 = Testing.createMockDevice("BA:BE:CA:FE:BE:EF");
        final byte[] advertisingData2 = new AdvertisingDataBuilder()
                .add(AdvertisingData.TYPE_SERVICE_DATA_128_BIT_UUID,
                     "00D1BCEA5F782315DEEF1212E1FE00FF")
                .buildRaw();
        final ScanResult scanResult2 = new ScanResult(device2,
                                                      Testing.parseScanRecordFromBytes(advertisingData2),
                                                      Testing.RSSI_DECENT,
                                                      SystemClock.elapsedRealtimeNanos());
        scanner.onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, scanResult2);

        scanner.onConcludeScan();

        assertThat(shadowScanner.getScanCallbacks(), not(hasItem(scanner)));
        assertThat(scanner.results.keySet(), hasItem(Testing.DEVICE_ADDRESS));
        assertThat(scanner.results.keySet(), not(hasItem("BA:BE:CA:FE:BE:EF")));
    }

    @Test
    public void filtersByAddress() {
        final ShadowBluetoothAdapterExt shadowAdapter = getShadowBluetoothAdapter();
        final ShadowBluetoothLeScanner shadowScanner = BuruberiShadows.shadowOf(shadowAdapter.getBluetoothLeScanner());
        final PeripheralCriteria criteria = new PeripheralCriteria();
        criteria.addPeripheralAddress(Testing.DEVICE_ADDRESS);
        final LollipopLePeripheralScanner scanner = new LollipopLePeripheralScanner(stack, criteria);

        scanner.call(Subscribers.empty());
        assertThat(shadowScanner.getScanCallbacks(), hasItem(scanner));

        final BluetoothDevice device1 = Testing.createMockDevice();
        final ScanResult scanResult1 = new ScanResult(device1,
                                                     Testing.EMPTY_SCAN_RECORD,
                                                     Testing.RSSI_DECENT,
                                                     SystemClock.elapsedRealtimeNanos());
        scanner.onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, scanResult1);

        final BluetoothDevice device2 = Testing.createMockDevice("BA:BE:CA:FE:BE:EF");
        final ScanResult scanResult2 = new ScanResult(device2,
                                                      Testing.EMPTY_SCAN_RECORD,
                                                      Testing.RSSI_DECENT,
                                                      SystemClock.elapsedRealtimeNanos());
        scanner.onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, scanResult2);

        scanner.onConcludeScan();

        assertThat(shadowScanner.getScanCallbacks(), not(hasItem(scanner)));
        assertThat(scanner.results.keySet(), hasItem(Testing.DEVICE_ADDRESS));
        assertThat(scanner.results.keySet(), not(hasItem("BA:BE:CA:FE:BE:EF")));
    }

    @Test
    public void concludesAtLimit() {
        final ShadowBluetoothAdapterExt shadowAdapter = getShadowBluetoothAdapter();
        final ShadowBluetoothLeScanner shadowScanner = BuruberiShadows.shadowOf(shadowAdapter.getBluetoothLeScanner());
        final PeripheralCriteria criteria = new PeripheralCriteria();
        criteria.setLimit(1);
        final LollipopLePeripheralScanner scanner = new LollipopLePeripheralScanner(stack, criteria);

        scanner.call(Subscribers.empty());
        assertThat(shadowScanner.getScanCallbacks(), hasItem(scanner));

        final BluetoothDevice device = Testing.createMockDevice();
        final ScanResult scanResult = new ScanResult(device,
                                                      Testing.EMPTY_SCAN_RECORD,
                                                      Testing.RSSI_DECENT,
                                                      SystemClock.elapsedRealtimeNanos());
        scanner.onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, scanResult);

        scanner.onConcludeScan();

        assertThat(shadowScanner.getScanCallbacks(), not(hasItem(scanner)));
    }

    @Test
    public void handlesAdapterOffRaceConditionOnStart() {
        // Test for fix to <https://github.com/hello/android-buruberi/issues/11>
        final ShadowBluetoothAdapterExt shadowAdapter = getShadowBluetoothAdapter();
        final PeripheralCriteria criteria = new PeripheralCriteria();
        final LollipopLePeripheralScanner scanner = new LollipopLePeripheralScanner(stack, criteria);

        shadowAdapter.setState(BluetoothAdapter.STATE_OFF);
        final AtomicReference<Throwable> expectedFailure = new AtomicReference<>();
        scanner.call(new Subscriber<List<GattPeripheral>>() {
            @Override
            public void onCompleted() {
                fail("Scanner should not complete");
            }

            @Override
            public void onError(Throwable e) {
                expectedFailure.set(e);
            }

            @Override
            public void onNext(List<GattPeripheral> gattPeripherals) {
                fail("Scanner should not scan peripherals");
            }
        });
        assertThat(expectedFailure.get(), is(instanceOf(UserDisabledBuruberiException.class)));
    }

    @Test
    public void suppressesResultsWhenAdapterOff() {
        final ShadowBluetoothAdapterExt shadowAdapter = getShadowBluetoothAdapter();
        final ShadowBluetoothLeScanner shadowScanner = BuruberiShadows.shadowOf(shadowAdapter.getBluetoothLeScanner());
        final PeripheralCriteria criteria = new PeripheralCriteria();
        final LollipopLePeripheralScanner scanner = new LollipopLePeripheralScanner(stack, criteria);

        scanner.call(new Subscriber<List<GattPeripheral>>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                fail(e.getMessage());
            }

            @Override
            public void onNext(List<GattPeripheral> gattPeripherals) {
                assertThat(gattPeripherals, is(empty()));
            }
        });
        assertThat(shadowScanner.getScanCallbacks(), hasItem(scanner));

        final BluetoothDevice device = Testing.createMockDevice();
        final ScanResult scanResult = new ScanResult(device,
                                                     Testing.EMPTY_SCAN_RECORD,
                                                     Testing.RSSI_DECENT,
                                                     SystemClock.elapsedRealtimeNanos());
        scanner.onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, scanResult);

        assertThat(scanner.results.keySet(), is(not(empty())));

        shadowAdapter.setState(BluetoothAdapter.STATE_OFF);
        scanner.onConcludeScan();
    }
}
