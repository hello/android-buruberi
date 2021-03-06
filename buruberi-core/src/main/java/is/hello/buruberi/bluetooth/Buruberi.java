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
package is.hello.buruberi.bluetooth;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.annotation.VisibleForTesting;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.android.NativeBluetoothStack;
import is.hello.buruberi.bluetooth.stacks.noop.NoOpBluetoothStack;
import is.hello.buruberi.bluetooth.stacks.util.ErrorListener;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.util.Defaults;

/**
 * Builder class for vending configured {@link BluetoothStack} instances.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public final class Buruberi {
    private Context applicationContext;
    private ErrorListener errorListener;
    private LoggerFacade loggerFacade;


    //region Attributes

    /**
     * <em>Required</em>
     * <p>
     * Sets the application context used by the {@code BluetoothStack}.
     * <p>
     * Calls {@code #getApplicationContext()} on the given {@code Context}.
     */
    public Buruberi setApplicationContext(@NonNull Context applicationContext) {
        this.applicationContext = applicationContext.getApplicationContext();
        return this;
    }

    /**
     * Sets the error listener to call whenever an error occurs within the {@code BluetoothStack}.
     * <p>
     * Builder provides a default implementation that does nothing.
     */
    public Buruberi setErrorListener(@NonNull ErrorListener errorListener) {
        this.errorListener = errorListener;
        return this;
    }

    /**
     * Sets the logger facade to be used by the {@code BluetoothStack}.
     * <p>
     * Builder provides a default implementation that logs to logcat.
     */
    public Buruberi setLoggerFacade(@NonNull LoggerFacade loggerFacade) {
        this.loggerFacade = loggerFacade;
        return this;
    }

    //endregion


    //region Building

    private void assertValid() {
        if (applicationContext == null) {
            throw new IllegalStateException("applicationContext == null");
        }

        if (errorListener == null) {
            this.errorListener = Defaults.createEmptyErrorListener();
        }

        if (loggerFacade == null) {
            this.loggerFacade = Defaults.createLogcatFacade();
        }
    }

    @VisibleForTesting boolean hasPermissions() {
        final int bluetoothStatus =
                    applicationContext.checkCallingOrSelfPermission(Manifest.permission.BLUETOOTH);
        final int adminStatus =
                applicationContext.checkCallingOrSelfPermission(Manifest.permission.BLUETOOTH_ADMIN);
        return (bluetoothStatus == PackageManager.PERMISSION_GRANTED &&
                adminStatus == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * Creates a new {@code BluetoothStack} instance using the parameters of this builder.
     *
     * @throws IllegalStateException if any <em>required</em> fields are missing.
     */
    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    })
    public BluetoothStack build() {
        assertValid();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2 || !hasPermissions()) {
            loggerFacade.error(getClass().getSimpleName(),
                               "Buruberi is running in an unsupported environment. " +
                                       "Are you on API level 18 with all required permissions?",
                               null);
            return new NoOpBluetoothStack(loggerFacade);
        } else {
            return new NativeBluetoothStack(applicationContext,
                                            errorListener,
                                            loggerFacade);
        }
    }

    //endregion
}
