package is.hello.buruberi.bluetooth.stacks;

import android.bluetooth.BluetoothGattService;

import java.util.UUID;

public interface PeripheralService {
    int SERVICE_TYPE_PRIMARY = BluetoothGattService.SERVICE_TYPE_PRIMARY;
    int SERVICE_TYPE_SECONDARY = BluetoothGattService.SERVICE_TYPE_SECONDARY;

    UUID getUuid();
    int getType();
}
