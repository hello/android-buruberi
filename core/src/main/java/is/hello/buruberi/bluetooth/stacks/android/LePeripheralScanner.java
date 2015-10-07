package is.hello.buruberi.bluetooth.stacks.android;

import java.util.List;

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import rx.Observable;

/**
 * Internal to native Android stack.
 */
interface LePeripheralScanner extends Observable.OnSubscribe<List<GattPeripheral>> {
}
