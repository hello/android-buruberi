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

/**
 * Indicates that the connection to the peripheral was unexpected
 * lost during a Bluetooth operation. Generally caused by the remote
 * device going offline, or moving out of range of the phone.
 */
public class LostConnectionException extends BuruberiException {
    public LostConnectionException() {
        super("The connection to the peripheral was unexpected lost");
    }
}
