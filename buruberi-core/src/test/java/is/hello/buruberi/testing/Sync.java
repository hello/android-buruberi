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
package is.hello.buruberi.testing;

import android.support.annotation.NonNull;

import org.hamcrest.Matcher;
import org.junit.Assert;

import java.util.Iterator;

import rx.Observable;
import rx.observables.BlockingObservable;

/**
 * A wrapper around BlockingObservable to make it more or
 * less idiot-proof to use when writing tests.
 * <p>
 * All operators throw exceptions emitted by the source observable.
 * @param <T>   The type emitted by the Sync wrapper.
 */
public final class Sync<T> implements Iterable<T> {
    /**
     * The wrapped observable.
     */
    private final BlockingObservable<T> observable;


    //region Creation

    /**
     * Wraps an unbounded source observable.
     * <p>
     * This method <b>does not</b> work PresenterSubject.
     */
    public static <T> Sync<T> wrap(@NonNull Observable<T> source) {
        return new Sync<>(source);
    }


    private Sync(@NonNull Observable<T> source) {
        this.observable = source.toBlocking();
    }

    //endregion


    //region Binding

    /**
     * Returns an iterator that yields any values the wrapped
     * observable has already emitted. <b>This method will
     * not block for values.</b>
     */
    @Override
    public Iterator<T> iterator() {
        return observable.getIterator();
    }

    /**
     * Blocks until the observable completes, then returns the last emitted value.
     */
    public T last() {
        return observable.last();
    }

    //endregion


    //region Assertions

    /**
     * Blocks until the observable errors out.
     * <p>
     * This method raises an assertion failure if the observable does not fail,
     * or if the error passed out of the observable does not match the given class.
     */
    public <E extends Throwable> void assertThrows(@NonNull Class<E> errorClass) {
        try {
            last();
            Assert.fail("Observable did not fail as expected");
        } catch (Throwable e) {
            if (!errorClass.isAssignableFrom(e.getClass()) &&
                    e.getCause() != null && !errorClass.isAssignableFrom(e.getCause().getClass())) {
                Assert.fail("Unexpected failure '" + e.getClass() + "'");
            }
        }
    }

    /**
     * Blocks until the observable completes then applies a matcher to the result.
     * @param matcher   The matcher.
     * @return  The matched value.
     */
    public T assertThat(@NonNull Matcher<? super T> matcher) {
        T last = last();
        Assert.assertThat(last, matcher);
        return last;
    }

    //endregion


    //region Convenience

    /**
     * Shorthand for <code>Sync.wrap(observable).last();</code>.
     *
     * @see #wrap(Observable)
     * @see #last()
     */
    public static <T> T last(@NonNull Observable<T> source) {
        return wrap(source).last();
    }

    //endregion
}
