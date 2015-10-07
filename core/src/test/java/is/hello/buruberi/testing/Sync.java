package is.hello.buruberi.testing;

import android.support.annotation.NonNull;

import org.hamcrest.Matcher;
import org.junit.Assert;

import java.util.Iterator;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
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

    /**
     * Wraps a given observable, <code>take</code>ing one emitted value from it,
     * and firing a given action block before returning.
     * <p>
     * This method should be used with ValuePresenter subclasses when updating them:
     * <pre>
     *     Account account = Sync.wrapAfter(presenter::update, presenter.account).last();
     * </pre>
     */
    public static <T> Sync<T> wrapAfter(@NonNull Action0 action, @NonNull Observable<T> source) {
        Observable<T> next = source.take(1);
        Sync<T> sync = new Sync<>(next);
        action.call();
        return sync;
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
     * Calls a given action for each value emitted by the wrapped
     * observable, blocking until the observable completes.
     */
    public void forEach(@NonNull Action1<T> action) {
        observable.forEach(action);
    }

    /**
     * Blocks until the observable completes, then returns the last emitted value.
     */
    public T last() {
        return observable.last();
    }

    /**
     * Blocks until the observable completes, ignoring the emitted value.
     */
    public void await() {
        last();
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
