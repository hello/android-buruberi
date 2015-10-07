package is.hello.buruberi.bluetooth.stacks.util;

import rx.functions.Action1;

/**
 * Friendly name for an RxJava doOnError implementation.
 */
public interface ErrorListener extends Action1<Throwable> {
}
