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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;

class HighPowerPeripheralScanner extends BroadcastReceiver implements Observable.OnSubscribe<List<BluetoothDevice>> {
    /**
     * Roughly how long the documentation says a scan should take.
     */
    private static final int SCAN_DURATION_S = 15;

    private final Context context;
    private final BluetoothAdapter adapter;
    private final Scheduler.Worker worker;
    private final LoggerFacade logger;

    private @Nullable final List<BluetoothDevice> devices;
    private @Nullable Subscriber<? super List<BluetoothDevice>> subscriber;
    private @Nullable Subscription timeout;

    private boolean discovering = false;
    private boolean registered = false;

    //region Lifecycle

    HighPowerPeripheralScanner(@NonNull NativeBluetoothStack stack, boolean saveResults) {
        this.context = stack.applicationContext;
        this.adapter = stack.getAdapter();
        this.worker = stack.scheduler.createWorker();
        this.logger = stack.getLogger();

        if (saveResults) {
            this.devices = new ArrayList<>();
        } else {
            this.devices = null;
        }
    }

    @Override
    public void call(Subscriber<? super List<BluetoothDevice>> subscriber) {
        this.subscriber = subscriber;

        startDiscovery();

        // This is only necessary on some (Samsung?) devices.
        this.timeout = worker.schedule(new Action0() {
            @Override
            public void call() {
                if (timeout != null && !timeout.isUnsubscribed()) {
                    HighPowerPeripheralScanner.this.stopDiscovery();
                }
            }
        }, SCAN_DURATION_S, TimeUnit.SECONDS);
    }

    //endregion


    //region Callbacks

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case BluetoothDevice.ACTION_FOUND: {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                final short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0);
                final ParcelUuid uuid = intent.getParcelableExtra(BluetoothDevice.EXTRA_UUID);
                final String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        HighPowerPeripheralScanner.this.onDeviceFound(device, rssi, uuid, name);
                    }
                });

                break;
            }

            case BluetoothAdapter.ACTION_DISCOVERY_STARTED: {
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        onDiscoveryStarted();
                    }
                });
                break;
            }

            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED: {
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        onDiscoveryFinished();
                    }
                });
                break;
            }

            default: {
                throw new IllegalArgumentException("Unknown intent " + intent);
            }
        }
    }

    private void onDeviceFound(@NonNull BluetoothDevice device,
                               short rssi,
                               @Nullable ParcelUuid uuid,
                               @Nullable String name) {
        logger.info(BluetoothStack.LOG_TAG, "high power scan found {" + device + " rssi: " + rssi + ", uuid: " + uuid + ", name: " + name + "}");
        if (devices != null) {
            devices.add(device);
        }
    }

    private void onDiscoveryStarted() {
        logger.info(BluetoothStack.LOG_TAG, "high power scan started");
        this.discovering = true;
        if (devices != null) {
            devices.clear();
        }
    }

    private void onDiscoveryFinished() {
        logger.info(BluetoothStack.LOG_TAG, "high power scan finished");

        this.discovering = false;
        stopDiscovery();
    }

    //endregion

    public void startDiscovery() {
        logger.info(BluetoothStack.LOG_TAG, "start high power scan");

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(this, intentFilter);
        this.registered = true;

        if (!adapter.isDiscovering() && !adapter.startDiscovery()) {
            logger.error(BluetoothStack.LOG_TAG, "Could not start discovery", null);

            if (subscriber != null && !subscriber.isUnsubscribed()) {
                subscriber.onError(new IllegalStateException("Could not stop discovery"));
                this.subscriber = null;
            }
        }
    }

    public void stopDiscovery() {
        if (!registered) {
            return;
        }

        logger.info(BluetoothStack.LOG_TAG, "stop high power scan");

        if (timeout != null) {
            timeout.unsubscribe();
            this.timeout = null;
        }

        context.unregisterReceiver(this);
        this.registered = false;

        if (discovering && !adapter.cancelDiscovery()) {
            logger.error(BluetoothStack.LOG_TAG, "Could not stop discovery", null);

            if (subscriber != null && !subscriber.isUnsubscribed()) {
                subscriber.onError(new IllegalStateException("Could not stop discovery"));
                this.subscriber = null;
            }
        }

        if (subscriber != null && !subscriber.isUnsubscribed()) {
            if (devices != null) {
                subscriber.onNext(devices);
            } else {
                subscriber.onNext(Collections.<BluetoothDevice>emptyList());
            }
            subscriber.onCompleted();

            this.subscriber = null;
        }
    }
}
