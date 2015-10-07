package is.hello.buruberi.bluetooth.stacks.android;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import is.hello.buruberi.bluetooth.errors.BluetoothConnectionLostError;
import is.hello.buruberi.bluetooth.errors.BluetoothGattError;
import is.hello.buruberi.bluetooth.errors.PeripheralBondAlterationError;
import is.hello.buruberi.bluetooth.errors.PeripheralServiceDiscoveryFailedError;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;
import is.hello.buruberi.bluetooth.stacks.PeripheralService;
import is.hello.buruberi.bluetooth.stacks.util.ErrorListener;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.testing.BuruberiTestCase;
import is.hello.buruberi.testing.Testing;
import is.hello.buruberi.testing.shadows.BuruberiShadows;
import is.hello.buruberi.testing.shadows.ShadowBluetoothDeviceExt;
import is.hello.buruberi.testing.shadows.ShadowBluetoothGatt;
import is.hello.buruberi.testing.shadows.ShadowBluetoothManager;
import is.hello.buruberi.util.Defaults;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class NativeGattPeripheralTests extends BuruberiTestCase {
    private static final byte[] WRITE_PAYLOAD = {0xC, 0xA, 0xF, 0xE};

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

    @SuppressWarnings("ConstantConditions")
    @Test
    public void unexpectedDisconnectBroadcast() throws Exception {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();

        try (TestReceiver receiver = new TestReceiver(new IntentFilter(GattPeripheral.ACTION_DISCONNECTED))) {
            final BluetoothGatt gatt = peripheral.gatt;
            final ShadowBluetoothGatt shadowGatt = BuruberiShadows.shadowOf(gatt);
            shadowGatt.getGattCallback().onConnectionStateChange(gatt,
                                                                 BluetoothGatt.GATT_FAILURE,
                                                                 BluetoothGatt.STATE_DISCONNECTED);

            assertThat(receiver.wasInvoked, is(true));
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
        peripheral.connect(timeout).subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt shadowGatt = BuruberiShadows.shadowOf(gatt);
        shadowGatt.verifyCall(ShadowBluetoothGatt.Call.CONNECT, timeout);
        verify(timeout).setTimeoutAction(any(Action0.class), any(Scheduler.class));
        verify(timeout).schedule();

        shadowGatt.getGattCallback().onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothGatt.STATE_CONNECTED);
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
        peripheral.connect(timeout).subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt shadowGatt = BuruberiShadows.shadowOf(gatt);
        shadowGatt.verifyCall(ShadowBluetoothGatt.Call.CONNECT, timeout);
        verify(timeout).setTimeoutAction(any(Action0.class), any(Scheduler.class));
        verify(timeout).schedule();

        try (TestReceiver receiver = new TestReceiver(new IntentFilter(GattPeripheral.ACTION_DISCONNECTED))) {
            shadowGatt.clearCalls();
            shadowGatt.getGattCallback().onConnectionStateChange(gatt, BluetoothGattError.GATT_CONN_TERMINATE_LOCAL_HOST, BluetoothGatt.STATE_DISCONNECTED);
            shadowGatt.verifyCall(ShadowBluetoothGatt.Call.CONNECT, timeout);
            verify(timeout).reschedule();
            assertThat(receiver.wasInvoked, is(false));

            shadowGatt.getGattCallback().onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothGatt.STATE_CONNECTED);
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
        peripheral.connect(timeout).subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt shadowGatt = BuruberiShadows.shadowOf(gatt);
        shadowGatt.verifyCall(ShadowBluetoothGatt.Call.CONNECT, timeout);
        verify(timeout).setTimeoutAction(any(Action0.class), any(Scheduler.class));
        verify(timeout).schedule();

        try (TestReceiver receiver = new TestReceiver(new IntentFilter(GattPeripheral.ACTION_DISCONNECTED))) {
            shadowGatt.clearCalls();
            shadowGatt.getGattCallback().onConnectionStateChange(gatt, BluetoothGattError.GATT_CONN_TERMINATE_LOCAL_HOST, BluetoothGatt.STATE_DISCONNECTED);
            shadowGatt.verifyCall(ShadowBluetoothGatt.Call.CONNECT, timeout);
            verify(timeout).reschedule();
            assertThat(receiver.wasInvoked, is(false));

            shadowGatt.getGattCallback().onConnectionStateChange(gatt, BluetoothGattError.GATT_CONN_TERMINATE_LOCAL_HOST, BluetoothGatt.STATE_DISCONNECTED);
            assertThat(result.isCompleted(), is(false));
            assertThat(result.getError(), is(instanceOf(BluetoothGattError.class)));
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
        peripheral.connect(timeout).subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt shadowGatt = BuruberiShadows.shadowOf(gatt);
        shadowGatt.verifyCall(ShadowBluetoothGatt.Call.CONNECT, timeout);
        verify(timeout).setTimeoutAction(any(Action0.class), any(Scheduler.class));
        verify(timeout).schedule();

        shadowGatt.getGattCallback().onConnectionStateChange(gatt, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, BluetoothGatt.STATE_DISCONNECTED);
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(BluetoothGattError.class)));
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
        assertThat(result.getError(), is(instanceOf(BluetoothGattError.class)));
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

        broadcast.putExtra(PeripheralBondAlterationError.EXTRA_REASON, PeripheralBondAlterationError.REASON_REMOVED);
        broadcast.putExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
        getContext().sendBroadcast(broadcast);

        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(PeripheralBondAlterationError.class)));
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

        broadcast.putExtra(PeripheralBondAlterationError.EXTRA_REASON, PeripheralBondAlterationError.REASON_REMOVED);
        broadcast.putExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
        getContext().sendBroadcast(broadcast);

        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(PeripheralBondAlterationError.class)));
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
        assertThat(peripheral.getBondStatus(), is(equalTo(GattPeripheral.BOND_BONDING)));

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

        final Testing.Result<Map<UUID, PeripheralService>> result = new Testing.Result<>();
        peripheral.discoverServices(timeout).subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt shadowGatt = BuruberiShadows.shadowOf(gatt);
        shadowGatt.verifyCall(ShadowBluetoothGatt.Call.DISCOVER_SERVICES);
        verify(timeout).setTimeoutAction(any(Action0.class), any(Scheduler.class));
        verify(timeout).schedule();

        shadowGatt.setServices(Collections.singletonList(Testing.createMockGattService()));
        shadowGatt.getGattCallback().onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS);
        assertThat(result.isCompleted(), is(true));

        final Map<UUID, PeripheralService> services = result.getValues().get(0);
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

        final Testing.Result<Map<UUID, PeripheralService>> result = new Testing.Result<>();
        peripheral.discoverServices(timeout).subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt shadowGatt = BuruberiShadows.shadowOf(gatt);
        shadowGatt.verifyCall(ShadowBluetoothGatt.Call.DISCOVER_SERVICES);
        verify(timeout).setTimeoutAction(any(Action0.class), any(Scheduler.class));
        verify(timeout).schedule();

        shadowGatt.getGattCallback().onServicesDiscovered(gatt, BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION);
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(BluetoothGattError.class)));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void discoverServiceSuccess() {
        doReturn(new Testing.NoOpScheduler(true))
                .when(stack)
                .getScheduler();
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();

        final Testing.Result<PeripheralService> result = new Testing.Result<>();
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

        final Testing.Result<PeripheralService> result = new Testing.Result<>();
        peripheral.discoverService(Testing.SERVICE_PRIMARY, timeout).subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt shadowGatt = BuruberiShadows.shadowOf(gatt);
        shadowGatt.verifyCall(ShadowBluetoothGatt.Call.DISCOVER_SERVICES);
        verify(timeout).setTimeoutAction(any(Action0.class), any(Scheduler.class));
        verify(timeout).schedule();

        shadowGatt.getGattCallback().onServicesDiscovered(gatt, BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION);
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(BluetoothGattError.class)));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void discoverServiceNotFoundFailure() {
        doReturn(new Testing.NoOpScheduler(true))
                .when(stack)
                .getScheduler();
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();

        final Testing.Result<PeripheralService> result = new Testing.Result<>();
        peripheral.discoverService(UUID.randomUUID(), timeout).subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt shadowGatt = BuruberiShadows.shadowOf(gatt);
        shadowGatt.verifyCall(ShadowBluetoothGatt.Call.DISCOVER_SERVICES);
        verify(timeout).setTimeoutAction(any(Action0.class), any(Scheduler.class));
        verify(timeout).schedule();

        shadowGatt.setServices(Collections.singletonList(Testing.createMockGattService()));
        shadowGatt.getGattCallback().onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS);
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(PeripheralServiceDiscoveryFailedError.class)));
    }

    //endregion


    //region Commands

    @SuppressWarnings("ConstantConditions")
    @Test
    public void enableNotificationSuccess() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final BluetoothGattService nativeService = Testing.createMockGattService();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();
        final Observable<UUID> enable = peripheral.enableNotification(new NativePeripheralService(nativeService),
                                                                      Testing.WRITE_CHARACTERISTIC,
                                                                      Testing.NOTIFY_DESCRIPTOR,
                                                                      timeout);

        final Testing.Result<UUID> result = new Testing.Result<>();
        enable.subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt gattShadow = BuruberiShadows.shadowOf(gatt);
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.SET_CHAR_NOTIFICATION,
                              Matchers.any(BluetoothGattCharacteristic.class),
                              Matchers.equalTo(true));
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.WRITE_DESCRIPTOR,
                              Matchers.any(BluetoothGattDescriptor.class));
        verify(timeout).setTimeoutAction(Mockito.any(Action0.class), Mockito.any(Scheduler.class));
        verify(timeout).schedule();

        final BluetoothGattCharacteristic characteristic = nativeService.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Testing.NOTIFY_DESCRIPTOR);
        verify(descriptor).setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        gattShadow.getGattCallback().onDescriptorWrite(gatt, descriptor, BluetoothGatt.GATT_SUCCESS);
        assertThat(result.getValues().size(), is(equalTo(1)));
        assertThat(result.isCompleted(), is(true));
        verify(timeout).unschedule();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void enableNotificationFailure() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final BluetoothGattService nativeService = Testing.createMockGattService();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();
        final Observable<UUID> enable = peripheral.enableNotification(new NativePeripheralService(nativeService),
                                                                      Testing.WRITE_CHARACTERISTIC,
                                                                      Testing.NOTIFY_DESCRIPTOR,
                                                                      timeout);

        final Testing.Result<UUID> result = new Testing.Result<>();
        enable.subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt gattShadow = BuruberiShadows.shadowOf(gatt);
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.SET_CHAR_NOTIFICATION,
                              Matchers.any(BluetoothGattCharacteristic.class),
                              Matchers.equalTo(true));
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.WRITE_DESCRIPTOR,
                              Matchers.any(BluetoothGattDescriptor.class));
        verify(timeout).setTimeoutAction(Mockito.any(Action0.class), Mockito.any(Scheduler.class));
        verify(timeout).schedule();

        final BluetoothGattCharacteristic characteristic = nativeService.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Testing.NOTIFY_DESCRIPTOR);
        verify(descriptor).setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        gattShadow.getGattCallback().onDescriptorWrite(gatt, descriptor, BluetoothGatt.GATT_WRITE_NOT_PERMITTED);
        assertThat(result.getValues().size(), is(equalTo(0)));
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(BluetoothGattError.class)));
        verify(timeout).unschedule();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void enableNotificationDisconnect() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final BluetoothGattService nativeService = Testing.createMockGattService();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();
        final Observable<UUID> enable = peripheral.enableNotification(new NativePeripheralService(nativeService),
                                                                      Testing.WRITE_CHARACTERISTIC,
                                                                      Testing.NOTIFY_DESCRIPTOR,
                                                                      timeout);

        final Testing.Result<UUID> result = new Testing.Result<>();
        enable.subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt gattShadow = BuruberiShadows.shadowOf(gatt);
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.SET_CHAR_NOTIFICATION,
                              Matchers.any(BluetoothGattCharacteristic.class),
                              Matchers.equalTo(true));
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.WRITE_DESCRIPTOR,
                              Matchers.any(BluetoothGattDescriptor.class));
        verify(timeout).setTimeoutAction(Mockito.any(Action0.class), Mockito.any(Scheduler.class));
        verify(timeout).schedule();

        final BluetoothGattCharacteristic characteristic = nativeService.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Testing.NOTIFY_DESCRIPTOR);
        verify(descriptor).setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        gattShadow.getGattCallback().onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothGatt.STATE_DISCONNECTED);
        assertThat(result.getValues().size(), is(equalTo(0)));
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(BluetoothConnectionLostError.class)));
        verify(timeout).unschedule();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void disableNotificationSuccess() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final BluetoothGattService nativeService = Testing.createMockGattService();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();
        final Observable<UUID> enable = peripheral.disableNotification(new NativePeripheralService(nativeService),
                                                                       Testing.WRITE_CHARACTERISTIC,
                                                                       Testing.NOTIFY_DESCRIPTOR,
                                                                       timeout);

        final Testing.Result<UUID> result = new Testing.Result<>();
        enable.subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt gattShadow = BuruberiShadows.shadowOf(gatt);
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.SET_CHAR_NOTIFICATION,
                              Matchers.any(BluetoothGattCharacteristic.class),
                              Matchers.equalTo(false));
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.WRITE_DESCRIPTOR,
                              Matchers.any(BluetoothGattDescriptor.class));
        verify(timeout).setTimeoutAction(Mockito.any(Action0.class), Mockito.any(Scheduler.class));
        verify(timeout).schedule();

        final BluetoothGattCharacteristic characteristic = nativeService.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Testing.NOTIFY_DESCRIPTOR);
        verify(descriptor).setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);

        gattShadow.getGattCallback().onDescriptorWrite(gatt, descriptor, BluetoothGatt.GATT_SUCCESS);
        assertThat(result.getValues().size(), is(equalTo(1)));
        assertThat(result.isCompleted(), is(true));
        verify(timeout).unschedule();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void disableNotificationFailure() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final BluetoothGattService nativeService = Testing.createMockGattService();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();
        final Observable<UUID> enable = peripheral.disableNotification(new NativePeripheralService(nativeService),
                                                                       Testing.WRITE_CHARACTERISTIC,
                                                                       Testing.NOTIFY_DESCRIPTOR,
                                                                       timeout);

        final Testing.Result<UUID> result = new Testing.Result<>();
        enable.subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt gattShadow = BuruberiShadows.shadowOf(gatt);
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.SET_CHAR_NOTIFICATION,
                              Matchers.any(BluetoothGattCharacteristic.class),
                              Matchers.equalTo(false));
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.WRITE_DESCRIPTOR,
                              Matchers.any(BluetoothGattDescriptor.class));
        verify(timeout).setTimeoutAction(Mockito.any(Action0.class), Mockito.any(Scheduler.class));
        verify(timeout).schedule();

        final BluetoothGattCharacteristic characteristic = nativeService.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Testing.NOTIFY_DESCRIPTOR);
        verify(descriptor).setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);

        gattShadow.getGattCallback().onDescriptorWrite(gatt, descriptor, BluetoothGatt.GATT_WRITE_NOT_PERMITTED);
        assertThat(result.getValues().size(), is(equalTo(0)));
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(BluetoothGattError.class)));
        verify(timeout).unschedule();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void disableNotificationDisconnect() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final BluetoothGattService nativeService = Testing.createMockGattService();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();
        final Observable<UUID> enable = peripheral.disableNotification(new NativePeripheralService(nativeService),
                                                                       Testing.WRITE_CHARACTERISTIC,
                                                                       Testing.NOTIFY_DESCRIPTOR,
                                                                       timeout);

        final Testing.Result<UUID> result = new Testing.Result<>();
        enable.subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt gattShadow = BuruberiShadows.shadowOf(gatt);
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.SET_CHAR_NOTIFICATION,
                              Matchers.any(BluetoothGattCharacteristic.class),
                              Matchers.equalTo(false));
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.WRITE_DESCRIPTOR,
                              Matchers.any(BluetoothGattDescriptor.class));
        verify(timeout).setTimeoutAction(Mockito.any(Action0.class), Mockito.any(Scheduler.class));
        verify(timeout).schedule();

        final BluetoothGattCharacteristic characteristic = nativeService.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Testing.NOTIFY_DESCRIPTOR);
        verify(descriptor).setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);

        gattShadow.getGattCallback().onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothGatt.STATE_DISCONNECTED);
        assertThat(result.getValues().size(), is(equalTo(0)));
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(BluetoothConnectionLostError.class)));
        verify(timeout).unschedule();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void writeCommandSuccess() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final BluetoothGattService nativeService = Testing.createMockGattService();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();
        final Observable<Void> write = peripheral.writeCommand(new NativePeripheralService(nativeService),
                                                               Testing.WRITE_CHARACTERISTIC,
                                                               GattPeripheral.WriteType.DEFAULT,
                                                               WRITE_PAYLOAD,
                                                               timeout);

        final Testing.Result<Void> result = new Testing.Result<>();
        write.subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt gattShadow = BuruberiShadows.shadowOf(gatt);
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.WRITE_CHAR,
                              Matchers.any(BluetoothGattCharacteristic.class));
        verify(nativeService).getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        verify(timeout).setTimeoutAction(Mockito.any(Action0.class), Mockito.any(Scheduler.class));
        verify(timeout).schedule();


        final BluetoothGattCharacteristic characteristic = nativeService.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        verify(characteristic).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        verify(characteristic).setValue(WRITE_PAYLOAD);

        gattShadow.getGattCallback().onCharacteristicWrite(gatt, characteristic, BluetoothGatt.GATT_SUCCESS);
        assertThat(result.getValues().size(), is(equalTo(1)));
        assertThat(result.isCompleted(), is(true));
        verify(timeout).unschedule();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void writeCommandFailure() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final BluetoothGattService nativeService = Testing.createMockGattService();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();
        final Observable<Void> write = peripheral.writeCommand(new NativePeripheralService(nativeService),
                                                               Testing.WRITE_CHARACTERISTIC,
                                                               GattPeripheral.WriteType.DEFAULT,
                                                               WRITE_PAYLOAD,
                                                               timeout);

        final Testing.Result<Void> result = new Testing.Result<>();
        write.subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt gattShadow = BuruberiShadows.shadowOf(gatt);
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.WRITE_CHAR,
                              Matchers.any(BluetoothGattCharacteristic.class));
        verify(nativeService).getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        verify(timeout).setTimeoutAction(Mockito.any(Action0.class), Mockito.any(Scheduler.class));
        verify(timeout).schedule();


        final BluetoothGattCharacteristic characteristic = nativeService.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        verify(characteristic).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        verify(characteristic).setValue(WRITE_PAYLOAD);

        gattShadow.getGattCallback().onCharacteristicWrite(gatt, characteristic, BluetoothGatt.GATT_WRITE_NOT_PERMITTED);
        assertThat(result.getValues().size(), is(equalTo(0)));
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(BluetoothGattError.class)));
        verify(timeout).unschedule();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void writeCommandDisconnect() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final BluetoothGattService nativeService = Testing.createMockGattService();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();
        final Observable<Void> write = peripheral.writeCommand(new NativePeripheralService(nativeService),
                                                               Testing.WRITE_CHARACTERISTIC,
                                                               GattPeripheral.WriteType.DEFAULT,
                                                               WRITE_PAYLOAD,
                                                               timeout);

        final Testing.Result<Void> result = new Testing.Result<>();
        write.subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt gattShadow = BuruberiShadows.shadowOf(gatt);
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.WRITE_CHAR,
                              Matchers.any(BluetoothGattCharacteristic.class));
        verify(nativeService).getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        verify(timeout).setTimeoutAction(Mockito.any(Action0.class), Mockito.any(Scheduler.class));
        verify(timeout).schedule();


        final BluetoothGattCharacteristic characteristic = nativeService.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        verify(characteristic).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        verify(characteristic).setValue(WRITE_PAYLOAD);

        gattShadow.getGattCallback().onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothGatt.STATE_DISCONNECTED);
        assertThat(result.getValues().size(), is(equalTo(0)));
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(BluetoothConnectionLostError.class)));
        verify(timeout).unschedule();
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
