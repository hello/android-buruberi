/**
 * Derived from portions of older versions of the RxAndroid library.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package is.hello.buruberi.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.internal.schedulers.ScheduledAction;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

public class Rx {
    //region Broadcasts

    public static Observable<Intent> fromBroadcast(@NonNull Context context, @NonNull IntentFilter intent) {
        return Observable.create(new BroadcastRegister(context, intent));
    }

    public static class BroadcastRegister implements Observable.OnSubscribe<Intent> {
        private final Context context;
        private final IntentFilter intent;

        public BroadcastRegister(@NonNull Context context, @NonNull IntentFilter intent) {
            this.context = context;
            this.intent = intent;
        }

        @Override
        public void call(final Subscriber<? super Intent> subscriber) {
            final BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    subscriber.onNext(intent);
                }
            };

            final Subscription subscription = Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    context.unregisterReceiver(receiver);
                }
            });
            subscriber.add(subscription);

            context.registerReceiver(receiver, intent);
        }
    }

    //endregion


    //region Local Broadcasts

    public static Observable<Intent> fromLocalBroadcast(@NonNull Context context, @NonNull IntentFilter intent) {
        return Observable.create(new LocalBroadcastRegister(context, intent));
    }

    public static class LocalBroadcastRegister implements Observable.OnSubscribe<Intent> {
        private final Context context;
        private final IntentFilter intent;

        public LocalBroadcastRegister(@NonNull Context context, @NonNull IntentFilter intent) {
            this.context = context;
            this.intent = intent;
        }

        @Override
        public void call(final Subscriber<? super Intent> subscriber) {
            final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(context);

            final BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    subscriber.onNext(intent);
                }
            };

            final Subscription subscription = Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    manager.unregisterReceiver(receiver);
                }
            });
            subscriber.add(subscription);

            manager.registerReceiver(receiver, intent);
        }
    }

    //endregion


    //region Schedulers

    private static final HandlerScheduler MAIN_THREAD_SCHEDULER = new HandlerScheduler(new Handler(Looper.getMainLooper()));

    public static HandlerScheduler mainThreadScheduler() {
        return MAIN_THREAD_SCHEDULER;
    }

    public static class HandlerScheduler extends Scheduler {
        private final Handler handler;

        //region Creation

        public HandlerScheduler(@NonNull Handler handler) {
            this.handler = handler;
        }

        //endregion


        //region Workers

        @Override
        public Worker createWorker() {
            return new HandlerWorker(handler);
        }

        static class HandlerWorker extends Worker {
            private final Handler handler;
            private final CompositeSubscription compositeSubscription = new CompositeSubscription();

            HandlerWorker(@NonNull Handler handler) {
                this.handler = handler;
            }

            @Override
            public Subscription schedule(Action0 action) {
                return schedule(action, 0, TimeUnit.MILLISECONDS);
            }

            @Override
            public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
                final ScheduledAction scheduledAction = new ScheduledAction(action);
                scheduledAction.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        handler.removeCallbacks(scheduledAction);
                    }
                }));
                scheduledAction.addParent(compositeSubscription);
                compositeSubscription.add(scheduledAction);

                handler.postDelayed(scheduledAction, unit.toMillis(delayTime));

                return scheduledAction;
            }

            @Override
            public void unsubscribe() {
                compositeSubscription.unsubscribe();
            }

            @Override
            public boolean isUnsubscribed() {
                return compositeSubscription.isUnsubscribed();
            }
        }

        //endregion
    }

    //endregion


    //region Operators

    /**
     * Inserts a bound if-statement within an observable stream.
     * @param <T>   The type of the stream.
     * @param <U>   The type of value to bind the operator's predicate to.
     */
    public static class OperatorConditionalBinding<T, U> implements Observable.Operator<T, T> {
        private U boundValue;
        private final Func1<? super U, Boolean> predicate;

        public OperatorConditionalBinding(@NonNull U boundValue,
                                          @NonNull Func1<? super U, Boolean> predicate) {
            this.predicate = predicate;
            this.boundValue = boundValue;
        }

        @Override
        public Subscriber<? super T> call(final Subscriber<? super T> child) {
            return new Subscriber<T>(child) {
                @Override
                public void onCompleted() {
                    if (shouldForward()) {
                        child.onCompleted();
                    } else {
                        handleLostBinding();
                    }
                }

                @Override
                public void onError(Throwable e) {
                    if (shouldForward()) {
                        child.onError(e);
                    } else {
                        handleLostBinding();
                    }
                }

                @Override
                public void onNext(T value) {
                    if (shouldForward()) {
                        child.onNext(value);
                    } else {
                        handleLostBinding();
                    }
                }


                private boolean shouldForward() {
                    return (boundValue != null && predicate.call(boundValue));
                }

                private void handleLostBinding() {
                    OperatorConditionalBinding.this.boundValue = null;
                    unsubscribe();
                }
            };
        }
    }

    /**
     * Variation of {@link rx.internal.operators.OperatorObserveOn}
     * that does not buffer values pushed through the stream.
     */
    public static class OperatorUnbufferedObserveOn<T> implements Observable.Operator<T, T> {
        private final Scheduler scheduler;

        public OperatorUnbufferedObserveOn(@NonNull Scheduler scheduler) {
            this.scheduler = scheduler;
        }

        @Override
        public Subscriber<? super T> call(Subscriber<? super T> child) {
            return new UnsafeObserveOnSubscriber<>(child, scheduler);
        }

        private static class UnsafeObserveOnSubscriber<T> extends Subscriber<T> {
            private final Subscriber<T> child;
            private final Scheduler.Worker worker;

            private UnsafeObserveOnSubscriber(@NonNull Subscriber<T> child, @NonNull Scheduler scheduler) {
                super(child);

                this.child = child;
                this.worker = scheduler.createWorker();
            }

            @Override
            public void onCompleted() {
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        child.onCompleted();
                    }
                });
            }

            @Override
            public void onError(final Throwable e) {
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        child.onError(e);
                    }
                });
            }

            @Override
            public void onNext(final T value) {
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        child.onNext(value);
                    }
                });
            }
        }
    }

    //endregion


    //region Serialization

    public static <T> Observable<T> serialize(@NonNull Observable<T> source,
                                              @NonNull SerialQueue executor) {
        return Observable.create(new OnSubscribeSerializeSubscription<>(source, executor));
    }

    public static class OnSubscribeSerializeSubscription<T> implements Observable.OnSubscribe<T> {
        private final Observable<T> source;
        private final SerialQueue executor;

        public OnSubscribeSerializeSubscription(@NonNull Observable<T> source,
                                                @NonNull SerialQueue executor) {
            this.source = source;
            this.executor = executor;
        }

        @Override
        public void call(final Subscriber<? super T> subscriber) {
            final Subscriber<T> child = new Subscriber<T>() {
                @Override
                public void onCompleted() {
                    executor.taskDone();

                    if (isUnsubscribed()) {
                        return;
                    }

                    subscriber.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    executor.taskDone();

                    if (isUnsubscribed()) {
                        return;
                    }

                    subscriber.onError(e);
                }

                @Override
                public void onNext(T value) {
                    if (isUnsubscribed()) {
                        return;
                    }

                    try {
                        subscriber.onNext(value);
                    } catch (Throwable e) {
                        executor.taskDone();
                        throw e;
                    }
                }
            };
            subscriber.add(child);

            executor.execute(new SerialQueue.Task() {
                @Override
                public void run() {
                    source.unsafeSubscribe(child);
                }

                @Override
                public void cancel(@Nullable Throwable cause) {
                    subscriber.onError(new RuntimeException("Subscribe task canceled by queue", cause));
                    child.unsubscribe();
                }
            });
        }
    }

    //endregion
}
