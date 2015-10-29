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
import android.text.TextUtils;

public final class Bytes {

    /**
     * Converts a subsection of an array of bytes to a string of the format <code>0122FF</code>
     */
    public static @NonNull String toString(@NonNull byte[] bytes, int start, int end) {
        if (start > end || end > bytes.length) {
            throw new IndexOutOfBoundsException();
        }

        StringBuilder builder = new StringBuilder(bytes.length * 2);

        for (int i = start; i < end; i++) {
            builder.append(String.format("%02X", bytes[i]));
        }

        return builder.toString();
    }

    /**
     * Converts an entire array of bytes to a string of the format <code>0122FF</code>
     */
    public static @NonNull String toString(@NonNull byte[] bytes) {
        return toString(bytes, 0, bytes.length);
    }

    /**
     * Converts a string of the format <code>0122FF</code> to an array of bytes.
     *
     * @throws IllegalArgumentException on invalid input.
     */
    public static @NonNull byte[] fromString(@Nullable String string) {
        if (TextUtils.isEmpty(string)) {
            return new byte[0];
        }

        if (string.length() % 2 != 0) {
            throw new IllegalArgumentException("string length is odd");
        }

        byte[] bytes = new byte[string.length() / 2];
        for (int i = 0, length = string.length(); i < length; i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(string.substring(i, i + 2), 16);
        }

        return bytes;
    }

    /**
     * Converts a string of the format <code>0122FF</code> to an array of bytes.
     * <p>
     * The same as {@see #fromString(String)}, but it does not throw exceptions.
     *
     * @return A <code>byte[]</code> array if the string could be converted; null otherwise.
     */
    public static @Nullable byte[] tryFromString(@Nullable String string) {
        try {
            return fromString(string);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean startWith(@NonNull byte[] haystack, @NonNull byte[] needle) {
        if (haystack.length < needle.length) {
            return false;
        }

        for (int i = 0, length = needle.length; i < length; i++) {
            if (haystack[i] != needle[i]) {
                return false;
            }
        }

        return true;
    }

    public static boolean contains(@NonNull byte[] haystack, byte needle) {
        for (byte b : haystack) {
            if (b == needle) {
                return true;
            }
        }

        return false;
    }

}
