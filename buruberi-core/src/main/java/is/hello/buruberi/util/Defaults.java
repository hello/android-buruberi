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
package is.hello.buruberi.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import is.hello.buruberi.bluetooth.stacks.util.ErrorListener;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;

public final class Defaults {
    public static LoggerFacade createLogcatFacade() {
        return new LoggerFacade() {
            @Override
            public void error(@NonNull String tag, @Nullable String message, @Nullable Throwable e) {
                Log.e(tag, message, e);
            }

            @Override
            public void warn(@NonNull String tag, @Nullable String message, @Nullable Throwable e) {
                Log.w(tag, message, e);
            }

            @Override
            public void warn(@NonNull String tag, @Nullable String message) {
                Log.w(tag, message);
            }

            @Override
            public void info(@NonNull String tag, @Nullable String message) {
                Log.i(tag, message);
            }

            @Override
            public void debug(@NonNull String tag, @Nullable String message) {
                Log.d(tag, message);
            }
        };
    }

    public static ErrorListener createEmptyErrorListener() {
        return new ErrorListener() {
            @Override
            public void call(Throwable ignored) {
                // Do nothing.
            }
        };
    }
}
