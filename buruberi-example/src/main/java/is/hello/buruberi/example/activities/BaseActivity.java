package is.hello.buruberi.example.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import is.hello.buruberi.example.R;
import is.hello.buruberi.example.modules.Injection;
import is.hello.buruberi.util.Rx;
import rx.Observable;
import rx.Observer;
import rx.functions.Func1;

/**
 * Extends the {@code AppCompatActivity} class to add automatic dependency injection,
 * and tools for interacting with {@code Observable<T>} objects vended by presenters.
 */
public abstract class BaseActivity extends AppCompatActivity {
    /**
     * A predicate that indicates whether or not it's safe to
     * deliver emitted observable items to a bound activity instance.
     */
    private static final Func1<BaseActivity, Boolean> IS_VALID = new Func1<BaseActivity, Boolean>() {
        @Override
        public Boolean call(BaseActivity activity) {
            return !activity.isDestroyed() && !activity.isFinishing();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injection.inject(this);
    }

    /**
     * Binds a given observable to the state of the activity, preventing emitted
     * items and errors from being delivered if an activity is destroyed.
     */
    public <T> Observable<T> bind(@NonNull Observable<T> observable) {
        return observable.lift(new Rx.OperatorConditionalBinding<T, BaseActivity>(this, IS_VALID));
    }

    /**
     * Subscribes to an observable, and calls {@link #presentError(Throwable)}
     * if the observable fails to complete. Does nothing otherwise.
     */
    public <T> void presentError(@NonNull Observable<T> observable) {
        bind(observable).subscribe(new Observer<T>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                presentError(e);
            }

            @Override
            public void onNext(T t) {
            }
        });
    }

    /**
     * Display a dialog for the information contained in a given error.
     */
    public void presentError(Throwable error) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_error);
        builder.setMessage(error.getLocalizedMessage());
        builder.setPositiveButton(android.R.string.ok, null);
        builder.create().show();
    }
}
