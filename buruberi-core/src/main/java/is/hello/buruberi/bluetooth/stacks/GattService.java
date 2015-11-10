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
package is.hello.buruberi.bluetooth.stacks;

import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;

import java.util.List;
import java.util.UUID;

public interface GattService {
    int TYPE_PRIMARY = BluetoothGattService.SERVICE_TYPE_PRIMARY;
    int TYPE_SECONDARY = BluetoothGattService.SERVICE_TYPE_SECONDARY;

    UUID getUuid();
    int getType();
    List<UUID> getCharacteristics();
    GattCharacteristic getCharacteristic(@NonNull UUID identifier);
}
