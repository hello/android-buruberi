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
package is.hello.buruberi.util;

import android.support.annotation.NonNull;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;
import is.hello.buruberi.bluetooth.stacks.util.Bytes;

public class AdvertisingDataBuilder {
    private static final int HEADER_LENGTH = 2;

    private final List<Pair<Integer, byte[]>> entries = new ArrayList<>();
    private int totalSize = 0;

    public AdvertisingDataBuilder add(int type, @NonNull String payload) {
        byte[] payloadAsBytes = Bytes.fromString(payload);
        entries.add(Pair.create(type, payloadAsBytes));
        totalSize += HEADER_LENGTH + payloadAsBytes.length;
        return this;
    }

    public byte[] buildRaw() {
        byte[] buffer = new byte[totalSize];

        int pointer = 0;
        for (Pair<Integer, byte[]> entry : entries) {
            //noinspection UnnecessaryLocalVariable
            int lengthOffset = pointer;
            int typeOffset = pointer + 1;
            int dataOffset = pointer + 2;
            int dataLength = entry.second.length;
            int entryLength = dataLength + 1;

            buffer[lengthOffset] = (byte) entryLength;
            buffer[typeOffset] = entry.first.byteValue();

            System.arraycopy(
                /* src */ entry.second,
                /* srcStart */ 0,
                /* dest */ buffer,
                /* destStart */ dataOffset,
                /* length */ dataLength
            );

            pointer += dataLength + HEADER_LENGTH;
        }

        return buffer;
    }

    public AdvertisingData build() {
        return AdvertisingData.parse(buildRaw());
    }
}
