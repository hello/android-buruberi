package is.hello.buruberi.example.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import is.hello.buruberi.example.modules.Injection;
import is.hello.buruberi.util.Rx;
import rx.Observable;
import rx.functions.Func1;

public abstract class BaseActivity extends AppCompatActivity {
    private static final Func1<BaseActivity, Boolean> IS_VALID = new Func1<BaseActivity, Boolean>() {
        @Override
        public Boolean call(BaseActivity activity) {
            return !activity.isDestroyed();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injection.inject(this);
    }

    public <T> Observable<T> bind(@NonNull Observable<T> observable) {
        return observable.lift(new Rx.OperatorConditionalBinding<T, BaseActivity>(this, IS_VALID));
    }
}
