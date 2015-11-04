package is.hello.buruberi.example.util;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import is.hello.buruberi.bluetooth.stacks.GattCharacteristic;

public class GattCharacteristics {
    private static boolean isFlagSet(int field, int flag) {
        return ((field & flag) == flag);
    }

    public static String getPermissionString(int permissions) {
        final List<String> permissionStrings = new ArrayList<>();
        if (isFlagSet(permissions, GattCharacteristic.PROPERTY_BROADCAST)) {
            permissionStrings.add("PROPERTY_BROADCAST");
        }
        if (isFlagSet(permissions, GattCharacteristic.PROPERTY_READ)) {
            permissionStrings.add("PROPERTY_READ");
        }
        if (isFlagSet(permissions, GattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) {
            permissionStrings.add("PROPERTY_WRITE_NO_RESPONSE");
        }
        if (isFlagSet(permissions, GattCharacteristic.PROPERTY_WRITE)) {
            permissionStrings.add("PROPERTY_WRITE");
        }
        if (isFlagSet(permissions, GattCharacteristic.PROPERTY_NOTIFY)) {
            permissionStrings.add("PROPERTY_NOTIFY");
        }
        if (isFlagSet(permissions, GattCharacteristic.PROPERTY_INDICATE)) {
            permissionStrings.add("PROPERTY_INDICATE");
        }
        if (isFlagSet(permissions, GattCharacteristic.PROPERTY_SIGNED_WRITE)) {
            permissionStrings.add("PROPERTY_SIGNED_WRITE");
        }
        if (isFlagSet(permissions, GattCharacteristic.PROPERTY_EXTENDED_PROPS)) {
            permissionStrings.add("PROPERTY_EXTENDED_PROPS");
        }
        return TextUtils.join(", ", permissionStrings);
    }

    public static String getPropertiesString(int properties) {
        final List<String> propertyStrings = new ArrayList<>();
        if (isFlagSet(properties, GattCharacteristic.PERMISSION_READ)) {
            propertyStrings.add("PERMISSION_READ");
        }
        if (isFlagSet(properties, GattCharacteristic.PERMISSION_READ_ENCRYPTED)) {
            propertyStrings.add("PERMISSION_READ_ENCRYPTED");
        }
        if (isFlagSet(properties, GattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM)) {
            propertyStrings.add("PERMISSION_READ_ENCRYPTED_MITM");
        }
        if (isFlagSet(properties, GattCharacteristic.PERMISSION_WRITE)) {
            propertyStrings.add("PERMISSION_WRITE");
        }
        if (isFlagSet(properties, GattCharacteristic.PERMISSION_WRITE_ENCRYPTED)) {
            propertyStrings.add("PERMISSION_WRITE_ENCRYPTED");
        }
        if (isFlagSet(properties, GattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM)) {
            propertyStrings.add("PERMISSION_WRITE_ENCRYPTED_MITM");
        }
        if (isFlagSet(properties, GattCharacteristic.PERMISSION_WRITE_SIGNED)) {
            propertyStrings.add("PERMISSION_WRITE_SIGNED");
        }
        if (isFlagSet(properties, GattCharacteristic.PERMISSION_WRITE_SIGNED_MITM)) {
            propertyStrings.add("PERMISSION_WRITE_SIGNED_MITM");
        }
        return TextUtils.join(", ", propertyStrings);
    }
}
