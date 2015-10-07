package is.hello.buruberi.bluetooth.stacks.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface LoggerFacade {
    void error(@NonNull String tag, @Nullable String message, @Nullable Throwable e);
    void warn(@NonNull String tag, @Nullable String message, @Nullable Throwable e);
    void warn(@NonNull String tag, @Nullable String message);
    void info(@NonNull String tag, @Nullable String message);
    void debug(@NonNull String tag, @Nullable String message);
}
