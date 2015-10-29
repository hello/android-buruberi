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

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;

import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;

class ScannedPeripheral {
    final BluetoothDevice device;
    final AdvertisingData advertisingData;
    int rssi;

    ScannedPeripheral(@NonNull BluetoothDevice device,
                      int rssi,
                      @NonNull AdvertisingData advertisingData) {
        this.device = device;
        this.rssi = rssi;
        this.advertisingData = advertisingData;
    }


    NativeGattPeripheral createPeripheral(@NonNull NativeBluetoothStack stack) {
        return new NativeGattPeripheral(stack, device, rssi, advertisingData);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScannedPeripheral that = (ScannedPeripheral) o;

        return (rssi == that.rssi &&
                advertisingData.equals(that.advertisingData) &&
                device.equals(that.device));

    }

    @Override
    public int hashCode() {
        int result = device.hashCode();
        result = 31 * result + advertisingData.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ScanResult{" +
                "device=" + device +
                ", advertisingData=" + advertisingData +
                ", rssi=" + rssi +
                '}';
    }
}
