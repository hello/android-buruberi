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

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import is.hello.buruberi.bluetooth.errors.UserDisabledBuruberiException;
import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;

@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class LegacyLePeripheralScanner implements LePeripheralScanner, BluetoothAdapter.LeScanCallback {
    private final @NonNull NativeBluetoothStack stack;
    private final @NonNull LoggerFacade logger;
    private final @NonNull PeripheralCriteria peripheralCriteria;
    @VisibleForTesting final @NonNull Map<String, ScannedPeripheral> results = new HashMap<>();
    private final boolean hasAddresses;

    private @Nullable Subscriber<? super List<GattPeripheral>> subscriber;
    private @Nullable Subscription timeout;
    private boolean scanning = false;

    LegacyLePeripheralScanner(@NonNull NativeBluetoothStack stack,
                              @NonNull PeripheralCriteria peripheralCriteria) {
        this.stack = stack;
        this.logger = stack.getLogger();
        this.peripheralCriteria = peripheralCriteria;
        this.hasAddresses = !peripheralCriteria.peripheralAddresses.isEmpty();
    }


    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
    public void call(Subscriber<? super List<GattPeripheral>> subscriber) {
        logger.info(BluetoothStack.LOG_TAG, "Beginning Scan (legacy impl)");

        this.subscriber = subscriber;

        this.scanning = stack.getAdapter().startLeScan(this);
        if (scanning) {
            this.timeout = stack.getScheduler()
                                .createWorker()
                                .schedule(new Action0() {
                                    @Override
                                    public void call() {
                                        onConcludeScan();
                                    }
                                }, peripheralCriteria.duration, TimeUnit.MILLISECONDS);
        } else {
            subscriber.onError(new UserDisabledBuruberiException());
        }
    }

    @Override
    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    })
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanResponse) {
        String address = device.getAddress();
        ScannedPeripheral existingResult = results.get(address);
        if (existingResult != null) {
            existingResult.rssi = rssi;
            return;
        }

        AdvertisingData advertisingData = AdvertisingData.parse(scanResponse);
        logger.info(BluetoothStack.LOG_TAG, "Found device " + device.getName() + " - " + address + " " + advertisingData);

        if (!peripheralCriteria.matches(advertisingData)) {
            return;
        }

        if (hasAddresses && !peripheralCriteria.peripheralAddresses.contains(address)) {
            return;
        }

        results.put(address, new ScannedPeripheral(device, advertisingData, rssi));

        if (results.size() >= peripheralCriteria.limit) {
            logger.info(BluetoothStack.LOG_TAG, "Discovery limit reached, concluding scan");
            onConcludeScan();
        }
    }

    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    })
    public void onConcludeScan() {
        if (!scanning) {
            return;
        }

        this.scanning = false;

        // Low energy scanning on Android <=4.4.4 is broken when
        // a large number of unique peripherals have been scanned by
        // the device. This manifests on the client as a NPE within the
        // implementation of BluetoothAdapter#stopScan(LeScanCallback).
        // See <https://code.google.com/p/android/issues/detail?id=67272>
        try {
            stack.getAdapter().stopLeScan(this);
        } catch (Exception e) {
            logger.warn(BluetoothStack.LOG_TAG, "Could not stop le scan due to internal stack error.", e);
        }

        if (timeout != null) {
            timeout.unsubscribe();
            this.timeout = null;
        }

        List<GattPeripheral> peripherals = new ArrayList<>();

        if (stack.getAdapter().getState() == BluetoothAdapter.STATE_ON) {
            for (ScannedPeripheral scannedPeripheral : results.values()) {
                NativeGattPeripheral peripheral = scannedPeripheral.createPeripheral(stack);
                peripherals.add(peripheral);
            }
        }

        logger.info(BluetoothStack.LOG_TAG, "Completed Scan " + peripherals);

        if (subscriber != null) {
            subscriber.onNext(peripherals);
            subscriber.onCompleted();
        } else {
            logger.warn(BluetoothStack.LOG_TAG, "LegacyLePeripheralScanner invoked without a subscriber, ignoring.");
        }
    }
}
