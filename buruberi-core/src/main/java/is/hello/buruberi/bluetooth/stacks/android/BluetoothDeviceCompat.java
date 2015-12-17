package is.hello.buruberi.bluetooth.stacks.android;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public final class BluetoothDeviceCompat {
    public static final int TRANSPORT_AUTO = 0;
    public static final int TRANSPORT_BREDR = 1;
    public static final int TRANSPORT_LE = 2;

    @Nullable
    public static BluetoothGatt connectGatt(@NonNull BluetoothDevice device,
                                            @NonNull Context context,
                                            boolean autoConnect,
                                            @NonNull BluetoothGattCallback callback,
                                            int transport) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return device.connectGatt(context, autoConnect, callback, transport);
        } else {
            return device.connectGatt(context, autoConnect, callback);
        }
    }

    private BluetoothDeviceCompat() {
    }
}
