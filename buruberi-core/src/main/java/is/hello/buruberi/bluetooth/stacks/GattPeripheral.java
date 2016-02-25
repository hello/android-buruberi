/*
 * Copyright 2015 Hello Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package is.hello.buruberi.bluetooth.stacks;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.support.annotation.CheckResult;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import is.hello.buruberi.bluetooth.errors.ConnectionStateException;
import is.hello.buruberi.bluetooth.errors.ServiceDiscoveryException;
import is.hello.buruberi.bluetooth.stacks.android.BluetoothDeviceCompat;
import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;
import is.hello.buruberi.util.NonGuaranteed;
import rx.Observable;

/**
 * Represents a Bluetooth Low Energy device that communicates over a gatt profile.
 * <p>
 * All Observable objects returned by a GattPeripheral must be subscribed
 * to before they will perform their work. No guarantees are made about
 * what scheduler the Observables will do, and yield their work on.
 * <p>
 * {@code GattPeripheral} objects are ordered based on the strength of their RSSI at scan time.
 * The {@code GattPeripheral} with the strongest signal in a collection will be placed last.
 */
public interface GattPeripheral extends Comparable<GattPeripheral> {
    /**
     * The log tag used by implementations of the GattPeripheral interface.
     */
    String LOG_TAG = "Bluetooth." + GattPeripheral.class.getSimpleName();


    //region Local Broadcasts

    /**
     * A local broadcast that informs interested listeners
     * that a {@code GattPeripheral} has disconnected.
     *
     * @see #EXTRA_NAME
     * @see #EXTRA_ADDRESS
     */
    String ACTION_DISCONNECTED = GattPeripheral.class.getName() + ".ACTION_DISCONNECTED";

    /**
     * The name of the affected {@code GattPeripheral}.
     *
     * @see #ACTION_DISCONNECTED
     */
    String EXTRA_NAME = GattPeripheral.class.getName() + ".EXTRA_NAME";

    /**
     * The address of the affected {@code GattPeripheral}.
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

    /**
     * Marks an {@code int} as containing one of the
     * bond status constants from {@code GattPeripheral}.
     *
     * @see #BOND_NONE
     * @see #BOND_CHANGING
     * @see #BOND_BONDED
     */
    @Target({
            ElementType.FIELD,
            ElementType.PARAMETER,
            ElementType.METHOD,
            ElementType.LOCAL_VARIABLE
    })
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @IntDef({BOND_NONE, BOND_CHANGING, BOND_BONDED})
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

    /**
     * Marks an {@code int} as containing one of the connectivity
     * status constants from {@code GattPeripheral}.
     *
     * @see #STATUS_DISCONNECTED
     * @see #STATUS_CONNECTING
     * @see #STATUS_CONNECTED
     * @see #STATUS_DISCONNECTING
     */
    @Target({
            ElementType.FIELD,
            ElementType.PARAMETER,
            ElementType.METHOD,
            ElementType.LOCAL_VARIABLE
    })
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @IntDef({STATUS_DISCONNECTED, STATUS_DISCONNECTING, STATUS_CONNECTED, STATUS_CONNECTING})
    @interface ConnectivityStatus {}

    //endregion


    //region Connect Flags

    /**
     * If set, connect will wait for the remote device to become
     * available before issuing the connection attempt.
     * <p>
     * Equivalent to passing {@code autoConnect: true} with
     * {@code BluetoothGatt#(Context, boolean, BluetoothGattCallback)}.
     */
    int CONNECT_FLAG_WAIT_AVAILABLE = (1 << 1);

    /**
     * Corresponds to {@link BluetoothDeviceCompat#TRANSPORT_AUTO}.
     * It is undefined behavior to include more than one transport flag.
     * <p>
     * Only applies to devices on API level 23 (Marshmallow) or later.
     */
    int CONNECT_FLAG_TRANSPORT_AUTO = (1 << 2);

    /**
     * Corresponds to {@link BluetoothDeviceCompat#TRANSPORT_BREDR}.
     * It is undefined behavior to include more than one transport flag.
     * <p>
     * Only applies to devices on API level 23 (Marshmallow) or later.
     */
    int CONNECT_FLAG_TRANSPORT_BREDR = (1 << 3);

    /**
     * Corresponds to {@link BluetoothDeviceCompat#TRANSPORT_LE}.
     * It is undefined behavior to include more than one transport flag.
     * <p>
     * Only applies to devices on API level 23 (Marshmallow) or later.
     */
    int CONNECT_FLAG_TRANSPORT_LE = (1 << 4);

    /**
     * The recommended default flags to use with gatt connections.
     */
    int CONNECT_FLAG_DEFAULTS = (CONNECT_FLAG_WAIT_AVAILABLE | CONNECT_FLAG_TRANSPORT_LE);

    /**
     * Marks an {@code int} as containing one of the
     * connect flag constants from {@code GattPeripheral}.
     *
     * @see #CONNECT_FLAG_WAIT_AVAILABLE
     * @see #CONNECT_FLAG_TRANSPORT_AUTO
     * @see #CONNECT_FLAG_TRANSPORT_BREDR
     * @see #CONNECT_FLAG_TRANSPORT_LE
     * @see #CONNECT_FLAG_DEFAULTS
     */
    @Target({
            ElementType.FIELD,
            ElementType.PARAMETER,
            ElementType.METHOD,
            ElementType.LOCAL_VARIABLE
    })
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @IntDef(flag = true, value = {
            CONNECT_FLAG_WAIT_AVAILABLE,
            CONNECT_FLAG_TRANSPORT_AUTO,
            CONNECT_FLAG_TRANSPORT_BREDR,
            CONNECT_FLAG_TRANSPORT_LE,
            CONNECT_FLAG_DEFAULTS,
    })
    @interface ConnectFlags {}

    //endregion


    //region Properties

    /**
     * Returns the RSSI value from when this
     * {@code GattPeripheral} was discovered.
     * <p>
     * This value does not update.
     */
    int getScanTimeRssi();

    /**
     * Returns the address of the GattPeripheral.
     */
    String getAddress();

    /**
     * Returns the name of the GattPeripheral.
     */
    String getName();

    /**
     * Returns the advertising data that was scanned when
     * this {@code GattPeripheral} was discovered.
     */
    @NonNull AdvertisingData getAdvertisingData();

    /**
     * Returns the stack this {@code GattPeripheral} is tied to.
     * <p>
     * @see BluetoothStack#newConfiguredObservable(Observable.OnSubscribe)
     */
    @CheckResult
    @NonNull BluetoothStack getStack();

    //endregion


    //region Timeouts

    /**
     * Returns a new {@link OperationTimeout} for use with the {@code GattPeripheral}.
     *
     * @param name The name to use when printing out status updates for the timeout.
     * @param duration The amount of time to elapse before the timeout expires.
     * @param timeUnit The time unit of the {@code duration}.
     * @return A new {@link OperationTimeout}
     */
    @CheckResult
    @NonNull OperationTimeout createOperationTimeout(@NonNull String name,
                                                     long duration,
                                                     @NonNull TimeUnit timeUnit);

    //endregion


    //region Connectivity

    /**
     * Attempts to create a gatt connection to the peripheral.
     * <p>
     * Does nothing if there is already an active connection.
     * <p>
     * Yields an {@link ConnectionStateException} if called
     * when peripheral connection status is changing.
     * <p>
     * <em>Important:</em> The order in which you connect and create bonds depends
     * on the device's Android version. {@link #createBond()} for more info.
     *
     * @param timeout   The timeout to apply to the connect operation.
     * @param flags     The configuration of the connect operation.
     * @return An observable that represents the connect operation.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @CheckResult
    @NonNull Observable<GattPeripheral> connect(@ConnectFlags int flags,
                                                @NonNull OperationTimeout timeout);

    /**
     * Ends the gatt connection of the peripheral.
     * <p>
     * Does nothing if the peripheral is already disconnected.
     * <p>
     * Yields a {@link ConnectionStateException}
     * if the peripheral is currently connecting.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @CheckResult
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
    @CheckResult
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
     * call this method before you call {@link #connect(int, OperationTimeout)}.
     */
    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    })
    @CheckResult
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
     *                    in a future release of Android. Tested through API level 23.
     */
    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    })
    @NonGuaranteed
    @CheckResult
    @NonNull Observable<GattPeripheral> removeBond(@NonNull OperationTimeout timeout);

    /**
     * Returns the bond status of the peripheral.
     *
     * @see GattPeripheral#BOND_NONE
     * @see GattPeripheral#BOND_CHANGING
     * @see GattPeripheral#BOND_BONDED
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @CheckResult
    @BondStatus int getBondStatus();

    //endregion


    //region Discovering Services

    /**
     * Performs remote service discovery on the peripheral.
     * <p>
     * Each call to this method will result in separate discovery operation
     * on the remote peripheral, it is not idempotent. The result of
     * the {@code Observable} this method returns should be saved
     * for the duration of your current peripheral connection.
     * <p>
     * Yields a {@link ConnectionStateException} if the
     * peripheral is not connected when this method is called.
     *
     * @see #discoverService(UUID, OperationTimeout) if you need a specific service.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @CheckResult
    @NonNull Observable<Map<UUID, ? extends GattService>> discoverServices(@NonNull OperationTimeout timeout);

    /**
     * Performs service discovery on the peripheral,
     * yielding the service matching a given identifier.
     * <p>
     * Each call to this method will result in separate discovery operation
     * on the remote peripheral, it is not idempotent. Client code should not
     * use this method if it needs multiple services from the peripheral. The
     * result of the {@code Observable} this method returns should be saved
     * for the duration of your current peripheral connection.
     * <p>
     * If the service cannot be found, this method will yield
     * a {@link ServiceDiscoveryException}.
     * <p>
     * Yields a {@link ConnectionStateException}
     * if the peripheral is not connected when this method is called.
     *
     * @see #discoverServices(OperationTimeout) if you need multiple services.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @CheckResult
    @NonNull Observable<GattService> discoverService(@NonNull UUID serviceIdentifier,
                                                     @NonNull OperationTimeout timeout);

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
}
