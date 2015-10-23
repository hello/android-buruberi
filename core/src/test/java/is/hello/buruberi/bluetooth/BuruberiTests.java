package is.hello.buruberi.bluetooth;

import android.Manifest;
import android.content.Context;
import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import is.hello.buruberi.BuildConfig;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
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

    @Test
    @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    public void hasPermissions() {
        ShadowApplication.getInstance().grantPermissions(Manifest.permission.BLUETOOTH,
                                                         Manifest.permission.BLUETOOTH_ADMIN);

        final Buruberi allowedBuilder = new Buruberi().setApplicationContext(getContext());
        assertThat(allowedBuilder.hasPermissions(), is(true));

        ShadowApplication.getInstance().denyPermissions(Manifest.permission.BLUETOOTH,
                                                        Manifest.permission.BLUETOOTH_ADMIN);

        final Buruberi deniedBuilder = new Buruberi().setApplicationContext(getContext());
        assertThat(deniedBuilder.hasPermissions(), is(false));
    }
}
