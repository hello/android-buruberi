package is.hello.buruberi.bluetooth.stacks.android;

import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import is.hello.buruberi.bluetooth.stacks.PeripheralService;

public final class NativePeripheralService implements PeripheralService {
    final @NonNull BluetoothGattService service;

    static @NonNull Map<UUID, PeripheralService> wrapGattServices(@NonNull List<BluetoothGattService> services) {
        Map<UUID, PeripheralService> peripheralServices = new HashMap<>();

        for (BluetoothGattService nativeService : services) {
            peripheralServices.put(nativeService.getUuid(), new NativePeripheralService(nativeService));
        }

        return peripheralServices;
    }

    NativePeripheralService(@NonNull BluetoothGattService service) {
        this.service = service;
    }


    @Override
    public UUID getUuid() {
        return service.getUuid();
    }

    @Override
    public int getType() {
        return service.getType();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NativePeripheralService that = (NativePeripheralService) o;

        return service.equals(that.service);

    }

    @Override
    public int hashCode() {
        return service.hashCode();
    }


    @Override
    public String toString() {
        return "AndroidPeripheralService{" +
                "service=" + service +
                '}';
    }
}
