package is.hello.buruberi.testing;

import android.support.annotation.NonNull;

import junit.framework.Assert;

/**
 * Extensions to JUnit's Assert class. All methods are intended to be statically imported.
 */
public class AssertExtensions {
    /**
     * Checks that a given {@see ThrowingRunnable} throws an exception, calling fail if it does not.
     * @param runnable  The runnable that should throw an exception. Required.
     */
    public static void assertThrows(@NonNull ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable e) {
            return;
        }

        Assert.fail("expected exception, got none");
    }

    /**
     * Checks that a given {@see ThrowingRunnable} doesn't throw an exception, calling fail if one is thrown.
     * @param runnable  The runnable that should not throw an exception. Required.
     */
    public static void assertNoThrow(@NonNull ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            Assert.fail("Unexpected exception '" + e + "' thrown");
        }
    }

    /**
     * Variant of the Runnable interface that throws a generic checked exception.
     */
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
