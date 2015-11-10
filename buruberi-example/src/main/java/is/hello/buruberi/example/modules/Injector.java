package is.hello.buruberi.example.modules;

import android.support.annotation.NonNull;

public interface Injector {
    <T> void inject(@NonNull T target);
}
