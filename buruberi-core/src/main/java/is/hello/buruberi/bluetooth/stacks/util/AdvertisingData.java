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
package is.hello.buruberi.bluetooth.stacks.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import rx.functions.Func1;

/**
 * Parses a raw BLE advertising data blob into a multi-map collection for querying
 * by predicates contained in a {@link PeripheralCriteria} instance.
 */
public final class AdvertisingData {
    private final SparseArray<List<byte[]>> records = new SparseArray<>();

    //region Creation

    /**
     * Parses a given byte array into an advertising data object.
     */
    public static @NonNull AdvertisingData parse(@NonNull byte[] rawData) {
        final AdvertisingData parsedResponses = new AdvertisingData();
        int index = 0;
        while (index < rawData.length) {
            final byte dataLength = rawData[index++];
            if (dataLength == 0) {
                break;
            }

            final int dataType = rawData[index];
            if (dataType == 0) {
                break;
            }

            final byte[] payload = Arrays.copyOfRange(rawData, index + 1, index + dataLength);
            parsedResponses.addRecord(dataType, payload);

            index += dataLength;
        }
        return parsedResponses;
    }

    private AdvertisingData() {
    }

    private void addRecord(int type, @NonNull byte[] contents) {
        List<byte[]> typeItems = getRecordsForType(type);
        if (typeItems == null) {
            typeItems = new ArrayList<>(1);
            records.put(type, typeItems);
        }

        typeItems.add(contents);
    }

    //endregion


    //region Querying

    /**
     * Returns whether or not there are no advertising data records.
     */
    public boolean isEmpty() {
        return (records.size() == 0);
    }

    /**
     * Returns a sorted copy of the record types contained in the advertising data.
     */
    public List<Integer> copyRecordTypes() {
        final int count = records.size();
        final List<Integer> recordTypes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            recordTypes.add(records.keyAt(i));
        }
        Collections.sort(recordTypes);
        return recordTypes;
    }

    /**
     * Returns the records matching a given type.
     */
    public @Nullable List<byte[]> getRecordsForType(int type) {
        return records.get(type);
    }

    /**
     * Returns whether or not any records in the advertising
     * data of a given type match a given predicate functor.
     */
    public boolean anyRecordMatches(int type, @NonNull Func1<byte[], Boolean> predicate) {
        final Collection<byte[]> recordsForType = getRecordsForType(type);
        if (recordsForType == null) {
            return false;
        }

        for (byte[] payload : recordsForType) {
            if (predicate.call(payload)) {
                return true;
            }
        }

        return false;
    }

    //endregion


    //region Identity

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AdvertisingData that = (AdvertisingData) o;
        return records.equals(that.records);
    }

    @Override
    public int hashCode() {
        return records.hashCode();
    }

    @Override
    public String toString() {
        String string = "{";
        for (int i = 0, count = records.size(); i < count; i++) {
            string += typeToString(records.keyAt(i));
            string += "=[";
            final Iterator<byte[]> entryIterator = records.valueAt(i).iterator();
            while (entryIterator.hasNext()) {
                byte[] contents = entryIterator.next();
                string += Bytes.toString(contents);
                if (entryIterator.hasNext()) {
                    string += ", ";
                }
            }
            if (i < count - 1) {
                string += "], ";
            } else {
                string += "]";
            }
        }
        string += '}';
        return string;
    }

    //endregion


    // See https://www.bluetooth.org/en-us/specification/assigned-numbers/generic-access-profile
    public static final int TYPE_FLAGS = 0x01;
    public static final int TYPE_INCOMPLETE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS = 0x02;
    public static final int TYPE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS = 0x03;
    public static final int TYPE_INCOMPLETE_LIST_OF_32_BIT_SERVICE_CLASS_UUIDS = 0x04;
    public static final int TYPE_LIST_OF_32_BIT_SERVICE_CLASS_UUIDS = 0x05;
    public static final int TYPE_INCOMPLETE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS = 0x06;
    public static final int TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS = 0x07;
    public static final int TYPE_SHORTENED_LOCAL_NAME = 0x08;
    public static final int TYPE_LOCAL_NAME = 0x09;
    public static final int TYPE_TX_POWER_LEVEL = 0x0A;
    public static final int TYPE_CLASS_OF_DEVICE = 0x0D;
    public static final int TYPE_SIMPLE_PAIRING_HASH_C = 0x0E;
    public static final int TYPE_SIMPLE_PAIRING_RANDOMIZER_R = 0x0F;
    public static final int TYPE_DEVICE_ID = 0x10;
    public static final int TYPE_SECURITY_MANAGER_OUT_OF_BAND_FLAGS = 0x11;
    public static final int TYPE_SLAVE_CONNECTION_INTERVAL_RANGE = 0x12;
    public static final int TYPE_LIST_OF_16_BIT_SERVICE_SOLICITATION_UUIDS = 0x14;
    public static final int TYPE_LIST_OF_32_BIT_SERVICE_SOLICITATION_UUIDS = 0x1F;
    public static final int TYPE_LIST_OF_128_BIT_SERVICE_SOLICITATION_UUIDS = 0x15;
    public static final int TYPE_SERVICE_DATA = 0x16;
    public static final int TYPE_SERVICE_DATA_32_BIT_UUID = 0x20;
    public static final int TYPE_SERVICE_DATA_128_BIT_UUID = 0x21;
    public static final int TYPE_PUBLIC_TARGET_ADDRESS = 0x17;
    public static final int TYPE_RANDOM_TARGET_ADDRESS = 0x18;
    public static final int TYPE_APPEARANCE = 0x19;
    public static final int TYPE_ADVERTISING_INTERVAL = 0x1A;
    public static final int TYPE_LE_BLUETOOTH_DEVICE_ADDRESS = 0x1B;
    public static final int TYPE_LE_ROLE = 0x1C;
    public static final int TYPE_SIMPLE_PAIRING_HASH_C_256 = 0x1D;
    public static final int TYPE_SIMPLE_PAIRING_RANDOMIZER_R_256 = 0x1E;
    public static final int TYPE_3D_INFORMATION_DATA = 0x3D;
    public static final int TYPE_MANUFACTURER_SPECIFIC_DATA = 0xFF;

    public static @NonNull String typeToString(int type) {
        switch (type) {
            case TYPE_FLAGS:
                return "TYPE_FLAGS";
            case TYPE_INCOMPLETE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS:
                return "TYPE_INCOMPLETE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS";
            case TYPE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS:
                return "TYPE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS";
            case TYPE_INCOMPLETE_LIST_OF_32_BIT_SERVICE_CLASS_UUIDS:
                return "TYPE_INCOMPLETE_LIST_OF_32_BIT_SERVICE_CLASS_UUIDS";
            case TYPE_LIST_OF_32_BIT_SERVICE_CLASS_UUIDS:
                return "TYPE_LIST_OF_32_BIT_SERVICE_CLASS_UUIDS";
            case TYPE_INCOMPLETE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS:
                return "TYPE_INCOMPLETE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS";
            case TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS:
                return "TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS";
            case TYPE_SHORTENED_LOCAL_NAME:
                return "TYPE_SHORTENED_LOCAL_NAME";
            case TYPE_LOCAL_NAME:
                return "TYPE_LOCAL_NAME";
            case TYPE_TX_POWER_LEVEL:
                return "TYPE_TX_POWER_LEVEL";
            case TYPE_CLASS_OF_DEVICE:
                return "TYPE_CLASS_OF_DEVICE";
            case TYPE_SIMPLE_PAIRING_HASH_C:
                return "TYPE_SIMPLE_PAIRING_HASH_C";
            case TYPE_SIMPLE_PAIRING_RANDOMIZER_R:
                return "TYPE_SIMPLE_PAIRING_RANDOMIZER_R";
            case TYPE_DEVICE_ID:
                return "TYPE_DEVICE_ID";
            case TYPE_SECURITY_MANAGER_OUT_OF_BAND_FLAGS:
                return "TYPE_SECURITY_MANAGER_OUT_OF_BAND_FLAGS";
            case TYPE_SLAVE_CONNECTION_INTERVAL_RANGE:
                return "TYPE_SLAVE_CONNECTION_INTERVAL_RANGE";
            case TYPE_LIST_OF_16_BIT_SERVICE_SOLICITATION_UUIDS:
                return "TYPE_LIST_OF_16_BIT_SERVICE_SOLICITATION_UUIDS";
            case TYPE_LIST_OF_32_BIT_SERVICE_SOLICITATION_UUIDS:
                return "TYPE_LIST_OF_32_BIT_SERVICE_SOLICITATION_UUIDS";
            case TYPE_LIST_OF_128_BIT_SERVICE_SOLICITATION_UUIDS:
                return "TYPE_LIST_OF_128_BIT_SERVICE_SOLICITATION_UUIDS";
            case TYPE_SERVICE_DATA:
                return "TYPE_SERVICE_DATA";
            case TYPE_SERVICE_DATA_32_BIT_UUID:
                return "TYPE_SERVICE_DATA_32_BIT_UUID";
            case TYPE_SERVICE_DATA_128_BIT_UUID:
                return "TYPE_SERVICE_DATA_128_BIT_UUID";
            case TYPE_PUBLIC_TARGET_ADDRESS:
                return "TYPE_PUBLIC_TARGET_ADDRESS";
            case TYPE_RANDOM_TARGET_ADDRESS:
                return "TYPE_RANDOM_TARGET_ADDRESS";
            case TYPE_APPEARANCE:
                return "TYPE_APPEARANCE";
            case TYPE_ADVERTISING_INTERVAL:
                return "TYPE_ADVERTISING_INTERVAL";
            case TYPE_MANUFACTURER_SPECIFIC_DATA:
                return "TYPE_MANUFACTURER_SPECIFIC_DATA";
            case TYPE_LE_BLUETOOTH_DEVICE_ADDRESS:
                return "TYPE_LE_BLUETOOTH_DEVICE_ADDRESS";
            case TYPE_LE_ROLE:
                return "TYPE_LE_ROLE";
            case TYPE_SIMPLE_PAIRING_HASH_C_256:
                return "TYPE_SIMPLE_PAIRING_HASH_C_256";
            case TYPE_SIMPLE_PAIRING_RANDOMIZER_R_256:
                return "TYPE_SIMPLE_PAIRING_RANDOMIZER_R_256";
            case TYPE_3D_INFORMATION_DATA:
                return "TYPE_3D_INFORMATION_DATA";
            default:
                return Integer.toHexString(type);
        }
    }
}
