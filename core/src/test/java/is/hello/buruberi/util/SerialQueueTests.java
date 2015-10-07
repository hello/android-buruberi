package is.hello.buruberi.util;

import android.support.annotation.Nullable;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import is.hello.buruberi.testing.AssertExtensions;
import is.hello.buruberi.testing.BuruberiTestCase;

import static is.hello.buruberi.testing.AssertExtensions.assertThrows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SerialQueueTests extends BuruberiTestCase {
    @Test
    public void simpleExecution() throws Exception {
        final SerialQueue executor = new SerialQueue();
        final AtomicInteger executed = new AtomicInteger(0);
        executor.execute(new SerialQueue.Task() {
            @Override
            public void run() {
                executed.incrementAndGet();
                executor.taskDone();
            }

            @Override
            public void cancel(@Nullable Throwable cause) {
                fail();
            }
        });
        executor.execute(new SerialQueue.Task() {
            @Override
            public void run() {
                executed.incrementAndGet();
                executor.taskDone();
            }

            @Override
            public void cancel(@Nullable Throwable cause) {
                fail();
            }
        });
        assertEquals(executed.get(), 2);
    }

    @Test
    public void queuing() throws Exception {
        final SerialQueue executor = new SerialQueue();
        final AtomicInteger executed = new AtomicInteger(0);
        executor.execute(new SerialQueue.Task() {
            @Override
            public void run() {
                executed.incrementAndGet();
            }

            @Override
            public void cancel(@Nullable Throwable cause) {
                fail();
            }
        });
        executor.execute(new SerialQueue.Task() {
            @Override
            public void run() {
                executed.incrementAndGet();
                executor.taskDone();
            }

            @Override
            public void cancel(@Nullable Throwable cause) {
                fail();
            }
        });
        executor.execute(new SerialQueue.Task() {
            @Override
            public void run() {
                executed.incrementAndGet();
                executor.taskDone();
            }

            @Override
            public void cancel(@Nullable Throwable cause) {
                fail();
            }
        });
        assertEquals(executed.get(), 1);
        executor.taskDone();
        assertEquals(executed.get(), 3);
    }

    @Test
    public void exceptions() throws Exception {
        final SerialQueue executor = new SerialQueue();

        // Have to force the queue into the intended state
        executor.queue.offer(new SerialQueue.Task() {
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
        executor.queue.offer(new SerialQueue.Task() {
            @Override
            public void cancel(@Nullable Throwable cause) {
                cancelCalled.set(true);
                assertTrue(cause instanceof RuntimeException);
            }

            @Override
            public void run() {
                fail();
            }
        });
        executor.busy = true;

        assertThrows(new AssertExtensions.ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                executor.pollTask();
            }
        });

        final AtomicBoolean fineAfterException = new AtomicBoolean();
        executor.execute(new SerialQueue.Task() {
            @Override
            public void run() {
                fineAfterException.set(true);
                executor.taskDone();
            }

            @Override
            public void cancel(@Nullable Throwable cause) {
                fail();
            }
        });

        assertTrue(fineAfterException.get());
    }
}
