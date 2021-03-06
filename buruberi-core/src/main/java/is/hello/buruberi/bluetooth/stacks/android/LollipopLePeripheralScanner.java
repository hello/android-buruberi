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
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
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

import is.hello.buruberi.bluetooth.errors.LowEnergyScanException;
import is.hello.buruberi.bluetooth.errors.UserDisabledBuruberiException;
import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class LollipopLePeripheralScanner extends ScanCallback implements LePeripheralScanner {
    private final @NonNull NativeBluetoothStack stack;
    private final @NonNull LoggerFacade logger;
    private final @NonNull PeripheralCriteria peripheralCriteria;
    private final @NonNull BluetoothAdapter adapter;
    private final @Nullable BluetoothLeScanner scanner;
    @VisibleForTesting final @NonNull Map<String, ScannedPeripheral> results = new HashMap<>();
    private final boolean hasAddresses;

    private @Nullable Subscriber<? super List<GattPeripheral>> subscriber;
    private @Nullable Subscription timeout;
    private boolean scanning = false;

    LollipopLePeripheralScanner(@NonNull NativeBluetoothStack stack,
                                @NonNull PeripheralCriteria peripheralCriteria) {
        this.stack = stack;
        this.logger = stack.getLogger();
        this.peripheralCriteria = peripheralCriteria;
        this.hasAddresses = !peripheralCriteria.peripheralAddresses.isEmpty();
        this.adapter = stack.getAdapter();
        this.scanner = adapter.getBluetoothLeScanner();
    }


    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
    public void call(Subscriber<? super List<GattPeripheral>> subscriber) {
        logger.info(BluetoothStack.LOG_TAG, "Beginning Scan (Lollipop impl)");

        this.subscriber = subscriber;

        if (scanner != null) {
            this.scanning = true;

            // Guards against the checking the power state of the BluetoothAdapter,
            // then the power state changing between that check and the following
            // call to startScan.
            try {
                final ScanSettings.Builder builder = new ScanSettings.Builder();
                builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
                scanner.startScan(null, builder.build(), this);
            } catch (IllegalStateException e) {
                this.scanning = false;
                subscriber.onError(new UserDisabledBuruberiException(e));

                return;
            }

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
    public void onBatchScanResults(List<ScanResult> results) {
        logger.info(BluetoothStack.LOG_TAG, "Forwarding batch results");

        for (ScanResult result : results) {
            onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result);
        }
    }

    @Override
    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    })
    public void onScanResult(int callbackType, ScanResult result) {
        if (result.getScanRecord() == null) {
            return;
        }

        BluetoothDevice device = result.getDevice();
        String address = device.getAddress();
        ScannedPeripheral existingResult = results.get(address);
        if (existingResult != null) {
            existingResult.rssi = result.getRssi();
            return;
        }

        byte[] scanResponse = result.getScanRecord().getBytes();
        AdvertisingData advertisingData = AdvertisingData.parse(scanResponse);
        logger.info(BluetoothStack.LOG_TAG, "Found device " + device.getName() + " - " + address + " " + advertisingData);

        if (!peripheralCriteria.matches(advertisingData)) {
            return;
        }

        if (hasAddresses && !peripheralCriteria.peripheralAddresses.contains(address)) {
            return;
        }

        results.put(address, new ScannedPeripheral(device, advertisingData, result.getRssi()));

        if (results.size() >= peripheralCriteria.limit) {
            logger.info(BluetoothStack.LOG_TAG, "Discovery limit reached, concluding scan");
            onConcludeScan();
        }
    }

    @Override
    public void onScanFailed(int errorCode) {
        this.scanning = false;

        if (timeout != null) {
            timeout.unsubscribe();
            this.timeout = null;
        }

        LowEnergyScanException error = new LowEnergyScanException(errorCode);
        if (subscriber != null) {
            subscriber.onError(error);
        } else {
            logger.error(BluetoothStack.LOG_TAG, "LePeripheralScanner invoked without a subscriber.", error);
        }
    }

    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    })
    public void onConcludeScan() {
        if (scanner == null) {
            throw new IllegalStateException("scanner is missing");
        }

        if (!scanning) {
            return;
        }

        this.scanning = false;

        boolean bluetoothOn = (adapter.getState() == BluetoothAdapter.STATE_ON);

        // The BluetoothLeScanner#stopScan(ScanCallback) method requires
        // that its associated BluetoothAdapter be in the on state to stop
        // the scan (how does this make sense?)
        if (bluetoothOn) {
            // State could conceivably change between getState and stopScan calls.
            try {
                scanner.stopScan(this);
            } catch (IllegalStateException e) {
                logger.warn(BluetoothStack.LOG_TAG, "Adapter state changed between calls, ignoring.", e);
            }
        }

        if (timeout != null) {
            timeout.unsubscribe();
            this.timeout = null;
        }

        List<GattPeripheral> peripherals = new ArrayList<>();

        if (bluetoothOn) {
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
            logger.warn(BluetoothStack.LOG_TAG, "LePeripheralScanner invoked without a subscriber, ignoring.");
        }
    }
}
