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

import org.junit.Test;

import java.util.Collection;

import is.hello.buruberi.testing.BuruberiTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class AdvertisingDataTests extends BuruberiTestCase {
    private static final byte[] TEST_PAYLOAD = {
            // Advertisement contents size
            (byte) 0x03,

            // Advertisement data type
            (byte) AdvertisingData.TYPE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS,

            // Begin data
            (byte) 0xE1,
            (byte) 0xFE,
    };

    @SuppressWarnings("ConstantConditions")
    @Test
    public void parse() throws Exception {
        final AdvertisingData advertisingData = AdvertisingData.parse(TEST_PAYLOAD);
        assertFalse(advertisingData.isEmpty());

        final Collection<byte[]> records = advertisingData.getRecordsForType(AdvertisingData.TYPE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS);
        assertNotNull(records);
        assertFalse(records.isEmpty());
        assertEquals(Bytes.toString(records.iterator().next()), "E1FE");
    }
}
