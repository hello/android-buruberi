package is.hello.buruberi.example.presenters;

import android.support.annotation.NonNull;

import is.hello.buruberi.util.Rx;
import rx.Observable;
import rx.functions.Action0;
import rx.subjects.ReplaySubject;

/**
 * Base class for all presenters. Includes automatic tracking of 'working' state
 * to make it simple to bind a loading indicator to a presenter's state.
 */
public abstract class BasePresenter {
    /**
     * Indicates whether or not the presenter is currently working.
     * <p>
     * Guaranteed to have a value. Never emits an error.
     */
    public final ReplaySubject<Boolean> working = ReplaySubject.createWithSize(1);

    protected BasePresenter() {
        working.onNext(false);
    }

    /**
     * Updates the working state of the presenter.
     * @param working   The new state.
     */
    protected void setWorking(boolean working) {
        this.working.onNext(working);
    }

    /**
     * Returns the instantaneous working state of the presenter.
     */
    public boolean isWorking() {
        return working.toBlocking().first();
    }

    /**
     * Binds a given presenter to the presenter. When the observable is subscribed to,
     * the presenter will have its working state set to true, and when the observable
     * terminates from either completion or an error, the presenter's working state will
     * be set to false.
     * @param observable    The observable to bind.
     * @param <U>   The type of value the observable emits.
     * @return A new bound copy of the given observable.
     */
    protected <U> Observable<U> bind(@NonNull Observable<U> observable) {
        return observable.doOnSubscribe(new Action0() {
            @Override
            public void call() {
                setWorking(true);
            }
        })
                         .doOnTerminate(new Action0() {
                             @Override
                             public void call() {
                                 setWorking(false);
                             }
                         })
                         .observeOn(Rx.mainThreadScheduler());
    }
}
