package is.hello.buruberi.example.presenters;

import android.support.annotation.NonNull;

import is.hello.buruberi.util.Rx;
import rx.Observable;
import rx.functions.Action0;
import rx.subjects.ReplaySubject;

public abstract class BasePresenter {
    public final ReplaySubject<Boolean> working = ReplaySubject.createWithSize(1);

    protected BasePresenter() {
        working.onNext(false);
    }

    protected void setWorking(boolean working) {
        this.working.onNext(working);
    }

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
