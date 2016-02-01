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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import is.hello.buruberi.bluetooth.errors.BondException;
import is.hello.buruberi.bluetooth.errors.ConnectionStateException;
import is.hello.buruberi.bluetooth.errors.GattException;
import is.hello.buruberi.bluetooth.errors.ServiceDiscoveryException;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.GattService;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;
import is.hello.buruberi.bluetooth.stacks.android.NativeGattPeripheral.ConnectedOnSubscribe;
import is.hello.buruberi.bluetooth.stacks.util.ErrorListener;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.testing.BuruberiShadows;
import is.hello.buruberi.testing.BuruberiTestCase;
import is.hello.buruberi.testing.ShadowBluetoothDeviceExt;
import is.hello.buruberi.testing.ShadowBluetoothGatt;
import is.hello.buruberi.testing.ShadowBluetoothManager;
import is.hello.buruberi.testing.Testing;
import is.hello.buruberi.util.Defaults;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class NativeGattPeripheralTests extends BuruberiTestCase {
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

    private NativeGattPeripheral createConnectedPeripheral() {
        final BluetoothDevice device = Testing.createMockDevice();
        getShadowBluetoothManager().setConnectionState(device, BluetoothProfile.STATE_CONNECTED);
        final NativeGattPeripheral peripheral = new NativeGattPeripheral(stack,
                                                                         device,
                                                                         Testing.RSSI_DECENT,
                                                                         Testing.EMPTY_ADVERTISING_DATA);
        peripheral.gatt = device.connectGatt(getContext(), false, peripheral.gattDispatcher);
        return peripheral;
    }


    //region Connectivity

    @Test
    public void getTransportFromConnectFlags() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        assertThat(peripheral.getTransportFromConnectFlags(GattPeripheral.CONNECT_FLAG_TRANSPORT_AUTO),
                   is(equalTo(BluetoothDeviceCompat.TRANSPORT_AUTO)));
        assertThat(peripheral.getTransportFromConnectFlags(GattPeripheral.CONNECT_FLAG_TRANSPORT_BREDR),
                   is(equalTo(BluetoothDeviceCompat.TRANSPORT_BREDR)));
        assertThat(peripheral.getTransportFromConnectFlags(GattPeripheral.CONNECT_FLAG_TRANSPORT_LE),
                   is(equalTo(BluetoothDeviceCompat.TRANSPORT_LE)));
        assertThat(peripheral.getTransportFromConnectFlags(0),
                   is(equalTo(BluetoothDeviceCompat.TRANSPORT_AUTO)));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void disconnectSideEffects() throws Exception {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();

        final NativeGattService fakeService = spy(new NativeGattService(Testing.createMockGattService(),
                                                                        peripheral));
        final Map<UUID, NativeGattService> services = new HashMap<>();
        services.put(fakeService.getUuid(), fakeService);
        peripheral.services = services;

        try (TestReceiver receiver = new TestReceiver(new IntentFilter(GattPeripheral.ACTION_DISCONNECTED))) {
            final BluetoothGatt gatt = peripheral.gatt;
            final ShadowBluetoothGatt shadowGatt = BuruberiShadows.shadowOf(gatt);
            shadowGatt.getGattCallback().onConnectionStateChange(gatt,
                                                                 BluetoothGatt.GATT_FAILURE,
                                                                 BluetoothGatt.STATE_DISCONNECTED);

            assertThat(receiver.wasInvoked, is(true));
            assertThat(peripheral.gatt, is(nullValue()));
            assertThat(peripheral.services.isEmpty(), is(true));
            verify(fakeService).dispatchDisconnect();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void connectSuccess() {
        final BluetoothDevice device = Testing.createMockDevice();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();
        final NativeGattPeripheral peripheral = new NativeGattPeripheral(stack,
                                                                         device,
                                                                         Testing.RSSI_DECENT,
                                                                         Testing.EMPTY_ADVERTISING_DATA);

        final Testing.Result<GattPeripheral> result = new Testing.Result<>();
        peripheral.connect(GattPeripheral.CONNECT_FLAG_DEFAULTS, timeout).subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt shadowGatt = BuruberiShadows.shadowOf(gatt);
        shadowGatt.verifyCall(ShadowBluetoothGatt.Call.CONNECT, timeout);
        verify(timeout).setTimeoutAction(any(Action0.class), any(Scheduler.class));
        verify(timeout).schedule();

        shadowGatt.getGattCallback().onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS,
                                                             BluetoothGatt.STATE_CONNECTED);
        assertThat(result.isCompleted(), is(true));
        assertThat(result.getValues().size(), is(equalTo(1)));
        verify(timeout).unschedule();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void connectRecoverableFailureSuccess() throws Exception {
        final BluetoothDevice device = Testing.createMockDevice();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();
        final NativeGattPeripheral peripheral = new NativeGattPeripheral(stack,
                                                                         device,
                                                                         Testing.RSSI_DECENT,
                                                                         Testing.EMPTY_ADVERTISING_DATA);

        final Testing.Result<GattPeripheral> result = new Testing.Result<>();
        peripheral.connect(GattPeripheral.CONNECT_FLAG_DEFAULTS, timeout).subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt shadowGatt = BuruberiShadows.shadowOf(gatt);
        shadowGatt.verifyCall(ShadowBluetoothGatt.Call.CONNECT, timeout);
        verify(timeout).setTimeoutAction(any(Action0.class), any(Scheduler.class));
        verify(timeout).schedule();

        try (TestReceiver receiver = new TestReceiver(new IntentFilter(GattPeripheral.ACTION_DISCONNECTED))) {
            shadowGatt.clearCalls();
            shadowGatt.getGattCallback().onConnectionStateChange(gatt, GattException.GATT_CONN_TERMINATE_LOCAL_HOST,
                                                                 BluetoothGatt.STATE_DISCONNECTED);
            shadowGatt.verifyCall(ShadowBluetoothGatt.Call.CONNECT, timeout);
            verify(timeout).reschedule();
            assertThat(receiver.wasInvoked, is(false));

            shadowGatt.getGattCallback().onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS,
                                                                 BluetoothGatt.STATE_CONNECTED);
            assertThat(result.isCompleted(), is(true));
            assertThat(result.getValues().size(), is(equalTo(1)));
            verify(timeout).unschedule();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void connectRecoverableFailureFailure() throws Exception {
        final BluetoothDevice device = Testing.createMockDevice();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();
        final NativeGattPeripheral peripheral = new NativeGattPeripheral(stack,
                                                                         device,
                                                                         Testing.RSSI_DECENT,
                                                                         Testing.EMPTY_ADVERTISING_DATA);

        final Testing.Result<GattPeripheral> result = new Testing.Result<>();
        peripheral.connect(GattPeripheral.CONNECT_FLAG_DEFAULTS, timeout).subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt shadowGatt = BuruberiShadows.shadowOf(gatt);
        shadowGatt.verifyCall(ShadowBluetoothGatt.Call.CONNECT, timeout);
        verify(timeout).setTimeoutAction(any(Action0.class), any(Scheduler.class));
        verify(timeout).schedule();

        try (TestReceiver receiver = new TestReceiver(new IntentFilter(GattPeripheral.ACTION_DISCONNECTED))) {
            shadowGatt.clearCalls();
            shadowGatt.getGattCallback().onConnectionStateChange(gatt, GattException.GATT_CONN_TERMINATE_LOCAL_HOST,
                                                                 BluetoothGatt.STATE_DISCONNECTED);
            shadowGatt.verifyCall(ShadowBluetoothGatt.Call.CONNECT, timeout);
            verify(timeout).reschedule();
            assertThat(receiver.wasInvoked, is(false));

            shadowGatt.getGattCallback().onConnectionStateChange(gatt, GattException.GATT_CONN_TERMINATE_LOCAL_HOST,
                                                                 BluetoothGatt.STATE_DISCONNECTED);
            assertThat(result.isCompleted(), is(false));
            assertThat(result.getError(), is(instanceOf(GattException.class)));
            verify(timeout).unschedule();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void connectFailure() {
        final BluetoothDevice device = Testing.createMockDevice();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();
        final NativeGattPeripheral peripheral = new NativeGattPeripheral(stack,
                                                                         device,
                                                                         Testing.RSSI_DECENT,
                                                                         Testing.EMPTY_ADVERTISING_DATA);

        final Testing.Result<GattPeripheral> result = new Testing.Result<>();
        peripheral.connect(GattPeripheral.CONNECT_FLAG_DEFAULTS, timeout).subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt shadowGatt = BuruberiShadows.shadowOf(gatt);
        shadowGatt.verifyCall(ShadowBluetoothGatt.Call.CONNECT, timeout);
        verify(timeout).setTimeoutAction(any(Action0.class), any(Scheduler.class));
        verify(timeout).schedule();

        shadowGatt.getGattCallback().onConnectionStateChange(gatt, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED,
                                                             BluetoothGatt.STATE_DISCONNECTED);
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(GattException.class)));
        verify(timeout).unschedule();
    }

    @Test
    public void disconnectSuccess() {
        final BluetoothDevice device = Testing.createMockDevice();
        getShadowBluetoothManager().setConnectionState(device, BluetoothProfile.STATE_CONNECTED);
        final NativeGattPeripheral peripheral = new NativeGattPeripheral(stack,
                                                                         device,
                                                                         Testing.RSSI_DECENT,
                                                                         Testing.EMPTY_ADVERTISING_DATA);
        final BluetoothGatt gatt = device.connectGatt(getContext(), false, peripheral.gattDispatcher);
        peripheral.gatt = gatt;

        final Testing.Result<GattPeripheral> result = new Testing.Result<>();
        peripheral.disconnect().subscribe(result);

        final ShadowBluetoothGatt shadowGatt = BuruberiShadows.shadowOf(gatt);
        shadowGatt.verifyCall(ShadowBluetoothGatt.Call.DISCONNECT);

        shadowGatt.getGattCallback().onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothGatt.STATE_DISCONNECTING);
        assertThat(result.isCompleted(), is(false));

        shadowGatt.getGattCallback().onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothGatt.STATE_DISCONNECTED);
        assertThat(result.isCompleted(), is(true));
        assertThat(result.getValues().size(), is(equalTo(1)));
    }

    @Test
    public void disconnectFailure() {
        final BluetoothDevice device = Testing.createMockDevice();
        getShadowBluetoothManager().setConnectionState(device, BluetoothProfile.STATE_CONNECTED);
        final NativeGattPeripheral peripheral = new NativeGattPeripheral(stack,
                                                                         device,
                                                                         Testing.RSSI_DECENT,
                                                                         Testing.EMPTY_ADVERTISING_DATA);
        final BluetoothGatt gatt = device.connectGatt(getContext(), false, peripheral.gattDispatcher);
        peripheral.gatt = gatt;

        final Testing.Result<GattPeripheral> result = new Testing.Result<>();
        peripheral.disconnect().subscribe(result);

        final ShadowBluetoothGatt shadowGatt = BuruberiShadows.shadowOf(gatt);
        shadowGatt.verifyCall(ShadowBluetoothGatt.Call.DISCONNECT);

        shadowGatt.getGattCallback().onConnectionStateChange(gatt, BluetoothGatt.GATT_FAILURE, BluetoothGatt.STATE_DISCONNECTED);
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(GattException.class)));
    }

    @Test
    public void getConnectionStatus() {
        final ShadowBluetoothManager shadowBluetoothManager = getShadowBluetoothManager();
        final BluetoothDevice device = Testing.createMockDevice();
        final NativeGattPeripheral peripheral = new NativeGattPeripheral(stack,
                                                                         device,
                                                                         Testing.RSSI_DECENT,
                                                                         Testing.EMPTY_ADVERTISING_DATA);
        peripheral.gatt = Testing.createMockGatt();

        shadowBluetoothManager.setConnectionState(device, BluetoothProfile.STATE_CONNECTED);
        assertThat(peripheral.getConnectionStatus(), is(equalTo(GattPeripheral.STATUS_CONNECTED)));

        shadowBluetoothManager.setConnectionState(device, BluetoothProfile.STATE_CONNECTING);
        assertThat(peripheral.getConnectionStatus(), is(equalTo(GattPeripheral.STATUS_CONNECTING)));

        shadowBluetoothManager.setConnectionState(device, BluetoothProfile.STATE_DISCONNECTED);
        assertThat(peripheral.getConnectionStatus(), is(equalTo(GattPeripheral.STATUS_DISCONNECTED)));

        shadowBluetoothManager.setConnectionState(device, BluetoothProfile.STATE_DISCONNECTING);
        assertThat(peripheral.getConnectionStatus(), is(equalTo(GattPeripheral.STATUS_DISCONNECTING)));
    }

    //endregion


    //region Bonding

    @Test
    public void createBondSuccess() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();

        final Testing.Result<GattPeripheral> result = new Testing.Result<>();
        peripheral.createBond().subscribe(result);

        final Intent broadcast = new Intent(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        broadcast.putExtra(BluetoothDevice.EXTRA_DEVICE, peripheral.bluetoothDevice);

        broadcast.putExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDING);
        broadcast.putExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE);
        getContext().sendBroadcast(broadcast);

        broadcast.putExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDED);
        broadcast.putExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_BONDING);
        getContext().sendBroadcast(broadcast);

        assertThat(result.isCompleted(), is(true));
        assertThat(result.getValues().size(), is(equalTo(1)));
    }

    @Test
    public void createBondFailure() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();

        final Testing.Result<GattPeripheral> result = new Testing.Result<>();
        peripheral.createBond().subscribe(result);

        final Intent broadcast = new Intent(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        broadcast.putExtra(BluetoothDevice.EXTRA_DEVICE, peripheral.bluetoothDevice);

        broadcast.putExtra(BondException.EXTRA_REASON, BondException.REASON_REMOVED);
        broadcast.putExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
        getContext().sendBroadcast(broadcast);

        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(BondException.class)));
    }

    @Test
    public void removeBondSuccess() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        BuruberiShadows.shadowOf(peripheral.bluetoothDevice).setBondState(BluetoothDevice.BOND_BONDED);
        final OperationTimeout timeout = Testing.createMockOperationTimeout();

        final Testing.Result<GattPeripheral> result = new Testing.Result<>();
        peripheral.removeBond(timeout).subscribe(result);
        verify(timeout).setTimeoutAction(any(Action0.class), any(Scheduler.class));
        verify(timeout).schedule();

        final Intent broadcast = new Intent(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        broadcast.putExtra(BluetoothDevice.EXTRA_DEVICE, peripheral.bluetoothDevice);

        broadcast.putExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDING);
        broadcast.putExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_BONDED);
        getContext().sendBroadcast(broadcast);

        broadcast.putExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
        broadcast.putExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_BONDING);
        getContext().sendBroadcast(broadcast);

        assertThat(result.isCompleted(), is(true));
        assertThat(result.getValues().size(), is(equalTo(1)));
        verify(timeout).unschedule();
    }

    @Test
    public void removeBondTimeoutSuccess() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        BuruberiShadows.shadowOf(peripheral.bluetoothDevice).setBondState(BluetoothDevice.BOND_BONDED);
        final FakeOperationTimeout timeout = spy(new FakeOperationTimeout());

        final Testing.Result<GattPeripheral> result = new Testing.Result<>();
        peripheral.removeBond(timeout).subscribe(result);
        verify(timeout).setTimeoutAction(any(Action0.class), any(Scheduler.class));
        verify(timeout).schedule();

        BuruberiShadows.shadowOf(peripheral.bluetoothDevice).setBondState(BluetoothDevice.BOND_NONE);
        timeout.action.call();

        assertThat(result.isCompleted(), is(true));
        assertThat(result.getValues().size(), is(equalTo(1)));
    }

    @Test
    public void removeBondFailure() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        BuruberiShadows.shadowOf(peripheral.bluetoothDevice).setBondState(BluetoothDevice.BOND_BONDED);
        final OperationTimeout timeout = Testing.createMockOperationTimeout();

        final Testing.Result<GattPeripheral> result = new Testing.Result<>();
        peripheral.removeBond(timeout).subscribe(result);
        verify(timeout).setTimeoutAction(any(Action0.class), any(Scheduler.class));
        verify(timeout).schedule();

        final Intent broadcast = new Intent(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        broadcast.putExtra(BluetoothDevice.EXTRA_DEVICE, peripheral.bluetoothDevice);

        broadcast.putExtra(BondException.EXTRA_REASON, BondException.REASON_REMOVED);
        broadcast.putExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
        getContext().sendBroadcast(broadcast);

        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(BondException.class)));
        verify(timeout).unschedule();
    }


    @Test
    public void getBondStatus() {
        final BluetoothDevice device = Testing.createMockDevice();
        final ShadowBluetoothDeviceExt shadowDevice = BuruberiShadows.shadowOf(device);
        final NativeGattPeripheral peripheral = new NativeGattPeripheral(stack,
                                                                         device,
                                                                         Testing.RSSI_DECENT,
                                                                         Testing.EMPTY_ADVERTISING_DATA);

        shadowDevice.setBondState(BluetoothDevice.BOND_NONE);
        assertThat(peripheral.getBondStatus(), is(equalTo(GattPeripheral.BOND_NONE)));

        shadowDevice.setBondState(BluetoothDevice.BOND_BONDING);
        assertThat(peripheral.getBondStatus(), is(equalTo(GattPeripheral.BOND_CHANGING)));

        shadowDevice.setBondState(BluetoothDevice.BOND_BONDED);
        assertThat(peripheral.getBondStatus(), is(equalTo(GattPeripheral.BOND_BONDED)));
    }

    //endregion


    //region Discovering Services

    @SuppressWarnings("ConstantConditions")
    @Test
    public void discoverServicesSuccess() {
        doReturn(new Testing.NoOpScheduler(true))
                .when(stack)
                .getScheduler();
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();

        final Testing.Result<Map<UUID, ? extends GattService>> result = new Testing.Result<>();
        peripheral.discoverServices(timeout).subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt shadowGatt = BuruberiShadows.shadowOf(gatt);
        shadowGatt.verifyCall(ShadowBluetoothGatt.Call.DISCOVER_SERVICES);
        verify(timeout).setTimeoutAction(any(Action0.class), any(Scheduler.class));
        verify(timeout).schedule();

        shadowGatt.setServices(Collections.singletonList(Testing.createMockGattService()));
        shadowGatt.getGattCallback().onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS);
        assertThat(result.isCompleted(), is(true));

        final Map<UUID, ? extends GattService> services = result.getValues().get(0);
        assertThat(services.keySet(), hasItem(Testing.SERVICE_PRIMARY));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void discoverServicesFailure() {
        doReturn(new Testing.NoOpScheduler(true))
                .when(stack)
                .getScheduler();
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();

        final Testing.Result<Map<UUID, ? extends GattService>> result = new Testing.Result<>();
        peripheral.discoverServices(timeout).subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt shadowGatt = BuruberiShadows.shadowOf(gatt);
        shadowGatt.verifyCall(ShadowBluetoothGatt.Call.DISCOVER_SERVICES);
        verify(timeout).setTimeoutAction(any(Action0.class), any(Scheduler.class));
        verify(timeout).schedule();

        shadowGatt.getGattCallback().onServicesDiscovered(gatt, BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION);
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(GattException.class)));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void discoverServiceSuccess() {
        doReturn(new Testing.NoOpScheduler(true))
                .when(stack)
                .getScheduler();
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();

        final Testing.Result<GattService> result = new Testing.Result<>();
        peripheral.discoverService(Testing.SERVICE_PRIMARY, timeout).subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt shadowGatt = BuruberiShadows.shadowOf(gatt);
        shadowGatt.verifyCall(ShadowBluetoothGatt.Call.DISCOVER_SERVICES);
        verify(timeout).setTimeoutAction(any(Action0.class), any(Scheduler.class));
        verify(timeout).schedule();

        shadowGatt.setServices(Collections.singletonList(Testing.createMockGattService()));
        shadowGatt.getGattCallback().onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS);
        assertThat(result.isCompleted(), is(true));
        assertThat(result.getValues().size(), is(equalTo(1)));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void discoverServiceGattFailure() {
        doReturn(new Testing.NoOpScheduler(true))
                .when(stack)
                .getScheduler();
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();

        final Testing.Result<GattService> result = new Testing.Result<>();
        peripheral.discoverService(Testing.SERVICE_PRIMARY, timeout).subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt shadowGatt = BuruberiShadows.shadowOf(gatt);
        shadowGatt.verifyCall(ShadowBluetoothGatt.Call.DISCOVER_SERVICES);
        verify(timeout).setTimeoutAction(any(Action0.class), any(Scheduler.class));
        verify(timeout).schedule();

        shadowGatt.getGattCallback().onServicesDiscovered(gatt, BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION);
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(GattException.class)));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void discoverServiceNotFoundFailure() {
        doReturn(new Testing.NoOpScheduler(true))
                .when(stack)
                .getScheduler();
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();

        final Testing.Result<GattService> result = new Testing.Result<>();
        peripheral.discoverService(UUID.randomUUID(), timeout).subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt shadowGatt = BuruberiShadows.shadowOf(gatt);
        shadowGatt.verifyCall(ShadowBluetoothGatt.Call.DISCOVER_SERVICES);
        verify(timeout).setTimeoutAction(any(Action0.class), any(Scheduler.class));
        verify(timeout).schedule();

        shadowGatt.setServices(Collections.singletonList(Testing.createMockGattService()));
        shadowGatt.getGattCallback().onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS);
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(ServiceDiscoveryException.class)));
    }

    //endregion


    //region Packet Dispatching

    @SuppressWarnings("ConstantConditions")
    @Test
    public void onCharacteristicChanged() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();

        final NativeGattService fakeService = spy(new NativeGattService(Testing.createMockGattService(),
                                                                        peripheral));
        final Map<UUID, NativeGattService> services = new HashMap<>();
        services.put(fakeService.getUuid(), fakeService);
        peripheral.services = services;

        final BluetoothGattCharacteristic characteristic =
                fakeService.wrappedService.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        characteristic.setValue(new byte[] {0x0, 0x1, 0x2, 0x3});
        peripheral.onCharacteristicChanged(peripheral.gatt,
                                           characteristic);

        verify(fakeService).dispatchNotify(Testing.WRITE_CHARACTERISTIC,
                                           new byte[]{0x0, 0x1, 0x2, 0x3});
    }

    //endregion

    //region ConnectedOnSubscribeTests

    @Test
    public void connectedConnectedOnSubscribe() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();

        final ConnectedOnSubscribe<Boolean> onSubscribe = new ConnectedOnSubscribe<Boolean>(peripheral) {
            @Override
            public void onSubscribe(@NonNull BluetoothGatt gatt,
                                    @NonNull Subscriber<? super Boolean> subscriber) {
                assertThat(gatt, is(notNullValue()));

                subscriber.onNext(true);
                subscriber.onCompleted();
            }
        };

        @SuppressWarnings("unchecked")
        final Subscriber<Boolean> fakeSubscriber = mock(Subscriber.class);
        onSubscribe.call(fakeSubscriber);

        verify(fakeSubscriber).onNext(true);
        verify(fakeSubscriber).onCompleted();
    }

    @Test
    public void notConnectedConnectedOnSubscribe() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        getShadowBluetoothManager().setConnectionState(peripheral.bluetoothDevice,
                                                       BluetoothProfile.STATE_DISCONNECTED);

        final ConnectedOnSubscribe<Boolean> onSubscribe = new ConnectedOnSubscribe<Boolean>(peripheral) {
            @Override
            public void onSubscribe(@NonNull BluetoothGatt gatt,
                                    @NonNull Subscriber<? super Boolean> subscriber) {
                assertThat(gatt, is(notNullValue()));

                subscriber.onNext(true);
                subscriber.onCompleted();
            }
        };

        @SuppressWarnings("unchecked")
        final Subscriber<Boolean> fakeSubscriber = mock(Subscriber.class);
        onSubscribe.call(fakeSubscriber);

        verify(fakeSubscriber).onError(any(ConnectionStateException.class));
    }

    @Test
    public void noGattConnectedOnSubscribe() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        peripheral.gatt = null;

        final ConnectedOnSubscribe<Boolean> onSubscribe = new ConnectedOnSubscribe<Boolean>(peripheral) {
            @Override
            public void onSubscribe(@NonNull BluetoothGatt gatt,
                                    @NonNull Subscriber<? super Boolean> subscriber) {
                assertThat(gatt, is(notNullValue()));

                subscriber.onNext(true);
                subscriber.onCompleted();
            }
        };

        @SuppressWarnings("unchecked")
        final Subscriber<Boolean> fakeSubscriber = mock(Subscriber.class);
        onSubscribe.call(fakeSubscriber);

        verify(fakeSubscriber).onError(any(ConnectionStateException.class));
    }

    //endregion


    class TestReceiver extends BroadcastReceiver implements AutoCloseable {
        public boolean wasInvoked = false;

        TestReceiver(@NonNull IntentFilter filter) {
            LocalBroadcastManager.getInstance(getContext())
                                 .registerReceiver(this, filter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            this.wasInvoked = true;
        }

        @Override
        public void close() throws Exception {
            LocalBroadcastManager.getInstance(getContext())
                                 .unregisterReceiver(this);
        }
    }

    static class FakeOperationTimeout implements OperationTimeout {
        Action0 action;

        @Override
        public void schedule() {

        }

        @Override
        public void unschedule() {

        }

        @Override
        public void reschedule() {

        }

        @Override
        public void setTimeoutAction(@NonNull Action0 action, @NonNull Scheduler scheduler) {
            this.action = action;
        }
    }
}
