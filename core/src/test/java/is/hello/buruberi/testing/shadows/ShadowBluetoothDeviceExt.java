package is.hello.buruberi.testing.shadows;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowBluetoothDevice;
import org.robolectric.util.ReflectionHelpers;

import static is.hello.buruberi.testing.shadows.BuruberiShadows.shadowOf;

@SuppressWarnings("unused")
@Implements(
        value = BluetoothDevice.class,
        inheritImplementationMethods = true
)
public class ShadowBluetoothDeviceExt extends ShadowBluetoothDevice {
    private String address;
    private int bondState = BluetoothDevice.BOND_NONE;


    //region Attributes

    @Implementation
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    @Implementation
    public int hashCode() {
        return address.hashCode();
    }

    //endregion


    //region Bonding

    @Implementation
    public int getBondState() {
        return bondState;
    }

    public void setBondState(int bondState) {
        this.bondState = bondState;
    }

    @Implementation
    public boolean createBond() {
        setBondState(BluetoothDevice.BOND_BONDED);
        return true;
    }

    @Implementation
    public boolean cancelBondProcess() {
        setBondState(BluetoothDevice.BOND_NONE);
        return true;
    }

    @Implementation
    public boolean removeBond() {
        setBondState(BluetoothDevice.BOND_NONE);
        return true;
    }

    //endregion


    //region Connections

    @Implementation
    public BluetoothGatt connectGatt(Context context, boolean autoConnect,
                                     BluetoothGattCallback callback) {
        BluetoothGatt bluetoothGatt = ReflectionHelpers.newInstance(BluetoothGatt.class);

        ShadowBluetoothGatt shadow = shadowOf(bluetoothGatt);
        shadow.setAutoConnect(autoConnect);
        shadow.setGattCallback(callback);

        return bluetoothGatt;
    }

    //endregion
}
