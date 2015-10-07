package is.hello.buruberi.bluetooth.stacks.android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;
import is.hello.buruberi.bluetooth.stacks.util.ErrorListener;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.buruberi.testing.BuruberiTestCase;
import is.hello.buruberi.testing.Testing;
import is.hello.buruberi.testing.shadows.ShadowBluetoothAdapterExt;
import is.hello.buruberi.util.AdvertisingDataBuilder;
import is.hello.buruberi.util.Defaults;
import rx.Subscriber;
import rx.observers.Subscribers;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class LegacyLePeripheralScannerTests extends BuruberiTestCase {
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
        final PeripheralCriteria criteria = new PeripheralCriteria();
        final LegacyLePeripheralScanner scanner = new LegacyLePeripheralScanner(stack, criteria);

        scanner.call(Subscribers.empty());
        assertThat(shadowAdapter.getLeScanCallbacks(), hasItem(scanner));

        final BluetoothDevice device = Testing.createMockDevice();
        scanner.onLeScan(device, Testing.RSSI_DECENT, Testing.EMPTY_SCAN_RESPONSE);

        final ScannedPeripheral scannedPeripheral = scanner.results.get(device.getAddress());
        assertThat(scannedPeripheral, is(notNullValue()));
        assertThat(scannedPeripheral.rssi, is(equalTo(Testing.RSSI_DECENT)));

        scanner.onLeScan(device, Testing.RSSI_BETTER, Testing.EMPTY_SCAN_RESPONSE);

        final ScannedPeripheral scannedPeripheralAgain = scanner.results.get(device.getAddress());
        assertThat(scannedPeripheralAgain, is(notNullValue()));
        assertThat(scannedPeripheralAgain.rssi, is(equalTo(Testing.RSSI_BETTER)));
        assertThat(scannedPeripheralAgain, is(sameInstance(scannedPeripheral)));

        scanner.onConcludeScan();

        assertThat(shadowAdapter.getLeScanCallbacks(), not(hasItem(scanner)));
    }

    @Test
    public void filtersByAdvertisingData() {
        final String serviceIdentifier = "23D1BCEA5F782315DEEF1212E1FE0000";
        final ShadowBluetoothAdapterExt shadowAdapter = getShadowBluetoothAdapter();
        final PeripheralCriteria criteria = new PeripheralCriteria();
        criteria.addExactMatchPredicate(AdvertisingData.TYPE_SERVICE_DATA_128_BIT_UUID,
                                        serviceIdentifier);
        final LegacyLePeripheralScanner scanner = new LegacyLePeripheralScanner(stack, criteria);

        scanner.call(Subscribers.empty());
        assertThat(shadowAdapter.getLeScanCallbacks(), hasItem(scanner));

        final BluetoothDevice device1 = Testing.createMockDevice();
        final byte[] advertisingData1 = new AdvertisingDataBuilder()
                .add(AdvertisingData.TYPE_SERVICE_DATA_128_BIT_UUID, serviceIdentifier)
                .buildRaw();
        scanner.onLeScan(device1, Testing.RSSI_DECENT, advertisingData1);

        final BluetoothDevice device2 = Testing.createMockDevice("BA:BE:CA:FE:BE:EF");
        final byte[] advertisingData2 = new AdvertisingDataBuilder()
                .add(AdvertisingData.TYPE_SERVICE_DATA_128_BIT_UUID,
                     "00D1BCEA5F782315DEEF1212E1FE00FF")
                .buildRaw();
        scanner.onLeScan(device2, Testing.RSSI_BETTER, advertisingData2);

        scanner.onConcludeScan();

        assertThat(shadowAdapter.getLeScanCallbacks(), not(hasItem(scanner)));
        assertThat(scanner.results.keySet(), hasItem(Testing.DEVICE_ADDRESS));
        assertThat(scanner.results.keySet(), not(hasItem("BA:BE:CA:FE:BE:EF")));
    }

    @Test
    public void filtersByAddress() {
        final ShadowBluetoothAdapterExt shadowAdapter = getShadowBluetoothAdapter();
        final PeripheralCriteria criteria = new PeripheralCriteria();
        criteria.addPeripheralAddress(Testing.DEVICE_ADDRESS);
        final LegacyLePeripheralScanner scanner = new LegacyLePeripheralScanner(stack, criteria);

        scanner.call(Subscribers.empty());
        assertThat(shadowAdapter.getLeScanCallbacks(), hasItem(scanner));

        final BluetoothDevice device1 = Testing.createMockDevice();
        scanner.onLeScan(device1, Testing.RSSI_DECENT, Testing.EMPTY_SCAN_RESPONSE);

        final BluetoothDevice device2 = Testing.createMockDevice("BA:BE:CA:FE:BE:EF");
        scanner.onLeScan(device2, Testing.RSSI_BETTER, Testing.EMPTY_SCAN_RESPONSE);

        scanner.onConcludeScan();

        assertThat(shadowAdapter.getLeScanCallbacks(), not(hasItem(scanner)));
        assertThat(scanner.results.keySet(), hasItem(Testing.DEVICE_ADDRESS));
        assertThat(scanner.results.keySet(), not(hasItem("BA:BE:CA:FE:BE:EF")));
    }

    @Test
    public void concludesAtLimit() {
        final ShadowBluetoothAdapterExt shadowAdapter = getShadowBluetoothAdapter();
        final PeripheralCriteria criteria = new PeripheralCriteria();
        criteria.setLimit(1);
        final LegacyLePeripheralScanner scanner = new LegacyLePeripheralScanner(stack, criteria);

        scanner.call(Subscribers.empty());
        assertThat(shadowAdapter.getLeScanCallbacks(), hasItem(scanner));

        final BluetoothDevice device = Testing.createMockDevice();
        scanner.onLeScan(device, Testing.RSSI_DECENT, Testing.EMPTY_SCAN_RESPONSE);

        assertThat(shadowAdapter.getLeScanCallbacks(), not(hasItem(scanner)));
    }

    @Test
    public void suppressesResultsWhenAdapterOff() {
        final ShadowBluetoothAdapterExt shadowAdapter = getShadowBluetoothAdapter();
        final PeripheralCriteria criteria = new PeripheralCriteria();
        final LegacyLePeripheralScanner scanner = new LegacyLePeripheralScanner(stack, criteria);

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
        assertThat(shadowAdapter.getLeScanCallbacks(), hasItem(scanner));

        final BluetoothDevice device = Testing.createMockDevice();
        scanner.onLeScan(device, Testing.RSSI_DECENT, Testing.EMPTY_SCAN_RESPONSE);
        assertThat(scanner.results.keySet(), is(not(empty())));

        shadowAdapter.setState(BluetoothAdapter.STATE_OFF);
        scanner.onConcludeScan();

        assertThat(shadowAdapter.getLeScanCallbacks(), not(hasItem(scanner)));
    }
}
