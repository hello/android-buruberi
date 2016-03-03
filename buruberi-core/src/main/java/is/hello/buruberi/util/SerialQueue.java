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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

/**
 * A serial task queue that decouples the start and completion of execution of a task
 * to allow serialization of tasks with asynchronous starts and finishes.
 */
public final class SerialQueue {
    private final String LOG_TAG = SerialQueue.class.getSimpleName();

    @VisibleForTesting final Queue<Task> queue = new LinkedList<>();
    @VisibleForTesting boolean busy = false;

    /**
     * Submit a task for execution into the queue. If the queue is currently empty,
     * the task will be synchronously run immediately.
     * @param task  The task to execute.
     */
    public void execute(@NonNull Task task) {
        Log.d(LOG_TAG, "execute(" + task + ") [busy: " + busy + "]");
        queue.offer(task);

        if (!busy) {
            Log.d(LOG_TAG, "-- start -- ");

            this.busy = true;
            pollTask();
        }
    }

    @VisibleForTesting void pollTask() {
        Log.d(LOG_TAG, "pollTask()");

        Task task = queue.poll();
        if (task != null) {
            try {
                task.run();
            } catch (Throwable e) {
                Log.d(LOG_TAG, "-- error clean up " + e + " --");
                cancelPending(e);
                throw e;
            }
        }
    }

    /**
     * Informs the queue that the currently executing task has completed,
     * and the next task in the queue should be run. If there is another
     * task in the queue, it will immediately be run.
     */
    public void taskDone() {
        Log.d(LOG_TAG, "taskDone()");

        if (!queue.isEmpty()) {
            pollTask();
        } else {
            Log.d(LOG_TAG, "-- finish -- ");

            this.busy = false;
        }
    }

    /**
     * Cancels any pending tasks in the queue, passing an optional cause object to them.
     * @param cause The cause of the queue cancellation.
     */
    public void cancelPending(@Nullable Throwable cause) {
        Log.d(LOG_TAG, "cancelPending()");

        Task enqueuedTask;
        while ((enqueuedTask = queue.poll()) != null) {
            enqueuedTask.cancel(cause);
        }
        this.busy = false;
    }

    /**
     * Cancels any pending tasks in the queue.
     */
    public void cancelPending() {
        cancelPending(null);
    }


    /**
     * A single unit of work to run inside of a {@link SerialQueue}.
     * <p>
     * When a {@code Task} has completed its work, it must call {@link SerialQueue#taskDone()}.
     * <p>
     * If the {@link #run()} method of the {@code Task} throws an exception,
     * the exception will be logged, and any pending tasks in the serial queue
     * will be immediately canceled.
     */
    public interface Task extends Runnable {
        /**
         * Informs the task it has been canceled before it could be run.
         * @param cause The cause of the cancellation.
         */
        void cancel(@Nullable Throwable cause);
    }
}
