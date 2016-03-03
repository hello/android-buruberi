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

import android.os.Parcel;
import android.os.Parcelable;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import is.hello.buruberi.testing.BuruberiTestCase;

import static is.hello.buruberi.bluetooth.stacks.util.AdvertisingData.TYPE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class AdvertisingDataTests extends BuruberiTestCase {
    private static final byte[] TEST_PAYLOAD = {
            // Advertisement contents size
            (byte) 0x03,

            // Advertisement data type
            (byte) TYPE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS,

            // Begin data
            (byte) 0xE1,
            (byte) 0xFE,
    };

    @SuppressWarnings("ConstantConditions")
    @Test
    public void parse() {
        final AdvertisingData advertisingData = AdvertisingData.parse(TEST_PAYLOAD);
        assertThat(advertisingData.isEmpty(), is(false));

        final Collection<byte[]> records =
                advertisingData.getRecordsForType(TYPE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS);
        assertThat(records, is(notNullValue()));
        assertThat(records.isEmpty(), is(false));
        assertThat(Bytes.toString(records.iterator().next()), is(equalTo("E1FE")));
    }

    @Test
    public void parceling() {
        final AdvertisingData outData = AdvertisingData.parse(TEST_PAYLOAD);
        assertThat(outData.isEmpty(), is(false));

        final AdvertisingData inData = parcelUnparcel(outData);
        assertThat(inData.isEmpty(), is(false));
        assertThat(asArray(inData.getRecordsForType(TYPE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS)),
                   is(deepEqualTo(asArray(outData.getRecordsForType(TYPE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS)))));
    }


    //region Utilities

    private static <T extends Parcelable> T parcelUnparcel(T out) {
        final Parcel outParcel = Parcel.obtain();
        final byte[] marshaled;
        try {
            outParcel.writeParcelable(out, 0);
            marshaled = outParcel.marshall();
        } finally {
            outParcel.recycle();
        }

        final Parcel inParcel = Parcel.obtain();
        try {
            inParcel.unmarshall(marshaled, 0, marshaled.length);
            return inParcel.readParcelable(out.getClass().getClassLoader());
        } finally {
            inParcel.recycle();
        }
    }

    private static byte[][] asArray(List<byte[]> entries) {
        if (entries == null || entries.isEmpty()) {
            return new byte[0][];
        } else {
            return entries.toArray(new byte[entries.size()][]);
        }
    }

    private static Matcher<Object[]> deepEqualTo(final Object[] right) {
        return new BaseMatcher<Object[]>() {
            @Override
            public boolean matches(Object item) {
                return (item.getClass().isArray() &&
                        Arrays.deepEquals((Object[]) item, right));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("deep equals ");
                description.appendValue(right);
            }
        };
    }

    //endregion
}
