package is.hello.buruberi.bluetooth.stacks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;

/**
 * Simple implementation of {@see OperationTimeout} that uses deferred workers.
 */
public final class SchedulerOperationTimeout implements OperationTimeout {
    private final String name;
    private final long durationMs;
    private final LoggerFacade logger;

    private @Nullable Action0 action;
    private @Nullable Scheduler scheduler;
    private @Nullable Subscription subscription;


    public SchedulerOperationTimeout(@NonNull String name,
                                     long duration,
                                     @NonNull TimeUnit timeUnit,
                                     @NonNull LoggerFacade logger) {
        this.name = name;
        this.logger = logger;
        this.durationMs = timeUnit.toMillis(duration);

        this.logger.info(LOG_TAG, "Created time out '" + name + "'");
    }


    @Override
    public void schedule() {
        if (action == null || scheduler == null) {
            throw new IllegalStateException("Cannot schedule a time out that has no action");
        }

        logger.info(LOG_TAG, "Scheduling time out '" + name + "'");

        if (subscription != null && !subscription.isUnsubscribed()) {
            unschedule();
        }

        // It's the responsibility of the scheduler of the timeout
        // to clean up after it when a timeout condition occurs.
        this.subscription = scheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                if (subscription == null) {
                    return;
                }

                action.call();
            }
        }, durationMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void unschedule() {
        if (subscription != null) {
            logger.info(LOG_TAG, "Unscheduling time out '" + name + "'");

            subscription.unsubscribe();
            this.subscription = null;
        }
    }

    @Override
    public void reschedule() {
        unschedule();
        schedule();
    }

    @Override
    public void setTimeoutAction(@NonNull Action0 action, @NonNull Scheduler scheduler) {
        this.action = action;
        this.scheduler = scheduler;
    }


    @Override
    public String toString() {
        return "SchedulerOperationTimeout{" +
                "name='" + name + '\'' +
                ", durationMs=" + durationMs +
                ", action=" + action +
                ", scheduler=" + scheduler +
                ", subscription=" + subscription +
                '}';
    }
}
