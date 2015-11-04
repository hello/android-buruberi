package is.hello.buruberi.example.util;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;
import is.hello.buruberi.example.R;

public class GattPeripherals {
    public static CharSequence getDisplayName(@NonNull GattPeripheral peripheral,
                                              @NonNull Resources resources) {
        final String name = peripheral.getName();
        if (TextUtils.isEmpty(name)) {
            return resources.getText(R.string.peripheral_name_placeholder);
        } else {
            return name;
        }
    }

    public static CharSequence getDetails(@NonNull GattPeripheral peripheral,
                                          @NonNull Resources resources) {
        return resources.getString(R.string.peripheral_details_format,
                                   peripheral.getAddress(),
                                   getBondStatusString(peripheral.getBondStatus(), resources),
                                   peripheral.getScanTimeRssi());
    }

    public static String getBondStatusString(int status, @NonNull Resources resources) {
        switch (status) {
            case GattPeripheral.BOND_NONE: {
                return resources.getString(R.string.bond_status_none);
            }
            case GattPeripheral.BOND_CHANGING: {
                return resources.getString(R.string.bond_status_changing);
            }
            case GattPeripheral.BOND_BONDED: {
                return resources.getString(R.string.bond_status_bonded);
            }
            default: {
                return "";
            }
        }
    }

    public static @NonNull String getAdvertisingDataTypeString(int type) {
        switch (type) {
            case AdvertisingData.TYPE_FLAGS:
                return "Flags";

            case AdvertisingData.TYPE_INCOMPLETE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS:
                return "Incomplete 16 Bit Service Class UUIDs";

            case AdvertisingData.TYPE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS:
                return "16 Bit Service Class UUIDs";

            case AdvertisingData.TYPE_INCOMPLETE_LIST_OF_32_BIT_SERVICE_CLASS_UUIDS:
                return "Incomplete 32 Bit Service Class UUIDs";

            case AdvertisingData.TYPE_LIST_OF_32_BIT_SERVICE_CLASS_UUIDS:
                return "32 Bit Service Class UUIDs";

            case AdvertisingData.TYPE_INCOMPLETE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS:
                return "Incomplete 128 Bit Service Class UUIDs";

            case AdvertisingData.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS:
                return "128 Bit Service Class UUIDs";

            case AdvertisingData.TYPE_SHORTENED_LOCAL_NAME:
                return "Shortened Local Name";

            case AdvertisingData.TYPE_LOCAL_NAME:
                return "Local Name";

            case AdvertisingData.TYPE_TX_POWER_LEVEL:
                return "TX Power Level";

            case AdvertisingData.TYPE_CLASS_OF_DEVICE:
                return "Class of Device";

            case AdvertisingData.TYPE_SIMPLE_PAIRING_HASH_C:
                return "Simple Pairing Hash C";

            case AdvertisingData.TYPE_SIMPLE_PAIRING_RANDOMIZER_R:
                return "Simple Pairing Randomizer R";

            case AdvertisingData.TYPE_DEVICE_ID:
                return "Device ID";

            case AdvertisingData.TYPE_SECURITY_MANAGER_OUT_OF_BAND_FLAGS:
                return "Security Manager Out of Band Flags";

            case AdvertisingData.TYPE_SLAVE_CONNECTION_INTERVAL_RANGE:
                return "Slave Connection Interval Range";

            case AdvertisingData.TYPE_LIST_OF_16_BIT_SERVICE_SOLICITATION_UUIDS:
                return "16 Bit Service Solicitation UUIDs";

            case AdvertisingData.TYPE_LIST_OF_32_BIT_SERVICE_SOLICITATION_UUIDS:
                return "32 Bit Service Solicitation UUIDs";

            case AdvertisingData.TYPE_LIST_OF_128_BIT_SERVICE_SOLICITATION_UUIDS:
                return "128 Bit Service Solicitation UUIDs";

            case AdvertisingData.TYPE_SERVICE_DATA:
                return "Service Data";

            case AdvertisingData.TYPE_SERVICE_DATA_32_BIT_UUID:
                return "Service Data 32 Bit UUID";

            case AdvertisingData.TYPE_SERVICE_DATA_128_BIT_UUID:
                return "Service Data 128 Bit UUID";

            case AdvertisingData.TYPE_PUBLIC_TARGET_ADDRESS:
                return "Public Target Address";

            case AdvertisingData.TYPE_RANDOM_TARGET_ADDRESS:
                return "Random Target Address";

            case AdvertisingData.TYPE_APPEARANCE:
                return "Appearance";

            case AdvertisingData.TYPE_ADVERTISING_INTERVAL:
                return "Advertising Interval";

            case AdvertisingData.TYPE_MANUFACTURER_SPECIFIC_DATA:
                return "Manufacturer Specific Data";

            case AdvertisingData.TYPE_LE_BLUETOOTH_DEVICE_ADDRESS:
                return "Bluetooth LE Device Address";

            case AdvertisingData.TYPE_LE_ROLE:
                return "LE Role";

            case AdvertisingData.TYPE_SIMPLE_PAIRING_HASH_C_256:
                return "Simple Pairing Hash C 256";

            case AdvertisingData.TYPE_SIMPLE_PAIRING_RANDOMIZER_R_256:
                return "Simple Pairing Randomizer R 256";

            case AdvertisingData.TYPE_3D_INFORMATION_DATA:
                return "3D Information Data";

            default:
                return "Unknown (" + Integer.toHexString(type) + ")";
        }
    }
}
