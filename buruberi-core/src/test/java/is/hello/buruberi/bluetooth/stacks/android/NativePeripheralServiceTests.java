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

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import is.hello.buruberi.bluetooth.stacks.PeripheralService;
import is.hello.buruberi.testing.BuruberiTestCase;

import static android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY;
import static android.bluetooth.BluetoothGattService.SERVICE_TYPE_SECONDARY;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class NativePeripheralServiceTests extends BuruberiTestCase {
    private final NativeGattPeripheral peripheral = mock(NativeGattPeripheral.class);

    @Test
    public void wrapGattServices() {
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();

        final List<BluetoothGattService> services =
                Arrays.asList(new BluetoothGattService(first, SERVICE_TYPE_PRIMARY),
                              new BluetoothGattService(second, SERVICE_TYPE_SECONDARY));
        final Map<UUID, PeripheralService> wrappedServices =
                NativePeripheralService.wrap(services, peripheral);
        assertThat(wrappedServices, is(notNullValue()));
        assertThat(wrappedServices.size(), is(equalTo(2)));

        assertThat(wrappedServices.keySet(), hasItem(first));
        assertThat(wrappedServices.keySet(), hasItem(second));
    }

    @Test
    public void getUuid() {
        final UUID uuid = UUID.randomUUID();
        final BluetoothGattService service = new BluetoothGattService(uuid, SERVICE_TYPE_PRIMARY);
        final NativePeripheralService peripheralService = new NativePeripheralService(service, peripheral);
        assertThat(peripheralService.getUuid(), is(equalTo(uuid)));
    }

    @Test
    public void getType() {
        final UUID uuid = UUID.randomUUID();
        final BluetoothGattService service = new BluetoothGattService(uuid, SERVICE_TYPE_PRIMARY);
        final NativePeripheralService peripheralService = new NativePeripheralService(service, peripheral);
        assertThat(peripheralService.getType(), is(equalTo(PeripheralService.SERVICE_TYPE_PRIMARY)));
    }

    @Test
    public void identity() {
        final UUID uuid = UUID.randomUUID();
        final BluetoothGattService service = new BluetoothGattService(uuid, SERVICE_TYPE_PRIMARY);
        final NativePeripheralService peripheralService1 = new NativePeripheralService(service, peripheral);
        final NativePeripheralService peripheralService2 = new NativePeripheralService(service, peripheral);

        assertThat(peripheralService1, is(equalTo(peripheralService2)));
        assertThat(peripheralService1.hashCode(), is(equalTo(peripheralService2.hashCode())));
    }
}
