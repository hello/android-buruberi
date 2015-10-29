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
package is.hello.buruberi.testing;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanRecord;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.mockito.internal.stubbing.answers.DoesNothing;
import org.robolectric.util.ReflectionHelpers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import is.hello.buruberi.bluetooth.stacks.OperationTimeout;
import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;
import is.hello.buruberi.testing.shadows.BuruberiShadows;
import is.hello.buruberi.testing.shadows.ShadowBluetoothDeviceExt;
import is.hello.buruberi.util.AdvertisingDataBuilder;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class Testing {
    //region Mocking

    public static final UUID SERVICE_PRIMARY = UUID.randomUUID();
    public static final UUID WRITE_CHARACTERISTIC = UUID.randomUUID();
    public static final UUID NOTIFY_DESCRIPTOR = UUID.randomUUID();

    public static final String DEVICE_ADDRESS = "CA:FE:BE:EF:BA:BE";
    public static final int RSSI_DECENT = -50;
    public static final int RSSI_BETTER = -25;
    public static final byte[] EMPTY_SCAN_RESPONSE = {};
    public static final ScanRecord EMPTY_SCAN_RECORD = parseScanRecordFromBytes(EMPTY_SCAN_RESPONSE);
    public static final AdvertisingData EMPTY_ADVERTISING_DATA = new AdvertisingDataBuilder().build();


    public static OperationTimeout createMockOperationTimeout() {
        return mock(OperationTimeout.class, new DoesNothing());
    }

    public static BluetoothGattService createMockGattService() {
        BluetoothGattService service = spy(new BluetoothGattService(SERVICE_PRIMARY,
                                                                    BluetoothGattService.SERVICE_TYPE_PRIMARY));

        BluetoothGattCharacteristic characteristic = spy(new BluetoothGattCharacteristic(WRITE_CHARACTERISTIC,
                                                                                         BluetoothGattCharacteristic.PROPERTY_WRITE,
                                                                                         BluetoothGattCharacteristic.PERMISSION_WRITE));
        service.addCharacteristic(characteristic);

        BluetoothGattDescriptor descriptor = spy(new BluetoothGattDescriptor(NOTIFY_DESCRIPTOR,
                                                                             BluetoothGattDescriptor.PERMISSION_WRITE));
        characteristic.addDescriptor(descriptor);

        return service;
    }

    public static BluetoothGatt createMockGatt() {
        return ReflectionHelpers.newInstance(BluetoothGatt.class);
    }

    public static String randomDeviceName() {
        int suffix = new Random().nextInt(0x99);
        return "Shadow-" + Integer.toString(suffix, 16);
    }

    public static BluetoothDevice createMockDevice() {
        return createMockDevice(DEVICE_ADDRESS);
    }

    public static BluetoothDevice createMockDevice(@NonNull String address) {
        final BluetoothDevice device = ReflectionHelpers.newInstance(BluetoothDevice.class);
        final ShadowBluetoothDeviceExt deviceShadow = BuruberiShadows.shadowOf(device);
        deviceShadow.setName(randomDeviceName());
        deviceShadow.setAddress(address);
        return device;
    }

    //endregion


    //region Private APIs

    public static ScanRecord parseScanRecordFromBytes(byte[] bytes) {
        try {
            Method parseFromBytes = ScanRecord.class.getMethod("parseFromBytes", byte[].class);
            return (ScanRecord) parseFromBytes.invoke(null, (Object) bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //endregion


    //region Scheduler

    private static NoOpScheduler NO_OP_SCHEDULER = new NoOpScheduler(false);

    public static NoOpScheduler getNoOpScheduler() {
        return NO_OP_SCHEDULER;
    }

    public static class NoOpScheduler extends Scheduler {
        private final Worker worker;

        public NoOpScheduler(boolean executeDelayedActions) {
            this.worker = new Worker(executeDelayedActions);
        }

        @Override
        public Worker createWorker() {
            return worker;
        }

        static class Worker extends Scheduler.Worker {
            private final boolean executeDelayedActions;

            Worker(boolean executeDelayedActions) {
                this.executeDelayedActions = executeDelayedActions;
            }

            @Override
            public Subscription schedule(Action0 action) {
                action.call();
                return Subscriptions.unsubscribed();
            }

            @Override
            public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
                if (executeDelayedActions) {
                    action.call();
                }
                return Subscriptions.empty();
            }

            @Override
            public void unsubscribe() {
            }

            @Override
            public boolean isUnsubscribed() {
                return true;
            }
        }
    }

    //endregion


    public static class Result<T> extends Subscriber<T> {
        private final List<T> values = new ArrayList<>();
        private @Nullable Throwable error;
        private boolean completed = false;

        @Override
        public void onCompleted() {
            this.completed = true;
        }

        @Override
        public void onError(Throwable e) {
            this.completed = false;
            this.error = e;
        }

        @Override
        public void onNext(T value) {
            values.add(value);
        }


        public List<T> getValues() {
            return values;
        }

        public @Nullable Throwable getError() {
            return error;
        }

        public boolean isCompleted() {
            return completed;
        }
    }
}
