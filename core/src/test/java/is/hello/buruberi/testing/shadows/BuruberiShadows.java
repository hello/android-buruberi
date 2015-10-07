package is.hello.buruberi.testing.shadows;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.support.annotation.NonNull;

import org.robolectric.internal.ShadowExtractor;

/**
 * Convenience methods for extracting shadows specific to the Buruberi project.
 * <p>
 * Project local copy of {@link org.robolectric.Shadows}.
 */
public class BuruberiShadows {
    public static ShadowBluetoothManager shadowOf(@NonNull BluetoothManager bluetoothManager) {
        return (ShadowBluetoothManager) ShadowExtractor.extract(bluetoothManager);
    }

    public static ShadowBluetoothDeviceExt shadowOf(@NonNull BluetoothDevice bluetoothDevice) {
        return (ShadowBluetoothDeviceExt) ShadowExtractor.extract(bluetoothDevice);
    }

    public static ShadowBluetoothGatt shadowOf(@NonNull BluetoothGatt bluetoothGatt) {
        return (ShadowBluetoothGatt) ShadowExtractor.extract(bluetoothGatt);
    }

    public static ShadowBluetoothAdapterExt shadowOf(@NonNull BluetoothAdapter bluetoothAdapter) {
        return (ShadowBluetoothAdapterExt) ShadowExtractor.extract(bluetoothAdapter);
    }

    public static ShadowBluetoothLeScanner shadowOf(@NonNull BluetoothLeScanner bluetoothAdapter) {
        return (ShadowBluetoothLeScanner) ShadowExtractor.extract(bluetoothAdapter);
    }
}
