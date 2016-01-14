package is.hello.buruberi.bluetooth.stacks.android;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import is.hello.buruberi.bluetooth.stacks.GattCharacteristic;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.GattService;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;
import rx.Observable;

class NativeGattCharacteristic implements GattCharacteristic {
    private final BluetoothGattCharacteristic nativeCharacteristic;
    private final NativeGattService service;
    private final NativeGattPeripheral peripheral;

    NativeGattCharacteristic(@NonNull BluetoothGattCharacteristic nativeCharacteristic,
                             @NonNull NativeGattService service,
                             @NonNull NativeGattPeripheral peripheral) {
        this.nativeCharacteristic = nativeCharacteristic;
        this.service = service;
        this.peripheral = peripheral;
    }

    @Override
    public UUID getUuid() {
        return nativeCharacteristic.getUuid();
    }

    @Override
    @Properties
    public int getProperties() {
        final @Properties int properties = nativeCharacteristic.getProperties();
        return properties;
    }

    @Override
    @Permissions
    public int getPermissions() {
        final @Permissions int permissions = nativeCharacteristic.getPermissions();
        return permissions;
    }

    @NonNull
    @Override
    public GattService getService() {
        return service;
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

    @Permissions
    @Override
    public int getDescriptorPermissions(@NonNull UUID identifier) {
        final BluetoothGattDescriptor descriptor = nativeCharacteristic.getDescriptor(identifier);
        if (descriptor != null) {
            final @Permissions int permissions = descriptor.getPermissions();
            return permissions;
        } else {
            return PERMISSION_NULL;
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
