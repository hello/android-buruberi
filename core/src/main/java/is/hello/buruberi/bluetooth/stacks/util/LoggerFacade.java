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
package is.hello.buruberi.bluetooth.stacks.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface LoggerFacade {
    void error(@NonNull String tag, @Nullable String message, @Nullable Throwable e);
    void warn(@NonNull String tag, @Nullable String message, @Nullable Throwable e);
    void warn(@NonNull String tag, @Nullable String message);
    void info(@NonNull String tag, @Nullable String message);
    void debug(@NonNull String tag, @Nullable String message);
}
