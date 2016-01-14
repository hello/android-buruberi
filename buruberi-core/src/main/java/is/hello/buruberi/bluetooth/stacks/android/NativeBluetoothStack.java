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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.annotation.VisibleForTesting;

import java.util.List;

import is.hello.buruberi.bluetooth.errors.ChangePowerStateException;
import is.hello.buruberi.bluetooth.errors.UserDisabledBuruberiException;
import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.util.ErrorListener;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.buruberi.util.Rx;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;

public class NativeBluetoothStack implements BluetoothStack {
    /*package*/ final @NonNull Context applicationContext;
    private final @NonNull ErrorListener errorListener;
    private final @NonNull LoggerFacade logger;

    private final @NonNull Scheduler scheduler = Rx.mainThreadScheduler();
    /*package*/ final @NonNull BluetoothManager bluetoothManager;
    private final @Nullable BluetoothAdapter adapter;

    private final @NonNull ReplaySubject<Boolean> enabled = ReplaySubject.createWithSize(1);

    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public NativeBluetoothStack(@NonNull Context applicationContext,
                                @NonNull ErrorListener errorListener,
                                @NonNull LoggerFacade logger) {
        this.applicationContext = applicationContext;
        this.errorListener = errorListener;
        this.logger = logger;

        this.bluetoothManager = (BluetoothManager) applicationContext.getSystemService(Context.BLUETOOTH_SERVICE);
        this.adapter = bluetoothManager.getAdapter();
        if (adapter != null) {
            final BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    final int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                                            BluetoothAdapter.ERROR);
                    if (newState == BluetoothAdapter.STATE_ON) {
                        enabled.onNext(true);
                    } else if (newState == BluetoothAdapter.STATE_OFF ||
                            newState == BluetoothAdapter.ERROR) {
                        enabled.onNext(false);
                    }
                }
            };
            applicationContext.registerReceiver(receiver,
                                                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            enabled.onNext(adapter.isEnabled());
        } else {
            logger.warn(LOG_TAG, "Host device has no bluetooth hardware!");
            enabled.onNext(false);
        }
    }


    @NonNull BluetoothAdapter getAdapter() {
        if (adapter == null) {
            throw new NullPointerException("Host device has no bluetooth hardware!");
        }

        return adapter;
    }


    @VisibleForTesting
    LePeripheralScanner createLeScanner(@NonNull PeripheralCriteria peripheralCriteria) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new LollipopLePeripheralScanner(this, peripheralCriteria);
        } else {
            return new LegacyLePeripheralScanner(this, peripheralCriteria);
        }
    }

    @NonNull
    @Override
    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    })
    public Observable<List<GattPeripheral>> discoverPeripherals(final @NonNull PeripheralCriteria peripheralCriteria) {
        if (adapter != null && adapter.isEnabled()) {
            if (peripheralCriteria.wantsHighPowerPreScan) {
                final Observable<List<BluetoothDevice>> devices =
                        newConfiguredObservable(new HighPowerPeripheralScanner(this, false));
                return devices.flatMap(new Func1<List<BluetoothDevice>, Observable<? extends List<GattPeripheral>>>() {
                    @Override
                    public Observable<? extends List<GattPeripheral>> call(List<BluetoothDevice> ignoredDevices) {
                        logger.info(LOG_TAG, "High power pre-scan completed.");
                        return newConfiguredObservable(createLeScanner(peripheralCriteria));
                    }
                });
            } else {
                return newConfiguredObservable(createLeScanner(peripheralCriteria));
            }
        } else {
            return Observable.error(new UserDisabledBuruberiException());
        }
    }

    @NonNull
    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public <T> Observable<T> newConfiguredObservable(@NonNull Observable.OnSubscribe<T> onSubscribe) {
        return Observable.create(onSubscribe)
                         .subscribeOn(getScheduler())
                         .doOnError(errorListener);
    }

    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public Observable<Boolean> enabled() {
        return this.enabled;
    }

    @Override
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public boolean isEnabled() {
        return (adapter != null && adapter.isEnabled());
    }

    @Override
    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    })
    public Observable<Void> turnOn() {
        if (adapter == null) {
            return Observable.error(new ChangePowerStateException());
        }

        final ReplaySubject<Void> turnOnMirror = ReplaySubject.createWithSize(1);
        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final int oldState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE,
                                                        BluetoothAdapter.ERROR);
                final int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                                        BluetoothAdapter.ERROR);
                if (oldState == BluetoothAdapter.STATE_OFF &&
                        newState == BluetoothAdapter.STATE_TURNING_ON) {
                    logger.info(LOG_TAG, "Bluetooth turning on");
                } else if (oldState == BluetoothAdapter.STATE_TURNING_ON &&
                        newState == BluetoothAdapter.STATE_ON) {
                    logger.info(LOG_TAG, "Bluetooth turned on");

                    applicationContext.unregisterReceiver(this);

                    turnOnMirror.onNext(null);
                    turnOnMirror.onCompleted();
                } else {
                    logger.info(LOG_TAG, "Bluetooth failed to turn on");

                    applicationContext.unregisterReceiver(this);

                    turnOnMirror.onError(new ChangePowerStateException());
                }
            }
        };
        applicationContext.registerReceiver(receiver,
                                            new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        if (!adapter.enable()) {
            applicationContext.unregisterReceiver(receiver);
            return Observable.error(new ChangePowerStateException());
        }

        return turnOnMirror;
    }

    @Override
    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    })
    public Observable<Void> turnOff() {
        if (adapter == null) {
            return Observable.error(new ChangePowerStateException());
        }

        final ReplaySubject<Void> turnOnMirror = ReplaySubject.createWithSize(1);
        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final int oldState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE,
                                                        BluetoothAdapter.ERROR);
                final int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                                        BluetoothAdapter.ERROR);
                if (oldState == BluetoothAdapter.STATE_ON &&
                        newState == BluetoothAdapter.STATE_TURNING_OFF) {
                    logger.info(LOG_TAG, "Bluetooth turning off");
                } else if (oldState == BluetoothAdapter.STATE_TURNING_OFF &&
                        newState == BluetoothAdapter.STATE_OFF) {
                    logger.info(LOG_TAG, "Bluetooth turned off");

                    applicationContext.unregisterReceiver(this);

                    turnOnMirror.onNext(null);
                    turnOnMirror.onCompleted();
                } else {
                    logger.info(LOG_TAG, "Bluetooth failed to turn off");

                    applicationContext.unregisterReceiver(this);

                    turnOnMirror.onError(new ChangePowerStateException());
                }
            }
        };
        applicationContext.registerReceiver(receiver,
                                            new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        if (!adapter.disable()) {
            applicationContext.unregisterReceiver(receiver);
            return Observable.error(new ChangePowerStateException());
        }

        return turnOnMirror;
    }


    @Override
    @NonNull
    public LoggerFacade getLogger() {
        return logger;
    }

    @Override
    @Deprecated
    public SupportLevel getDeviceSupportLevel() {
        return DeviceSupport.getDeviceSupportLevel();
    }


    @Override
    public String toString() {
        return "AndroidBluetoothStack{" +
                "applicationContext=" + applicationContext +
                ", scheduler=" + scheduler +
                ", adapter=" + adapter +
                '}';
    }
}
