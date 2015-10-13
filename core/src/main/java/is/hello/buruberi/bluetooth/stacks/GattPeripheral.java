package is.hello.buruberi.bluetooth.stacks;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import is.hello.buruberi.bluetooth.errors.ConnectionStateException;
import is.hello.buruberi.bluetooth.errors.ServiceDiscoveryException;
import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;
import is.hello.buruberi.util.NonGuaranteed;
import rx.Observable;

/**
 * Represents a Bluetooth Low Energy device that communicates over a gatt profile.
 * <p>
 * All Observable objects returned by a GattPeripheral must be subscribed
 * to before they will perform their work. No guarantees are made about
 * what scheduler the Observables will do, and yield their work on.
 */
public interface GattPeripheral {
    /**
     * The logging tag that should be used by implementations of the GattPeripheral interface.
     */
    String LOG_TAG = "Bluetooth." + GattPeripheral.class.getSimpleName();


    //region Local Broadcasts

    /**
     * A local broadcast that informs interested listeners that a GattPeripheral has disconnected.
     *
     * @see #EXTRA_NAME
     * @see #EXTRA_ADDRESS
     */
    String ACTION_DISCONNECTED = GattPeripheral.class.getName() + ".ACTION_DISCONNECTED";

    /**
     * The name of the affected GattPeripheral.
     *
     * @see #ACTION_DISCONNECTED
     */
    String EXTRA_NAME = GattPeripheral.class.getName() + ".EXTRA_NAME";

    /**
     * The address of the affected GattPeripheral.
     *
     * @see #ACTION_DISCONNECTED
     */
    String EXTRA_ADDRESS = GattPeripheral.class.getName() + ".EXTRA_ADDRESS";

    //endregion


    //region Bond Status

    /**
     * Indicates that no bond exists.
     */
    int BOND_NONE = BluetoothDevice.BOND_NONE;

    /**
     * Indicates the bond is in the process of changing.
     */
    int BOND_CHANGING = BluetoothDevice.BOND_BONDING;

    /**
     * Indicates the peripheral is bonded.
     */
    int BOND_BONDED = BluetoothDevice.BOND_BONDED;

    @IntDef({BOND_NONE, BOND_CHANGING, BOND_BONDED})
    @Retention(RetentionPolicy.SOURCE)
    @interface BondStatus {}

    //endregion


    //region Connection Status

    /**
     * Indicates the GattPeripheral is not connected.
     */
    int STATUS_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;

    /**
     * Indicates the GattPeripheral is in the process of connecting.
     */
    int STATUS_CONNECTING = BluetoothProfile.STATE_CONNECTING;

    /**
     * Indicates the GattPeripheral is connected.
     */
    int STATUS_CONNECTED = BluetoothProfile.STATE_CONNECTED;

    /**
     * Indicates the GattPeripheral is in the process of disconnecting.
     */
    int STATUS_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;

    @IntDef({STATUS_DISCONNECTED, STATUS_DISCONNECTING, STATUS_CONNECTED, STATUS_CONNECTING})
    @Retention(RetentionPolicy.SOURCE)
    @interface ConnectivityStatus {}

    //endregion


    //region Properties

    /**
     * Returns the received signal strength of the GattPeripheral
     * when it was discovered by the {@see BluetoothStack}.
     * <p>
     * This value does not update.
     */
    int getScanTimeRssi();

    /**
     * Returns the address of the GattPeripheral.
     * <p>
     * This value should be included in the implementation's toString method.
     */
    String getAddress();

    /**
     * Returns the name of the GattPeripheral.
     * <p>
     * This value should be included in the implementation's toString method.
     */
    String getName();

    /**
     * Returns the advertising data associated with the GattPeripheral.
     */
    @NonNull AdvertisingData getAdvertisingData();

    /**
     * Returns the stack this GattPeripheral is tied to.
     * <p>
     * @see is.hello.buruberi.bluetooth.stacks.BluetoothStack#newConfiguredObservable(Observable.OnSubscribe)
     */
    BluetoothStack getStack();

    //endregion


    //region Timeouts

    /**
     * Returns a new operation timeout for use with the GattPeripheral.
     */
    @NonNull OperationTimeout createOperationTimeout(@NonNull String name, long duration, @NonNull TimeUnit timeUnit);

    //endregion


    //region Connectivity

    /**
     * Attempts to create a gatt connection to the peripheral.
     * <p>
     * Does nothing if there is already an active connection.
     * <p>
     * Yields an {@link ConnectionStateException} if called
     * when peripheral connection status is changing.
     * @param timeout   The timeout to apply to the connect operation. Will only fire on certain phones.
     * <p>
     * <em>Important:</em> The order in which you connect and create bonds depends
     * on the device's Android version. {@link #createBond()} for more info.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @NonNull Observable<GattPeripheral> connect(@NonNull OperationTimeout timeout);

    /**
     * Ends the gatt connection of the peripheral.
     * <p>
     * Safe to call multiple times if the peripheral is disconnected,
     * or in the process of disconnecting.
     * <p>
     * Yields a {@link ConnectionStateException}
     * if the peripheral is currently connecting.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @NonNull Observable<GattPeripheral> disconnect();

    /**
     * Returns the connection status of the Peripheral.
     *
     * @see GattPeripheral#STATUS_DISCONNECTED
     * @see GattPeripheral#STATUS_CONNECTING
     * @see GattPeripheral#STATUS_CONNECTED
     * @see GattPeripheral#STATUS_DISCONNECTING
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @ConnectivityStatus int getConnectionStatus();

    //endregion


    //region Bonding

    /**
     * Creates a bond to the peripheral from the current device.
     * <p>
     * Does nothing if the device is already bonded.
     * <p>
     * <em>Important:</em> the behavior of this method varies depending
     * on the device's Android version. In API levels 18 and 19 (JB and KitKat),
     * this method can only be called if the GattPeripheral is currently connected.
     * <p>
     * However, starting in API level 21 (Lollipop), this method will <em>fail</em>
     * if you call it when the device is connected. If you need a bond, you should
     * call this method before you call {@link #connect(OperationTimeout)}.
     */
    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    })
    @NonNull Observable<GattPeripheral> createBond();

    /**
     * Removes any bond to the peripheral from the current device.
     * Does nothing if the device is not bonded.
     * <p>
     * <em>Note:</em> this method does not exhibit the same inconsistent behavior
     * that {@link #createBond()} does between device Android versions.
     * @param timeout   The timeout to apply to the operation.
     *
     * @see NonGuaranteed The current implementation of this method depends on the existence
     *                    of a private method on {@code BluetoothDevice}, and may stop functioning
     *                    in a future release of Android.
     */
    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    })
    @NonGuaranteed
    @NonNull Observable<GattPeripheral> removeBond(@NonNull OperationTimeout timeout);

    /**
     * Returns the bond status of the peripheral.
     *
     * @see GattPeripheral#BOND_NONE
     * @see GattPeripheral#BOND_CHANGING
     * @see GattPeripheral#BOND_BONDED
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @BondStatus int getBondStatus();

    //endregion


    //region Discovering Services

    /**
     * Performs service discovery on the peripheral.
     * <p>
     * Yields a {@link ConnectionStateException}
     * if the peripheral is not connected when this method is called.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @NonNull Observable<Map<UUID, PeripheralService>> discoverServices(@NonNull OperationTimeout timeout);

    /**
     * Performs service discovery on the peripheral,
     * yielding the service matching a given identifier.
     * <p>
     * If the service cannot be found, this method will yield
     * a {@link ServiceDiscoveryException}.
     * <p>
     * Yields a {@link ConnectionStateException}
     * if the peripheral is not connected when this method is called.
     *
     * @see #discoverServices(OperationTimeout)
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @NonNull Observable<PeripheralService> discoverService(@NonNull UUID serviceIdentifier,
                                                           @NonNull OperationTimeout timeout);

    //endregion


    //region Characteristics

    /**
     * @deprecated Prefer {@link GattCharacteristic#enableNotification(UUID, OperationTimeout)}
     *             for all new code.
     */
    @Deprecated
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @NonNull Observable<UUID> enableNotification(@NonNull PeripheralService onPeripheralService,
                                                 @NonNull UUID characteristicIdentifier,
                                                 @NonNull UUID descriptorIdentifier,
                                                 @NonNull OperationTimeout timeout);

    /**
     * @deprecated Prefer {@link GattCharacteristic#disableNotification(UUID, OperationTimeout)}
     *             for all new code.
     */
    @Deprecated
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @NonNull Observable<UUID> disableNotification(@NonNull PeripheralService onPeripheralService,
                                                  @NonNull UUID characteristicIdentifier,
                                                  @NonNull UUID descriptorIdentifier,
                                                  @NonNull OperationTimeout timeout);

    /**
     * Writes a given payload to a characteristic belonging to a service on the peripheral.
     * <p>
     * <em>Important:</em> it appears on some devices that writes are asynchronous on multiple
     * levels. Although the stack may report the write was successful, it doesn't mean the
     * write has actually completed. If you're going to disconnect after a write operation,
     * it's a good idea to add a delay of a few seconds.
     * <p>
     * The value of {@literal writeType} is supposed to be automatically inferred during service
     * discovery, but some devices do not consistently populate the value (Verizon Note 4).
     * As such, this value must be provided for every write command call.
     *
     * @param service           The service to write to.
     * @param characteristic    The characteristic to write to.
     * @param writeType         The type of write to perform.
     * @param payload           The payload to write. Must be 20 <code>bytes</code> or less.
     * @param timeout           The timeout to wrap the operation within.
     * @return An observable that will emit a single null value, then complete upon success.
     *
     * @deprecated Prefer {@link GattCharacteristic#write(WriteType, byte[], OperationTimeout)}
     *             for all new code.
     */
    @Deprecated
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @NonNull Observable<Void> writeCommand(@NonNull PeripheralService service,
                                           @NonNull UUID characteristic,
                                           @NonNull WriteType writeType,
                                           @NonNull byte[] payload,
                                           @NonNull OperationTimeout timeout);

    /**
     * Associates a given packet handler with the Peripheral.
     * <p>
     * Characteristic read and change data will be sent to the
     * packet handler for processing, and outward propagation.
     * <p>
     * While it is not required that you provide a packet handler,
     * it is strongly recommended.
     */
    void setPacketHandler(@Nullable PacketHandler dataHandler);

    //endregion


    /**
     * Determines how a command will be written to a peripheral.
     */
    enum WriteType {
        /**
         * Write characteristic, requesting acknowledgement by the remote device.
         */
        DEFAULT(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT),

        /**
         * Write characteristic without requiring a response by the remote device.
         */
        NO_RESPONSE(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE),

        /**
         * Write characteristic including authentication signature.
         */
        SIGNED(BluetoothGattCharacteristic.WRITE_TYPE_SIGNED);

        /**
         * The underlying enumeration value used by the Android stack.
         */
        public final int value;

        WriteType(int value) {
            this.value = value;
        }
    }


    /**
     * Responsible for encoding and decoding packets for the Bluetooth stack.
     */
    interface PacketHandler {
        /**
         * The maximum length of a Bluetooth Low Energy packet.
         */
        int PACKET_LENGTH = 20;

        /**
         * Attempt to process an incoming packet from a characteristic.
         */
        boolean processIncomingPacket(@NonNull UUID characteristicIdentifier,
                                      @NonNull byte[] payload);

        /**
         * Informs the packet handler that the Bluetooth transport has disconnected.
         */
        void transportDisconnected();
    }
}
