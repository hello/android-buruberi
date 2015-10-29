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
package is.hello.buruberi.bluetooth.errors;

import android.annotation.TargetApi;
import android.bluetooth.le.ScanCallback;
import android.os.Build;

/**
 * Indicates that a Bluetooth Low Energy scan failed due to a specified reason.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LowEnergyScanException extends BuruberiException {
    /**
     * Returns the corresponding constant name for a
     * given {@code ScanCallback#SCAN_FAILED_*} value.
     */
    public static String scanFailureToString(int scanFailure) {
        switch (scanFailure) {
            case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                return "SCAN_FAILED_ALREADY_STARTED";

            case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                return "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED";

            case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                return "SCAN_FAILED_INTERNAL_ERROR";

            case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                return "SCAN_FAILED_FEATURE_UNSUPPORTED";

            default:
                return "UNKNOWN: " + scanFailure;
        }
    }


    /**
     * The reason the scan failed. Corresponds to the error
     * codes given by Android's {@code ScanCallback} class.
     */
    public final int scanFailure;

    public LowEnergyScanException(int scanFailure) {
        super(scanFailureToString(scanFailure));

        this.scanFailure = scanFailure;
    }
}
