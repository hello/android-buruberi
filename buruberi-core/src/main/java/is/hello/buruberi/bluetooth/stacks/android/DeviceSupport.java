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
package is.hello.buruberi.bluetooth.stacks.android;

import android.os.Build;
import android.support.annotation.NonNull;

import java.util.regex.Pattern;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;

public final class DeviceSupport {
    static final String MANUFACTURER_SAMSUNG = "samsung";
    static final String MANUFACTURER_LG = "lg";
    static final String MANUFACTURER_HTC = "htc";
    static final String MANUFACTURER_MOTOROLA = "motorola";

    static boolean isModelSupported(@NonNull String manufacturerPattern, @NonNull String model) {
        return Pattern.compile(manufacturerPattern, Pattern.CASE_INSENSITIVE)
                      .matcher(model)
                      .matches();
    }

    static String getManufacturerSupportedPattern(@NonNull String manufacturer) {
        switch (manufacturer.toLowerCase()) {
            case MANUFACTURER_SAMSUNG: {
                return "((GT\\-|SCH\\-|SGH\\-|SHV\\-|SPH\\-|SM\\-|N)(I9301I|I9305(N|T)|I535|J021|R530|S960L|S968C|I747(m)?|T999(Lv|L)?|E210(K|L|S)|I9500|I9502|I9505(G)?|I9506|I9508|I545|I959|R970(X|C)?|I337(M)?|M919(V)?|N045|E300(K|L|S)|E330(K|L|S)|L720|G900(A|FD|F|H|I|R4|T|V|RZWAUSC|P|W8)|G920(0|8|8/SS|9|A|F|FD|I|S|T)|G925(0|A|F|FQ|I|K|L|S|T)|I605|R950|L900|N910(M|V|A|T|P|R4)|N915(FY|A|T|K|L|S|G|D)|7100|900(0|2|5)))";
            }
            case MANUFACTURER_LG: {
                return "(Nexus (4|5)|LG\\-D8(55|50|51|52)|H815(TR|T|P)?|H8(12|10|11)|LS991|VS986|US991)";
            }
            case MANUFACTURER_HTC: {
                return "(Nexus 9|HTC Desire (320|510|816 dual sim|816|820s dual sim|820)|(HTC One)( (801(e|n|c|s)|m7|m8x|m8|m9|mini 2|mini))?)";
            }
            case MANUFACTURER_MOTOROLA: {
                return "(MotoE2|Nexus 6|XT10(31|32|33|34|39|40|42|45|63|64|68|69|72|79|49|50|52|53|55|56|58|60|85|92|93|94|95|96|97|21|22|23|80))";
            }
            default: {
                return "(D6708|A0001)";
            }
        }
    }

    public static @NonNull BluetoothStack.SupportLevel getDeviceSupportLevel() {
        String manufacturerPattern = getManufacturerSupportedPattern(Build.MANUFACTURER);
        if (isModelSupported(manufacturerPattern, Build.MODEL)) {
            return BluetoothStack.SupportLevel.TESTED;
        } else {
            return BluetoothStack.SupportLevel.UNTESTED;
        }
    }

    public static boolean isHighPowerPreScanNeeded() {
        return false;
    }
}
