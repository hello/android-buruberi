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

import android.support.annotation.NonNull;

import junit.framework.Assert;

/**
 * Extensions to JUnit's Assert class. All methods are intended to be statically imported.
 */
public class AssertExtensions {
    /**
     * Checks that a given {@link ThrowingRunnable} throws an exception, calling fail if it does not.
     * @param runnable  The runnable that should throw an exception. Required.
     */
    public static void assertThrows(@NonNull ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable e) {
            return;
        }

        Assert.fail("expected exception, got none");
    }

    /**
     * Variant of the Runnable interface that throws a generic checked exception.
     */
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
