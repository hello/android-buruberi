package is.hello.buruberi.bluetooth.stacks.util;

import org.junit.Test;

import java.util.Arrays;

import is.hello.buruberi.testing.AssertExtensions;
import is.hello.buruberi.testing.BuruberiTestCase;

import static is.hello.buruberi.testing.AssertExtensions.assertThrows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BytesTests extends BuruberiTestCase {
    @Test
    public void toStringWithBounds() throws Exception {
        final byte[] emptyBytes = {};
        final String emptyString = Bytes.toString(emptyBytes, 0, 0);
        assertEquals("", emptyString);


        final byte[] testBytes = { 0x12, 0x14, 0x0f, 0x12 };
        assertEquals("1214", Bytes.toString(testBytes, 0, 2));
        assertEquals("0F12", Bytes.toString(testBytes, 2, 4));

        assertThrows(new AssertExtensions.ThrowingRunnable() {
            @SuppressWarnings("ResultOfMethodCallIgnored")
            @Override
            public void run() throws Exception {
                Bytes.toString(emptyBytes, 0, 1);
            }
        });
        assertThrows(new AssertExtensions.ThrowingRunnable() {
            @SuppressWarnings("ResultOfMethodCallIgnored")
            @Override
            public void run() throws Exception {
                Bytes.toString(testBytes, 3, 2);
            }
        });
    }

    @Test
    public void toStringWithoutBounds() throws Exception {
        byte[] emptyBytes = {};
        String emptyString = Bytes.toString(emptyBytes);
        assertEquals("", emptyString);


        byte[] testBytes = { 0x12, 0x14, 0x0f, 0x12 };
        String testString = Bytes.toString(testBytes);
        assertEquals("12140F12", testString);
    }

    @Test
    public void fromString() throws Exception {
        byte[] emptyBytes = {};
        assertTrue(Arrays.equals(emptyBytes, Bytes.fromString("")));

        byte[] testBytes = { 0x12, 0x14, 0x0f, 0x12 };
        assertTrue(Arrays.equals(testBytes, Bytes.fromString("12140F12")));

        assertThrows(new AssertExtensions.ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                Bytes.fromString("121");
            }
        });
    }

    @Test
    public void startWith() throws Exception {
        byte[] sequence = { 0x1, 0x2, 0x3 };
        assertTrue(Bytes.startWith(sequence, new byte[] { 0x1, 0x2 }));
        assertFalse(Bytes.startWith(sequence, new byte[] { 0x2, 0x3 }));
    }

    @Test
    public void contains() throws Exception {
        byte[] sequence = { 0x1, 0x2, 0x3 };
        assertTrue(Bytes.contains(sequence, (byte) 0x1));
        assertTrue(Bytes.contains(sequence, (byte) 0x2));
        assertTrue(Bytes.contains(sequence, (byte) 0x3));
        assertFalse(Bytes.contains(sequence, (byte) 0x4));
    }
}
