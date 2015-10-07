package is.hello.buruberi.testing.shadows;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.os.Build;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowBluetoothAdapter;
import org.robolectric.util.ReflectionHelpers;

@SuppressWarnings("unused")
@Implements(
        value = BluetoothAdapter.class,
        inheritImplementationMethods = true
)
public class ShadowBluetoothAdapterExt extends ShadowBluetoothAdapter {
    private BluetoothLeScanner bluetoothLeScanner;

    @Implementation
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BluetoothLeScanner getBluetoothLeScanner() {
        if (bluetoothLeScanner == null) {
            this.bluetoothLeScanner = ReflectionHelpers.newInstance(BluetoothLeScanner.class);
        }

        return bluetoothLeScanner;
    }
}
