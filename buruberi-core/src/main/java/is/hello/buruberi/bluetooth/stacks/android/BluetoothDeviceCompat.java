/*
 * Copyright 2015 Hello Inc.
 * Copyright (C) 2009 The Android Open Source Project
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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.util.Log;

import java.lang.reflect.Method;

import is.hello.buruberi.util.NonGuaranteed;

/**
 * Helper class for accessing features added to {@code BluetoothDevice}
 * in later versions of the Android API.
 */
public final class BluetoothDeviceCompat {
    public static final String LOG_TAG = BluetoothDeviceCompat.class.getSimpleName();

    /**
     * No preference of physical transport for GATT connections to remote dual-mode devices.
     * <p>
     * Corresponds to {@code BluetoothDevice.TRANSPORT_AUTO}.
     */
    public static final int TRANSPORT_AUTO = 0;

    /**
     * Prefer BR/EDR transport for GATT connections to remote dual-mode devices
     * <p>
     * Corresponds to {@code BluetoothDevice.TRANSPORT_BREDR}.
     */
    public static final int TRANSPORT_BREDR = 1;

    /**
     * Prefer LE transport for GATT connections to remote dual-mode devices
     * <p>
     * Corresponds to {@code BluetoothDevice.TRANSPORT_LE}.
     */
    public static final int TRANSPORT_LE = 2;

    /**
     * Connect to GATT Server hosted by this device.
     */
    @Nullable
    public static BluetoothGatt connectGatt(@NonNull BluetoothDevice device,
                                            @NonNull Context context,
                                            boolean autoConnect,
                                            @NonNull BluetoothGattCallback callback,
                                            int transport) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return device.connectGatt(context, autoConnect, callback, transport);
        } else {
            return device.connectGatt(context, autoConnect, callback);
        }
    }

    /**
     * Start the bonding (pairing) process with the remote device.
     * @param device The device.
     * @return false on immediate error, true if bonding will begin
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
    public static boolean createBond(@NonNull BluetoothDevice device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return device.createBond();
        } else {
            try {
                final Method method = device.getClass().getMethod("createBond", (Class[]) null);
                method.invoke(device, (Object[]) null);
                return true;
            } catch (Exception e) {
                Log.e(LOG_TAG, "Could not invoke `createBond` on BluetoothDevice.", e);
                return false;
            }
        }
    }

    /**
     * Start the bond clearing process.
     * @param device The device.
     * @return false on immediate error, true if removing the bond will begin
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
    @NonGuaranteed
    public static boolean removeBond(@NonNull BluetoothDevice device) {
        try {
            final Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
            return true;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not invoke `createBond` on BluetoothDevice.", e);
            return false;
        }
    }

    private BluetoothDeviceCompat() {
    }
}
