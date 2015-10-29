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

import android.support.annotation.NonNull;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DeviceSupportTests {
    public static boolean isSupported(@NonNull String manufacturer, @NonNull String model) {
        return DeviceSupport.isModelSupported(DeviceSupport.getManufacturerSupportedPattern(manufacturer), model);
    }

    @Test
    public void samsung() throws Exception {
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "GT-I9301I"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "GT-I9305N"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "GT-I9305T"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SCH-I535"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SCH-J021"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SCH-R530"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SCH-S960L"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SCH-S968C"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SGH-I747"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SGH-I747m"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SGH-T999Lv"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SGH-T999L"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SHV-E210K"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SHV-E210L"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SHV-E210S"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "GT-I9500"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "GT-I9502"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "GT-I9505"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "GT-I9505G"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "GT-I9506"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "GT-I9508"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SCH-I545"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SCH-I959"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SCH-R970X"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SCH-R970C"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SGH-I337"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SGH-I337M"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SGH-M919"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SGH-M919V"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SGH-N045"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SHV-E300K"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SHV-E300L"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SHV-E300S"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SHV-E330K"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SHV-E330L"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SHV-E330S"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SPH-L720"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G900A"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G900F"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G900H"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G900I"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G900R4"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G900T"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G900V"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G900RZWAUSC"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G900P"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G900W8"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G900FD"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G9200"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G9208"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G9208/SS"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G9209"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G920A"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G920F"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G920FD"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G920I"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G920S"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G920T"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G9250"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G925A"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G925F"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G925FQ"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G925I"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G925K"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G925L"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G925S"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-G925T"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "N7100"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SCH-I605"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SCH-R950"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SPH-L900"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "N9000"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "N9002"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "N9005"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-N910M"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-N910V"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-N910A"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-N910T"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-N910P"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-N910R4"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-N915FY"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-N915A"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-N915T"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-N915K"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-N915L"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-N915S"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-N915G"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_SAMSUNG, "SM-N915D"));
    }

    @Test
    public void lg() throws Exception {
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_LG, "Nexus 4"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_LG, "Nexus 5"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_LG, "H815"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_LG, "H815TR"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_LG, "H815T"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_LG, "H815P"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_LG, "H812"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_LG, "H810"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_LG, "H811"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_LG, "LS991"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_LG, "VS986"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_LG, "US991"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_LG, "LG-D855"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_LG, "LG-D850"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_LG, "LG-D851"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_LG, "LG-D852"));
    }

    @Test
    public void htc() throws Exception {
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_HTC, "HTC One"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_HTC, "HTC One 801e"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_HTC, "HTC One 801n"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_HTC, "HTC One 801c"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_HTC, "HTC One 801s"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_HTC, "HTC One M8"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_HTC, "HTC One M8x"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_HTC, "HTC One M9"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_HTC, "HTC One mini"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_HTC, "HTC One mini 2"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_HTC, "HTC Desire 320"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_HTC, "HTC Desire 510"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_HTC, "HTC Desire 816 dual sim"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_HTC, "HTC Desire 816"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_HTC, "HTC Desire 820s dual sim"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_HTC, "HTC Desire 820"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_HTC, "Nexus 9"));
    }

    @Test
    public void motorola() throws Exception {
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1031"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1032"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1033"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1034"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1039"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1040"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1042"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1045"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1063"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1064"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1068"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1069"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1072"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1079"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1049"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1050"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1052"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1053"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1055"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1056"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1058"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1060"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1085"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1092"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1093"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1094"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1095"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1096"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1097"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1021"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1022"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1023"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "MotoE2"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "XT1080"));
        assertTrue(isSupported(DeviceSupport.MANUFACTURER_MOTOROLA, "Nexus 6"));
    }

    @Test
    public void other() throws Exception {
        assertTrue(isSupported("", "D6708"));
        assertTrue(isSupported("", "A0001"));
    }
}
