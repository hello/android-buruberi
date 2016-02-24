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

import android.support.annotation.Nullable;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import is.hello.buruberi.testing.AssertExtensions;
import is.hello.buruberi.testing.BuruberiTestCase;

import static is.hello.buruberi.testing.AssertExtensions.assertThrows;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class SerialQueueTests extends BuruberiTestCase {
    @Test
    public void simpleExecution() throws Exception {
        final SerialQueue queue = new SerialQueue();
        final AtomicInteger runCount = new AtomicInteger(0);
        queue.execute(new SerialQueue.Task() {
            @Override
            public void run() {
                runCount.incrementAndGet();
                queue.taskDone();
            }

            @Override
            public void cancel(@Nullable Throwable cause) {
                fail();
            }
        });
        queue.execute(new SerialQueue.Task() {
            @Override
            public void run() {
                runCount.incrementAndGet();
                queue.taskDone();
            }

            @Override
            public void cancel(@Nullable Throwable cause) {
                fail();
            }
        });
        assertThat(runCount.get(), is(equalTo(2)));
    }

    @Test
    public void queuing() throws Exception {
        final SerialQueue queue = new SerialQueue();
        final AtomicInteger runCount = new AtomicInteger(0);
        queue.execute(new SerialQueue.Task() {
            @Override
            public void run() {
                runCount.incrementAndGet();
            }

            @Override
            public void cancel(@Nullable Throwable cause) {
                fail();
            }
        });
        queue.execute(new SerialQueue.Task() {
            @Override
            public void run() {
                runCount.incrementAndGet();
                queue.taskDone();
            }

            @Override
            public void cancel(@Nullable Throwable cause) {
                fail();
            }
        });
        queue.execute(new SerialQueue.Task() {
            @Override
            public void run() {
                runCount.incrementAndGet();
                queue.taskDone();
            }

            @Override
            public void cancel(@Nullable Throwable cause) {
                fail();
            }
        });
        assertThat(runCount.get(), is(equalTo(1)));
        queue.taskDone();
        assertThat(runCount.get(), is(equalTo(3)));
    }

    @Test
    public void exceptions() throws Exception {
        final SerialQueue queue = new SerialQueue();

        // Have to force the queue into the intended state
        queue.queue.offer(new SerialQueue.Task() {
            @Override
            public void run() {
                throw new RuntimeException(">_<");
            }

            @Override
            public void cancel(@Nullable Throwable cause) {
                fail();
            }
        });
        final AtomicBoolean cancelCalled = new AtomicBoolean();
        queue.queue.offer(new SerialQueue.Task() {
            @Override
            public void cancel(@Nullable Throwable cause) {
                cancelCalled.set(true);
                assertThat(cause, is(instanceOf(RuntimeException.class)));
            }

            @Override
            public void run() {
                fail();
            }
        });
        queue.busy = true;

        assertThrows(new AssertExtensions.ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                queue.pollTask();
            }
        });

        final AtomicBoolean fineAfterException = new AtomicBoolean();
        queue.execute(new SerialQueue.Task() {
            @Override
            public void run() {
                fineAfterException.set(true);
                queue.taskDone();
            }

            @Override
            public void cancel(@Nullable Throwable cause) {
                fail();
            }
        });

        assertThat(fineAfterException.get(), is(true));
    }

    @Test
    public void cancelPending() {
        final SerialQueue queue = new SerialQueue();

        // Have to force the queue into the intended state
        final AtomicInteger cancelCount = new AtomicInteger(0);
        queue.queue.offer(new SerialQueue.Task() {
            @Override
            public void run() {
                fail();
            }

            @Override
            public void cancel(@Nullable Throwable cause) {
                cancelCount.incrementAndGet();
                assertThat(cause, is(nullValue()));
            }
        });
        queue.queue.offer(new SerialQueue.Task() {
            @Override
            public void run() {
                fail();
            }

            @Override
            public void cancel(@Nullable Throwable cause) {
                cancelCount.incrementAndGet();
                assertThat(cause, is(nullValue()));
            }
        });
        queue.busy = true;

        queue.cancelPending();
        assertThat(cancelCount.get(), is(equalTo(2)));
    }
}
