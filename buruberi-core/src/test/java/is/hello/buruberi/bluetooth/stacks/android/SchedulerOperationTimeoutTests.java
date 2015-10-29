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

import org.junit.Test;
import org.robolectric.Robolectric;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import is.hello.buruberi.bluetooth.stacks.OperationTimeout;
import is.hello.buruberi.testing.BuruberiTestCase;
import is.hello.buruberi.util.Defaults;
import is.hello.buruberi.util.Rx;
import rx.Scheduler;
import rx.functions.Action0;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SchedulerOperationTimeoutTests extends BuruberiTestCase {
    private static final Scheduler TEST_SCHEDULER = Rx.mainThreadScheduler();

    @Test
    public void scheduling() throws Exception {
        OperationTimeout timeout = new SchedulerOperationTimeout("Test", 500, TimeUnit.MILLISECONDS, Defaults.createLogcatFacade());
        final AtomicBoolean called = new AtomicBoolean();
        timeout.setTimeoutAction(new Action0() {
            @Override
            public void call() {
                called.set(true);
            }
        }, TEST_SCHEDULER);

        timeout.schedule();
        timeout.unschedule();
        assertFalse(called.get());

        timeout.schedule();
        Robolectric.flushForegroundThreadScheduler();
        Thread.sleep(800, 0);
        timeout.unschedule();
        assertTrue(called.get());
    }
}
