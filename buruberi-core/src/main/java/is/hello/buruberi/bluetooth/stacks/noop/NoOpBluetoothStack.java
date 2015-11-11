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
package is.hello.buruberi.bluetooth.stacks.noop;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import is.hello.buruberi.bluetooth.Buruberi;
import is.hello.buruberi.bluetooth.errors.ChangePowerStateException;
import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.buruberi.util.Rx;
import rx.Observable;
import rx.Scheduler;

/**
 * An implementation of {@link BluetoothStack} that does nothing, and always indicates
 * that it's disabled. Vended by the {@link Buruberi} factory when the user's device
 * does not support Bluetooth Low Energy, or if the app bundling Būrūberi does not have
 * the correct permissions in its {@code AndroidManifest.xml}. Intentionally targets an
 * SDK level that does not include the Bluetooth Low Energy APIs.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class NoOpBluetoothStack implements BluetoothStack {
    private final LoggerFacade logger;

    public NoOpBluetoothStack(@NonNull LoggerFacade logger) {
        this.logger = logger;

        logger.error(LOG_TAG, "Device does not support Bluetooth", null);
    }

    @NonNull
    @Override
    public Observable<List<GattPeripheral>> discoverPeripherals(@NonNull PeripheralCriteria peripheralCriteria) {
        return Observable.just(Collections.<GattPeripheral>emptyList());
    }

    @NonNull
    @Override
    public Scheduler getScheduler() {
        return Rx.mainThreadScheduler();
    }

    @Override
    public <T> Observable<T> newConfiguredObservable(@NonNull Observable.OnSubscribe<T> onSubscribe) {
        return Observable.create(onSubscribe)
                         .subscribeOn(getScheduler());
    }

    @Override
    public Observable<Boolean> enabled() {
        return Observable.just(false);
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public Observable<Void> turnOn() {
        return Observable.error(new ChangePowerStateException());
    }

    @Override
    public Observable<Void> turnOff() {
        return Observable.error(new ChangePowerStateException());
    }

    @Override
    public boolean errorRequiresReconnect(@Nullable Throwable e) {
        return false;
    }

    @NonNull
    @Override
    public LoggerFacade getLogger() {
        return logger;
    }

    @Override
    public SupportLevel getDeviceSupportLevel() {
        return SupportLevel.UNSUPPORTED;
    }
}
