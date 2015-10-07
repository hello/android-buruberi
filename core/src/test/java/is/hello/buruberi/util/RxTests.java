package is.hello.buruberi.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import org.junit.Test;
import org.robolectric.Robolectric;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import is.hello.buruberi.testing.BuruberiTestCase;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.ReplaySubject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RxTests extends BuruberiTestCase {
    private static final String ACTION_TEST = RxTests.class.getName() + ".ACTION_TEST";

    @Test
    public void broadcasts() throws Exception {
        Context context = getContext();

        Observable<Intent> observable = Rx.fromBroadcast(context, new IntentFilter(ACTION_TEST));
        final AtomicInteger counter = new AtomicInteger(0);
        Subscription subscription = observable.subscribe(new Action1<Intent>() {
            @Override
            public void call(Intent intent) {
                counter.incrementAndGet();
            }
        });

        context.sendBroadcast(new Intent(ACTION_TEST));
        assertEquals(1, counter.get());

        context.sendBroadcast(new Intent(ACTION_TEST));
        assertEquals(2, counter.get());

        subscription.unsubscribe();

        context.sendBroadcast(new Intent(ACTION_TEST));
        assertEquals(2, counter.get());
    }

    @Test
    public void localBroadcasts() throws Exception {
        Context context = getContext();
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);

        Observable<Intent> observable = Rx.fromLocalBroadcast(context, new IntentFilter(ACTION_TEST));
        final AtomicInteger counter = new AtomicInteger(0);
        Subscription subscription = observable.subscribe(new Action1<Intent>() {
            @Override
            public void call(Intent intent) {
                counter.incrementAndGet();
            }
        });

        localBroadcastManager.sendBroadcast(new Intent(ACTION_TEST));
        assertEquals(1, counter.get());

        localBroadcastManager.sendBroadcast(new Intent(ACTION_TEST));
        assertEquals(2, counter.get());

        subscription.unsubscribe();

        localBroadcastManager.sendBroadcast(new Intent(ACTION_TEST));
        assertEquals(2, counter.get());
    }

    @Test
    public void handlerSchedulers() throws Exception {
        Rx.HandlerScheduler scheduler = Rx.mainThreadScheduler();
        Scheduler.Worker worker = scheduler.createWorker();


        final CountDownLatch immediateLatch = new CountDownLatch(1);
        final AtomicBoolean immediateCalled = new AtomicBoolean();
        worker.schedule(new Action0() {
            @Override
            public void call() {
                immediateCalled.set(true);
                immediateLatch.countDown();
            }
        });
        Robolectric.flushForegroundThreadScheduler();
        immediateLatch.await(1, TimeUnit.SECONDS);
        assertTrue(immediateCalled.get());


        final CountDownLatch delayedLatch = new CountDownLatch(1);
        final AtomicBoolean delayedCalled = new AtomicBoolean();
        final long scheduledTime = SystemClock.uptimeMillis();
        final AtomicLong calledTime = new AtomicLong();
        worker.schedule(new Action0() {
            @Override
            public void call() {
                calledTime.set(SystemClock.uptimeMillis());
                delayedCalled.set(true);
                delayedLatch.countDown();
            }
        }, 500, TimeUnit.MILLISECONDS);
        Robolectric.flushForegroundThreadScheduler();
        delayedLatch.await(1, TimeUnit.SECONDS);
        assertTrue(delayedCalled.get());
        long timePassed = calledTime.get() - scheduledTime;
        assertTrue(timePassed >= 500 && timePassed <= 700); // Allow some latency
    }

    @Test
    public void operatorConditionalBinding() throws Exception {
        AtomicBoolean permitted = new AtomicBoolean(true);
        Func1<AtomicBoolean, Boolean> predicate = new Func1<AtomicBoolean, Boolean>() {
            @Override
            public Boolean call(AtomicBoolean atomicBoolean) {
                return atomicBoolean.get();
            }
        };

        ReplaySubject<Integer> numbers = ReplaySubject.createWithSize(1);

        final AtomicInteger counter = new AtomicInteger();
        final AtomicBoolean errorCalled = new AtomicBoolean();
        numbers.lift(new Rx.OperatorConditionalBinding<Integer, AtomicBoolean>(permitted, predicate))
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        errorCalled.set(true);
                    }

                    @Override
                    public void onNext(Integer number) {
                        counter.addAndGet(number);
                    }
                });

        numbers.onNext(1);
        numbers.onNext(2);
        numbers.onNext(3);

        assertEquals(6, counter.get());

        permitted.set(false);

        numbers.onNext(3);
        numbers.onNext(4);
        numbers.onNext(5);
        numbers.onError(new Throwable("Everything is broken!"));

        assertEquals(6, counter.get());
        assertFalse(errorCalled.get());
    }

    @Test
    public void operatorUnbufferedObserveOn() throws Exception {
        ReplaySubject<Integer> numbers = ReplaySubject.createWithSize(1);

        final AtomicInteger counter = new AtomicInteger();
        numbers.lift(new Rx.OperatorUnbufferedObserveOn<Integer>(Schedulers.immediate()))
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer number) {
                        counter.addAndGet(number);
                    }
                });

        numbers.onNext(3);
        numbers.onNext(3);
        numbers.onNext(3);

        assertEquals(9, counter.get());
    }

    @Test
    public void serialize() throws Exception {
        final SerialQueue executor = new SerialQueue();

        AtomicInteger counter = new AtomicInteger(0);
        TestSource firstSource = new TestSource(false, counter);
        Observable<Void> first = Rx.serialize(Observable.create(firstSource), executor);

        assertEquals(0, counter.get());
        first.subscribe();
        assertEquals(1, counter.get());

        assertTrue(executor.busy);

        TestSource secondSource = new TestSource(true, counter);
        Observable<Void> second = Rx.serialize(Observable.create(secondSource), executor);

        second.subscribe();
        assertEquals(1, counter.get());
        assertFalse(executor.queue.isEmpty());

        firstSource.complete();
        assertEquals(2, counter.get());
        assertFalse(executor.busy);
    }


    static class TestSource extends AtomicInteger implements Observable.OnSubscribe<Void> {
        private final boolean completeImmediately;
        private final AtomicInteger subscribeCounter;
        private Subscriber<? super Void> subscriber;

        public TestSource(boolean completeImmediately,
                          @NonNull AtomicInteger subscribeCounter) {
            this.completeImmediately = completeImmediately;
            this.subscribeCounter = subscribeCounter;
        }

        @Override
        public void call(Subscriber<? super Void> subscriber) {
            subscribeCounter.incrementAndGet();

            this.subscriber = subscriber;

            subscriber.onNext(null);

            if (completeImmediately) {
                complete();
            }
        }

        void complete() {
            subscriber.onCompleted();
        }
    }
}
