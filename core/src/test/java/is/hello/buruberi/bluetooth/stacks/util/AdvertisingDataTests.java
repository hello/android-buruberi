package is.hello.buruberi.bluetooth.stacks.util;

import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class AdvertisingDataTests {
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
        AdvertisingData advertisingData = AdvertisingData.parse(TEST_PAYLOAD);
        assertFalse(advertisingData.isEmpty());

        Collection<byte[]> records = advertisingData.getRecordsForType(AdvertisingData.TYPE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS);
        assertNotNull(records);
        assertFalse(records.isEmpty());
        assertEquals(Bytes.toString(records.iterator().next()), "E1FE");
    }
}
