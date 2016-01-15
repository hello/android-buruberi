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

import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.GattCharacteristic;
import is.hello.buruberi.bluetooth.stacks.GattService;
import is.hello.buruberi.testing.BuruberiTestCase;
import is.hello.buruberi.testing.Testing;
import is.hello.buruberi.util.Defaults;

import static android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY;
import static android.bluetooth.BluetoothGattService.SERVICE_TYPE_SECONDARY;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class NativeGattServiceTests extends BuruberiTestCase {
    private final NativeGattPeripheral peripheral = mock(NativeGattPeripheral.class);

    public NativeGattServiceTests() {
        final BluetoothStack stackMock = mock(BluetoothStack.class);
        doReturn(Defaults.createLogcatFacade())
                .when(stackMock)
                .getLogger();

        doReturn(stackMock)
                .when(peripheral)
                .getStack();
    }

    @Test
    public void wrapGattServices() {
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();

        final List<BluetoothGattService> services =
                Arrays.asList(new BluetoothGattService(first, SERVICE_TYPE_PRIMARY),
                              new BluetoothGattService(second, SERVICE_TYPE_SECONDARY));
        final Map<UUID, ? extends GattService> wrappedServices =
                NativeGattService.wrap(services, peripheral);
        assertThat(wrappedServices, is(notNullValue()));
        assertThat(wrappedServices.size(), is(equalTo(2)));

        assertThat(wrappedServices.keySet(), hasItem(first));
        assertThat(wrappedServices.keySet(), hasItem(second));
    }

    @Test
    public void getUuid() {
        final UUID uuid = UUID.randomUUID();
        final BluetoothGattService service = new BluetoothGattService(uuid, SERVICE_TYPE_PRIMARY);
        final NativeGattService peripheralService = new NativeGattService(service, peripheral);
        assertThat(peripheralService.getUuid(), is(equalTo(uuid)));
    }

    @Test
    public void getType() {
        final UUID uuid = UUID.randomUUID();
        final BluetoothGattService service = new BluetoothGattService(uuid, SERVICE_TYPE_PRIMARY);
        final NativeGattService peripheralService = new NativeGattService(service, peripheral);
        assertThat(peripheralService.getType(), is(equalTo(GattService.TYPE_PRIMARY)));
    }

    @Test
    public void identity() {
        final UUID uuid = UUID.randomUUID();
        final BluetoothGattService service = new BluetoothGattService(uuid, SERVICE_TYPE_PRIMARY);
        final NativeGattService peripheralService1 = new NativeGattService(service, peripheral);
        final NativeGattService peripheralService2 = new NativeGattService(service, peripheral);

        assertThat(peripheralService1, is(equalTo(peripheralService2)));
        assertThat(peripheralService1.hashCode(), is(equalTo(peripheralService2.hashCode())));
    }

    @Test
    public void dispatchNotify() {
        final BluetoothGattService nativeService = Testing.createMockGattService();
        final NativeGattService service = new NativeGattService(nativeService, peripheral);

        final NativeGattCharacteristic characteristic =
                service.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final AtomicBoolean notifyCalled = new AtomicBoolean(false);
        characteristic.setPacketListener(new GattCharacteristic.PacketListener() {
            @Override
            public void onCharacteristicNotify(@NonNull UUID characteristic,
                                               @NonNull byte[] payload) {
                notifyCalled.set(true);

                assertThat(characteristic, is(equalTo(Testing.WRITE_CHARACTERISTIC)));
                assertThat(payload, is(equalTo(new byte[] {0x0, 0x1})));
            }

            @Override
            public void onPeripheralDisconnected() {
            }
        });

        service.dispatchNotify(characteristic.getUuid(), new byte[]{0x0, 0x1});

        assertThat(notifyCalled.get(), is(true));
        assertThat(characteristic.packetListener, is(notNullValue()));
    }

    @Test
    public void dispatchDisconnect() {
        final BluetoothGattService nativeService = Testing.createMockGattService();
        final NativeGattService service = new NativeGattService(nativeService, peripheral);

        final NativeGattCharacteristic characteristic =
                service.getCharacteristic(Testing.WRITE_CHARACTERISTIC);
        final AtomicBoolean disconnectCalled = new AtomicBoolean(false);
        characteristic.setPacketListener(new GattCharacteristic.PacketListener() {
            @Override
            public void onCharacteristicNotify(@NonNull UUID characteristic,
                                               @NonNull byte[] payload) {
            }

            @Override
            public void onPeripheralDisconnected() {
                disconnectCalled.set(true);
            }
        });

        service.dispatchDisconnect();

        assertThat(disconnectCalled.get(), is(true));
        assertThat(characteristic.packetListener, is(nullValue()));
    }
}
