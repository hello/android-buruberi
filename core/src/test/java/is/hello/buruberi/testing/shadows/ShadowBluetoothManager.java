package is.hello.buruberi.testing.shadows;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.support.annotation.Nullable;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.Map;
import java.util.WeakHashMap;

@SuppressWarnings("unused")
@Implements(BluetoothManager.class)
public class ShadowBluetoothManager {
    private final Map<BluetoothDevice, Integer> connectionStates = new WeakHashMap<>();
    private @Nullable BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

    public void clearConnectionStates() {
        connectionStates.clear();
    }

    public void setConnectionState(BluetoothDevice device, int profile) {
        connectionStates.put(device, profile);
    }

    @Implementation
    public int getConnectionState(BluetoothDevice device, int profile) {
        Integer connectionState = connectionStates.get(device);
        if (connectionState != null) {
            return connectionState;
        } else {
            return BluetoothProfile.STATE_DISCONNECTED;
        }
    }

    public void setAdapter(@Nullable BluetoothAdapter adapter) {
        this.adapter = adapter;
    }

    @Implementation
    public @Nullable BluetoothAdapter getAdapter() {
        return adapter;
    }
}
