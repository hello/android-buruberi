package is.hello.buruberi.example.util;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
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
}
