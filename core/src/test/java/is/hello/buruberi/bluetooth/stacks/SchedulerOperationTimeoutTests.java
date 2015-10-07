package is.hello.buruberi.bluetooth.stacks;

import org.junit.Test;
import org.robolectric.Robolectric;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
