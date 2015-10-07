/*
 * Copyright 2015 Hello, Inc
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


public final class SerialQueue {
    private final String LOG_TAG = SerialQueue.class.getSimpleName();

    @VisibleForTesting final Queue<Task> queue = new LinkedList<>();
    @VisibleForTesting boolean busy = false;

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

                Task enqueuedTask;
                while ((enqueuedTask = queue.poll()) != null) {
                    enqueuedTask.cancel(e);
                }
                this.busy = false;

                throw e;
            }
        }
    }

    public void taskDone() {
        Log.d(LOG_TAG, "taskDone()");

        if (!queue.isEmpty()) {
            pollTask();
        } else {
            Log.d(LOG_TAG, "-- finish -- ");

            this.busy = false;
        }
    }


    public interface Task extends Runnable {
        void cancel(@Nullable Throwable cause);
    }
}
