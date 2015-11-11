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
package is.hello.buruberi.bluetooth.stacks;

import android.support.annotation.NonNull;

import rx.Scheduler;
import rx.functions.Action0;

/**
 * Encapsulates a simple expiration timer. Implementations are provided by buruberi.
 * <p>
 * The stack will {@link #schedule()} a timeout when it begins
 * an operation, and {@link #unschedule()} it when the task terminates.
 * <p>
 * <em>Important:</em> OperationTimeout implementations are not guaranteed to be thread-safe.
 */
public interface OperationTimeout {
    String LOG_TAG = "Bluetooth." + OperationTimeout.class.getSimpleName();

    /**
     * Called by {@link GattPeripheral}. Schedules a timeout timer.
     * <p>
     * This method is not assumed to be safe to call until after
     * {@link #setTimeoutAction(Action0, Scheduler)} is called.
     */
    void schedule();

    /**
     * Called by {@link GattPeripheral}. Unschedules the timeout timer.
     * <p>
     * It is a valid condition for this method to be called multiple times,
     * it should become a no-op after the first call.
     * <p>
     * This method is not assumed to be safe to call until after
     * {@link #setTimeoutAction(Action0, Scheduler)} is called.
     */
    void unschedule();

    /**
     * For use by clients. Unschedules and reschedules the timeout timer.
     * <p>
     * This method is not assumed to be safe to call until after
     * {@link #setTimeoutAction(Action0, Scheduler)} is called.
     */
    void reschedule();

    /**
     * Called by {@link GattPeripheral}. Specifies an action to run when
     * the timeout expires that will allow the stack to clean up any resources.
     * <p>
     * Client code should check the state of a peripheral after a timeout has expired.
     *
     * @param action    Stack specific logic to handle the timeout.
     * @param scheduler The scheduler to run the handler on.
     */
    void setTimeoutAction(@NonNull Action0 action, @NonNull Scheduler scheduler);
}
