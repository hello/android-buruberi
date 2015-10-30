package is.hello.buruberi.example.modules;

import android.content.Context;
import android.support.annotation.NonNull;

public final class Injection {
    public static Injector getInjector(@NonNull Context context) {
        return (Injector) context.getApplicationContext();
    }

    public static <T> void inject(@NonNull Context context, @NonNull T target) {
        getInjector(context).inject(target);
    }

    public static <T extends Context> void inject(@NonNull T target) {
        inject(target, target);
    }
}
