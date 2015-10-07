package is.hello.buruberi.bluetooth.stacks.android;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import is.hello.buruberi.bluetooth.stacks.Characteristic;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;
import is.hello.buruberi.bluetooth.stacks.PeripheralService;
import rx.Observable;

public final class NativePeripheralService implements PeripheralService {
    final @NonNull BluetoothGattService service;
    final @NonNull NativeGattPeripheral peripheral;


    static @NonNull Map<UUID, PeripheralService> wrap(@NonNull List<BluetoothGattService> services,
                                                      @NonNull NativeGattPeripheral peripheral) {
        final Map<UUID, PeripheralService> peripheralServices = new HashMap<>();

        for (final BluetoothGattService nativeService : services) {
            peripheralServices.put(nativeService.getUuid(),
                                   new NativePeripheralService(nativeService, peripheral));
        }

        return peripheralServices;
    }

    NativePeripheralService(@NonNull BluetoothGattService service,
                            @NonNull NativeGattPeripheral peripheral) {
        this.service = service;
        this.peripheral = peripheral;
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
    public List<UUID> getCharacteristics() {
        final List<UUID> identifiers = new ArrayList<>();
        for (final BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
            identifiers.add(characteristic.getUuid());
        }
        return identifiers;
    }

    @Override
    public Characteristic getCharacteristic(@NonNull UUID identifier) {
        final BluetoothGattCharacteristic characteristic = service.getCharacteristic(identifier);
        if (characteristic != null) {
            return new NativeCharacteristic(characteristic);
        } else {
            return null;
        }
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


    class NativeCharacteristic implements Characteristic {
        private final BluetoothGattCharacteristic nativeCharacteristic;

        NativeCharacteristic(@NonNull BluetoothGattCharacteristic nativeCharacteristic) {
            this.nativeCharacteristic = nativeCharacteristic;
        }

        @Override
        public int getProperties() {
            return nativeCharacteristic.getProperties();
        }

        @Override
        public int getPermissions() {
            return nativeCharacteristic.getPermissions();
        }

        @NonNull
        @Override
        public PeripheralService getService() {
            return NativePeripheralService.this;
        }

        @NonNull
        @Override
        public List<UUID> getDescriptors() {
            final List<UUID> identifiers = new ArrayList<>();
            for (final BluetoothGattDescriptor descriptor : nativeCharacteristic.getDescriptors()) {
                identifiers.add(descriptor.getUuid());
            }
            return identifiers;
        }

        @Override
        public int getDescriptorPermissions(@NonNull UUID identifier) {
            final BluetoothGattDescriptor descriptor = nativeCharacteristic.getDescriptor(identifier);
            if (descriptor != null) {
                return descriptor.getPermissions();
            } else {
                return 0;
            }
        }

        @NonNull
        @Override
        public Observable<UUID> enableNotification(@NonNull UUID descriptor,
                                                   @NonNull OperationTimeout timeout) {
            // TODO: Re-implement this operation in terms of new API.

            return peripheral.enableNotification(getService(),
                                                 getUuid(),
                                                 descriptor,
                                                 timeout);
        }

        @NonNull
        @Override
        public Observable<UUID> disableNotification(@NonNull UUID descriptor,
                                                    @NonNull OperationTimeout timeout) {
            // TODO: Re-implement this operation in terms of new API.

            return peripheral.disableNotification(getService(),
                                                  getUuid(),
                                                  descriptor,
                                                  timeout);
        }

        @NonNull
        @Override
        public Observable<Void> write(@NonNull GattPeripheral.WriteType writeType,
                                      @NonNull byte[] payload,
                                      @NonNull OperationTimeout timeout) {
            // TODO: Re-implement this operation in terms of new API.

            return peripheral.writeCommand(getService(),
                                           getUuid(),
                                           writeType,
                                           payload,
                                           timeout);
        }

        @Override
        public String toString() {
            return "NativeCharacteristic{" +
                    "uuid=" + getUuid() +
                    '}';
        }
    }
}
