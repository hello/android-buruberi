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
import android.content.Intent;
import android.os.Build;
import android.os.Parcelable;

import org.junit.Test;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBluetoothAdapter;

import is.hello.buruberi.bluetooth.errors.ChangePowerStateException;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;
import is.hello.buruberi.bluetooth.stacks.util.ErrorListener;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.buruberi.testing.BuruberiTestCase;
import is.hello.buruberi.testing.Sync;
import is.hello.buruberi.util.AdvertisingDataBuilder;
import is.hello.buruberi.util.Defaults;
import rx.Observable;

import static is.hello.buruberi.bluetooth.stacks.util.AdvertisingData.TYPE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS;
import static is.hello.buruberi.testing.Testing.DEVICE_ADDRESS;
import static is.hello.buruberi.testing.Testing.createMockDevice;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class NativeBluetoothStackTests extends BuruberiTestCase {
    private final ErrorListener errorListener = Defaults.createEmptyErrorListener();
    private final LoggerFacade loggerFacade = Defaults.createLogcatFacade();

    @Test
    @Config(sdk = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void createLeScannerJellyBeanMR2() {
        final NativeBluetoothStack stack = new NativeBluetoothStack(getContext(),
                                                                    errorListener,
                                                                    loggerFacade);
        final LePeripheralScanner scanner = stack.createLeScanner(new PeripheralCriteria());
        assertThat(scanner, is(instanceOf(LegacyLePeripheralScanner.class)));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void createLeScannerLollipop() {
        final NativeBluetoothStack stack = new NativeBluetoothStack(getContext(),
                                                                    errorListener,
                                                                    loggerFacade);
        final LePeripheralScanner scanner = stack.createLeScanner(new PeripheralCriteria());
        assertThat(scanner, is(instanceOf(LollipopLePeripheralScanner.class)));
    }

    @Test
    public void isEnabledNoAdapter() {
        getShadowBluetoothManager().setAdapter(null);

        final NativeBluetoothStack stack = new NativeBluetoothStack(getContext(),
                                                                    errorListener,
                                                                    loggerFacade);
        assertThat(stack.isEnabled(), is(false));
    }

    @Test
    public void isEnabled() {
        final ShadowBluetoothAdapter shadowAdapter = getShadowBluetoothAdapter();
        final NativeBluetoothStack stack = new NativeBluetoothStack(getContext(),
                                                                    errorListener,
                                                                    loggerFacade);

        shadowAdapter.setEnabled(false);
        assertThat(stack.isEnabled(), is(false));

        shadowAdapter.setEnabled(true);
        assertThat(stack.isEnabled(), is(true));
    }

    @Test
    public void enabledOn() {
        final ShadowBluetoothAdapter shadowAdapter = getShadowBluetoothAdapter();
        shadowAdapter.setEnabled(false);
        final NativeBluetoothStack stack = new NativeBluetoothStack(getContext(),
                                                                    errorListener,
                                                                    loggerFacade);
        assertThat(Sync.last(stack.enabled().take(1)), is(false));

        Intent stateChange = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED)
                .putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);
        getContext().sendBroadcast(stateChange);
        assertThat(Sync.last(stack.enabled().take(1)), is(true));
    }

    @Test
    public void enabledOff() {
        final ShadowBluetoothAdapter shadowAdapter = getShadowBluetoothAdapter();
        shadowAdapter.setEnabled(true);
        final NativeBluetoothStack stack = new NativeBluetoothStack(getContext(),
                                                                    errorListener,
                                                                    loggerFacade);
        assertThat(Sync.last(stack.enabled().take(1)), is(true));

        Intent stateChange = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED)
                .putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        getContext().sendBroadcast(stateChange);
        assertThat(Sync.last(stack.enabled().take(1)), is(false));
    }

    @Test
    public void enabledError() {
        final ShadowBluetoothAdapter shadowAdapter = getShadowBluetoothAdapter();
        shadowAdapter.setEnabled(true);
        final NativeBluetoothStack stack = new NativeBluetoothStack(getContext(),
                                                                    errorListener,
                                                                    loggerFacade);
        assertThat(Sync.last(stack.enabled().take(1)), is(true));

        Intent stateChange = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED)
                .putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
        getContext().sendBroadcast(stateChange);
        assertThat(Sync.last(stack.enabled().take(1)), is(false));
    }

    @Test
    public void turnOnSuccess() {
        final ShadowBluetoothAdapter shadowAdapter = getShadowBluetoothAdapter();
        shadowAdapter.setEnabled(false);
        final NativeBluetoothStack stack = new NativeBluetoothStack(getContext(),
                                                                    errorListener,
                                                                    loggerFacade);

        // NativeBluetoothStack#turnOn() returns a mirror, so this is fine.
        Observable<Void> turnOn = stack.turnOn();

        Intent stateChange = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED)
                .putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF)
                .putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_TURNING_ON);
        getContext().sendBroadcast(stateChange);

        stateChange.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_TURNING_ON)
                   .putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);
        getContext().sendBroadcast(stateChange);

        Sync.wrap(turnOn)
            .assertThat(is(nullValue()));
    }

    @Test
    public void turnOnFailure() {
        final ShadowBluetoothAdapter shadowAdapter = getShadowBluetoothAdapter();
        shadowAdapter.setEnabled(false);
        final NativeBluetoothStack stack = new NativeBluetoothStack(getContext(),
                                                                    errorListener,
                                                                    loggerFacade);

        // NativeBluetoothStack#turnOn() returns a mirror, so this is fine.
        Observable<Void> turnOn = stack.turnOn();

        Intent stateChange = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        getContext().sendBroadcast(stateChange);

        Sync.wrap(turnOn)
            .assertThrows(ChangePowerStateException.class);
    }

    @Test
    public void turnOffSuccess() {
        final ShadowBluetoothAdapter shadowAdapter = getShadowBluetoothAdapter();
        shadowAdapter.setEnabled(false);
        final NativeBluetoothStack stack = new NativeBluetoothStack(getContext(),
                                                                    errorListener,
                                                                    loggerFacade);

        // NativeBluetoothStack#turnOff() returns a mirror, so this is fine.
        Observable<Void> turnOff = stack.turnOff();

        Intent stateChange = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED)
                .putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_ON)
                .putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_TURNING_OFF);
        getContext().sendBroadcast(stateChange);

        stateChange.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_TURNING_OFF)
                   .putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        getContext().sendBroadcast(stateChange);

        Sync.wrap(turnOff)
            .assertThat(is(nullValue()));
    }

    @Test
    public void turnOffFailure() {
        final ShadowBluetoothAdapter shadowAdapter = getShadowBluetoothAdapter();
        shadowAdapter.setEnabled(false);
        final NativeBluetoothStack stack = new NativeBluetoothStack(getContext(),
                                                                    errorListener,
                                                                    loggerFacade);

        // NativeBluetoothStack#turnOff() returns a mirror, so this is fine.
        Observable<Void> turnOff = stack.turnOff();

        Intent stateChange = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        getContext().sendBroadcast(stateChange);

        Sync.wrap(turnOff)
            .assertThrows(ChangePowerStateException.class);
    }


    @Test(expected = IllegalArgumentException.class)
    public void saveStateSafety() {
        final NativeBluetoothStack stack = new NativeBluetoothStack(getContext(),
                                                                    errorListener,
                                                                    loggerFacade);
        stack.saveState(mock(GattPeripheral.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void restoreStateSafety() {
        final NativeBluetoothStack stack = new NativeBluetoothStack(getContext(),
                                                                    errorListener,
                                                                    loggerFacade);
        stack.restoreState(new Intent());
    }

    @Test
    public void restoreStateNullSafety() {
        final NativeBluetoothStack stack = new NativeBluetoothStack(getContext(),
                                                                    errorListener,
                                                                    loggerFacade);
        assertThat(stack.restoreState(null), is(nullValue()));
    }

    @Test
    public void saveRestoreState() {
        final NativeBluetoothStack stack = new NativeBluetoothStack(getContext(),
                                                                    errorListener,
                                                                    loggerFacade);
        getShadowBluetoothAdapter();
        final BluetoothDevice bluetoothDevice = createMockDevice(DEVICE_ADDRESS);
        final AdvertisingData advertisingData = new AdvertisingDataBuilder()
                .add(TYPE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS, "E1FE")
                .build();
        final int rssi = -50;

        final GattPeripheral outPeripheral = new NativeGattPeripheral(stack, bluetoothDevice,
                                                                      rssi, advertisingData);
        assertThat(outPeripheral.getAddress(), is(equalTo(DEVICE_ADDRESS)));

        final Parcelable outState = stack.saveState(outPeripheral);
        assertThat(outState, is(notNullValue()));

        final GattPeripheral inPeripheral = stack.restoreState(outState);
        assertThat(inPeripheral, is(notNullValue()));
        assertThat(inPeripheral.getScanTimeRssi(), is(equalTo(rssi)));
        assertThat(inPeripheral.getAddress(), is(equalTo(outPeripheral.getAddress())));
        assertThat(inPeripheral.getAdvertisingData().isEmpty(),
                   is(equalTo(outPeripheral.getAdvertisingData().isEmpty())));
    }
}
