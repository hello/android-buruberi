package is.hello.buruberi.bluetooth;

import android.content.Context;
import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import is.hello.buruberi.BuildConfig;

import static org.junit.Assert.fail;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class BuruberiTests {
    protected Context getContext() {
        return RuntimeEnvironment.application.getApplicationContext();
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.JELLY_BEAN)
    public void doesNotCrashOnOldVersions() {
        try {
            new Buruberi()
                    .setApplicationContext(getContext())
                    .build();
        } catch (Throwable e) {
            fail();
        }
    }
}
