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
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Build;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.UUID;

import is.hello.buruberi.bluetooth.errors.GattException;
import is.hello.buruberi.bluetooth.errors.LostConnectionException;
import is.hello.buruberi.bluetooth.stacks.GattCharacteristic;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;
import is.hello.buruberi.bluetooth.stacks.util.ErrorListener;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.testing.BuruberiShadows;
import is.hello.buruberi.testing.BuruberiTestCase;
import is.hello.buruberi.testing.ShadowBluetoothGatt;
import is.hello.buruberi.testing.Testing;
import is.hello.buruberi.util.Defaults;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class NativeGattCharacteristicTests extends BuruberiTestCase {
    private static final byte[] WRITE_PAYLOAD = {0xC, 0xA, 0xF, 0xE};

    private final ErrorListener errorListener = Defaults.createEmptyErrorListener();
    private final LoggerFacade loggerFacade = Defaults.createLogcatFacade();
    private NativeBluetoothStack stack;

    //region Lifecycle

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

    //endregion


    //region Notifications

    @SuppressWarnings("ConstantConditions")
    @Test
    public void readSuccess() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final BluetoothGattService nativeService = Testing.createMockGattService();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();

        final NativeGattService service = new NativeGattService(nativeService, peripheral);
        final NativeGattCharacteristic characteristic =
                service.getCharacteristic(Testing.WRITE_CHARACTERISTIC);

        final Observable<byte[]> read = characteristic.read(timeout);
        final Testing.Result<byte[]> result = new Testing.Result<>();
        read.subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt gattShadow = BuruberiShadows.shadowOf(gatt);
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.READ_CHAR,
                              Matchers.any(BluetoothGattCharacteristic.class));
        verify(timeout).setTimeoutAction(Mockito.any(Action0.class), Mockito.any(Scheduler.class));
        verify(timeout).schedule();

        characteristic.wrappedCharacteristic.setValue(new byte[]{0x0, 0x1});
        gattShadow.getGattCallback().onCharacteristicRead(gatt,
                                                          characteristic.wrappedCharacteristic,
                                                          BluetoothGatt.GATT_SUCCESS);
        assertThat(result.getValues().size(), is(equalTo(1)));
        assertThat(result.isCompleted(), is(true));
        verify(timeout).unschedule();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void readFailure() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final BluetoothGattService nativeService = Testing.createMockGattService();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();

        final NativeGattService service = new NativeGattService(nativeService, peripheral);
        final NativeGattCharacteristic characteristic =
                service.getCharacteristic(Testing.WRITE_CHARACTERISTIC);

        final Observable<byte[]> read = characteristic.read(timeout);
        final Testing.Result<byte[]> result = new Testing.Result<>();
        read.subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt gattShadow = BuruberiShadows.shadowOf(gatt);
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.READ_CHAR,
                              Matchers.any(BluetoothGattCharacteristic.class));
        verify(timeout).setTimeoutAction(Mockito.any(Action0.class), Mockito.any(Scheduler.class));
        verify(timeout).schedule();

        gattShadow.getGattCallback().onCharacteristicRead(gatt,
                                                          characteristic.wrappedCharacteristic,
                                                          BluetoothGatt.GATT_READ_NOT_PERMITTED);
        assertThat(result.getValues().size(), is(equalTo(0)));
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(GattException.class)));
        verify(timeout).unschedule();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void enableNotificationSuccess() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final BluetoothGattService nativeService = Testing.createMockGattService();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();
        final NativeGattService service = new NativeGattService(nativeService, peripheral);
        final GattCharacteristic characteristic = service.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final Observable<UUID> enable = characteristic.enableNotification(Testing.NOTIFY_DESCRIPTOR,
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

        final BluetoothGattCharacteristic nativeCharacteristic =
                nativeService.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final BluetoothGattDescriptor descriptor = nativeCharacteristic.getDescriptor(Testing.NOTIFY_DESCRIPTOR);
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
        final NativeGattService service = new NativeGattService(nativeService, peripheral);
        final GattCharacteristic characteristic = service.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final Observable<UUID> enable = characteristic.enableNotification(Testing.NOTIFY_DESCRIPTOR,
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

        final BluetoothGattCharacteristic nativeCharacteristic =
                nativeService.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final BluetoothGattDescriptor descriptor = nativeCharacteristic.getDescriptor(Testing.NOTIFY_DESCRIPTOR);
        verify(descriptor).setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        gattShadow.getGattCallback().onDescriptorWrite(gatt, descriptor, BluetoothGatt.GATT_WRITE_NOT_PERMITTED);
        assertThat(result.getValues().size(), is(equalTo(0)));
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(GattException.class)));
        verify(timeout).unschedule();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void enableNotificationDisconnect() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final BluetoothGattService nativeService = Testing.createMockGattService();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();
        final NativeGattService service = new NativeGattService(nativeService, peripheral);
        final GattCharacteristic characteristic = service.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final Observable<UUID> enable = characteristic.enableNotification(Testing.NOTIFY_DESCRIPTOR,
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

        final BluetoothGattCharacteristic nativeCharacteristic =
                nativeService.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final BluetoothGattDescriptor descriptor = nativeCharacteristic.getDescriptor(Testing.NOTIFY_DESCRIPTOR);
        verify(descriptor).setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        gattShadow.getGattCallback().onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothGatt.STATE_DISCONNECTED);
        assertThat(result.getValues().size(), is(equalTo(0)));
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(LostConnectionException.class)));
        verify(timeout).unschedule();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void disableNotificationSuccess() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final BluetoothGattService nativeService = Testing.createMockGattService();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();
        final NativeGattService service = new NativeGattService(nativeService, peripheral);
        final GattCharacteristic characteristic = service.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final Observable<UUID> disable = characteristic.disableNotification(Testing.NOTIFY_DESCRIPTOR,
                                                                            timeout);

        final Testing.Result<UUID> result = new Testing.Result<>();
        disable.subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt gattShadow = BuruberiShadows.shadowOf(gatt);
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.SET_CHAR_NOTIFICATION,
                              Matchers.any(BluetoothGattCharacteristic.class),
                              Matchers.equalTo(false));
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.WRITE_DESCRIPTOR,
                              Matchers.any(BluetoothGattDescriptor.class));
        verify(timeout).setTimeoutAction(Mockito.any(Action0.class), Mockito.any(Scheduler.class));
        verify(timeout).schedule();

        final BluetoothGattCharacteristic nativeCharacteristic =
                nativeService.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final BluetoothGattDescriptor descriptor = nativeCharacteristic.getDescriptor(Testing.NOTIFY_DESCRIPTOR);
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
        final NativeGattService service = new NativeGattService(nativeService, peripheral);
        final GattCharacteristic characteristic = service.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final Observable<UUID> disable = characteristic.disableNotification(Testing.NOTIFY_DESCRIPTOR,
                                                                            timeout);

        final Testing.Result<UUID> result = new Testing.Result<>();
        disable.subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt gattShadow = BuruberiShadows.shadowOf(gatt);
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.SET_CHAR_NOTIFICATION,
                              Matchers.any(BluetoothGattCharacteristic.class),
                              Matchers.equalTo(false));
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.WRITE_DESCRIPTOR,
                              Matchers.any(BluetoothGattDescriptor.class));
        verify(timeout).setTimeoutAction(Mockito.any(Action0.class), Mockito.any(Scheduler.class));
        verify(timeout).schedule();

        final BluetoothGattCharacteristic nativeCharacteristic =
                nativeService.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final BluetoothGattDescriptor descriptor = nativeCharacteristic.getDescriptor(Testing.NOTIFY_DESCRIPTOR);
        verify(descriptor).setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);

        gattShadow.getGattCallback().onDescriptorWrite(gatt, descriptor, BluetoothGatt.GATT_WRITE_NOT_PERMITTED);
        assertThat(result.getValues().size(), is(equalTo(0)));
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(GattException.class)));
        verify(timeout).unschedule();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void disableNotificationDisconnect() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final BluetoothGattService nativeService = Testing.createMockGattService();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();
        final NativeGattService service = new NativeGattService(nativeService, peripheral);
        final GattCharacteristic characteristic = service.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final Observable<UUID> disable = characteristic.disableNotification(Testing.NOTIFY_DESCRIPTOR,
                                                                            timeout);

        final Testing.Result<UUID> result = new Testing.Result<>();
        disable.subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt gattShadow = BuruberiShadows.shadowOf(gatt);
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.SET_CHAR_NOTIFICATION,
                              Matchers.any(BluetoothGattCharacteristic.class),
                              Matchers.equalTo(false));
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.WRITE_DESCRIPTOR,
                              Matchers.any(BluetoothGattDescriptor.class));
        verify(timeout).setTimeoutAction(Mockito.any(Action0.class), Mockito.any(Scheduler.class));
        verify(timeout).schedule();

        final BluetoothGattCharacteristic nativeCharacteristic =
                nativeService.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final BluetoothGattDescriptor descriptor = nativeCharacteristic.getDescriptor(Testing.NOTIFY_DESCRIPTOR);
        verify(descriptor).setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);

        gattShadow.getGattCallback().onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothGatt.STATE_DISCONNECTED);
        assertThat(result.getValues().size(), is(equalTo(0)));
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(LostConnectionException.class)));
        verify(timeout).unschedule();
    }

    //endregion


    //region Writes

    @SuppressWarnings("ConstantConditions")
    @Test
    public void writeSuccess() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final BluetoothGattService nativeService = Testing.createMockGattService();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();
        final NativeGattService service = new NativeGattService(nativeService, peripheral);
        final GattCharacteristic characteristic = service.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final Observable<Void> write = characteristic.write(GattPeripheral.WriteType.DEFAULT,
                                                            WRITE_PAYLOAD,
                                                            timeout);

        final Testing.Result<Void> result = new Testing.Result<>();
        write.subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt gattShadow = BuruberiShadows.shadowOf(gatt);
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.WRITE_CHAR,
                              Matchers.any(BluetoothGattCharacteristic.class));
        verify(timeout).setTimeoutAction(Mockito.any(Action0.class), Mockito.any(Scheduler.class));
        verify(timeout).schedule();


        final BluetoothGattCharacteristic nativeCharacteristic =
                nativeService.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        verify(nativeCharacteristic).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        verify(nativeCharacteristic).setValue(WRITE_PAYLOAD);

        gattShadow.getGattCallback().onCharacteristicWrite(gatt, nativeCharacteristic, BluetoothGatt.GATT_SUCCESS);
        assertThat(result.getValues().size(), is(equalTo(1)));
        assertThat(result.isCompleted(), is(true));
        verify(timeout).unschedule();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void writeFailure() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final BluetoothGattService nativeService = Testing.createMockGattService();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();
        final NativeGattService service = new NativeGattService(nativeService, peripheral);
        final GattCharacteristic characteristic = service.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final Observable<Void> write = characteristic.write(GattPeripheral.WriteType.DEFAULT,
                                                            WRITE_PAYLOAD,
                                                            timeout);

        final Testing.Result<Void> result = new Testing.Result<>();
        write.subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt gattShadow = BuruberiShadows.shadowOf(gatt);
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.WRITE_CHAR,
                              Matchers.any(BluetoothGattCharacteristic.class));
        verify(timeout).setTimeoutAction(Mockito.any(Action0.class), Mockito.any(Scheduler.class));
        verify(timeout).schedule();


        final BluetoothGattCharacteristic nativeCharacteristic =
                nativeService.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        verify(nativeCharacteristic).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        verify(nativeCharacteristic).setValue(WRITE_PAYLOAD);

        gattShadow.getGattCallback().onCharacteristicWrite(gatt, nativeCharacteristic, BluetoothGatt.GATT_WRITE_NOT_PERMITTED);
        assertThat(result.getValues().size(), is(equalTo(0)));
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(GattException.class)));
        verify(timeout).unschedule();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void writeDisconnect() {
        final NativeGattPeripheral peripheral = createConnectedPeripheral();
        final BluetoothGattService nativeService = Testing.createMockGattService();
        final OperationTimeout timeout = Testing.createMockOperationTimeout();
        final NativeGattService service = new NativeGattService(nativeService, peripheral);
        final GattCharacteristic characteristic = service.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final Observable<Void> write = characteristic.write(GattPeripheral.WriteType.DEFAULT,
                                                            WRITE_PAYLOAD,
                                                            timeout);

        final Testing.Result<Void> result = new Testing.Result<>();
        write.subscribe(result);

        final BluetoothGatt gatt = peripheral.gatt;
        final ShadowBluetoothGatt gattShadow = BuruberiShadows.shadowOf(gatt);
        gattShadow.verifyCall(ShadowBluetoothGatt.Call.WRITE_CHAR,
                              Matchers.any(BluetoothGattCharacteristic.class));
        verify(timeout).setTimeoutAction(Mockito.any(Action0.class), Mockito.any(Scheduler.class));
        verify(timeout).schedule();


        final BluetoothGattCharacteristic nativeCharacteristic =
                nativeService.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        verify(nativeCharacteristic).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        verify(nativeCharacteristic).setValue(WRITE_PAYLOAD);

        gattShadow.getGattCallback().onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothGatt.STATE_DISCONNECTED);
        assertThat(result.getValues().size(), is(equalTo(0)));
        assertThat(result.isCompleted(), is(false));
        assertThat(result.getError(), is(instanceOf(LostConnectionException.class)));
        verify(timeout).unschedule();
    }

    //endregion
}
