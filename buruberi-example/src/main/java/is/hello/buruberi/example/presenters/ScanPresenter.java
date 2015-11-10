package is.hello.buruberi.example.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.subjects.ReplaySubject;

/**
 * Responsible for conducting Bluetooth Low Energy peripheral scans.
 */
@Singleton public class ScanPresenter extends BasePresenter {
    static final String LOG_TAG = ScanPresenter.class.getSimpleName();

    /**
     * The peripherals found by the scan peripheral. Initially contains an empty list.
     * <p>
     * This subject will emit an empty list if a peripheral scan fails.
     */
    public final ReplaySubject<List<GattPeripheral>> peripherals = ReplaySubject.createWithSize(1);

    private final BluetoothStack bluetoothStack;
    private final LoggerFacade logger;

    private final PeripheralCriteria scanCriteria = new PeripheralCriteria();
    private @Nullable Subscription scanSubscription;

    @Inject public ScanPresenter(@NonNull BluetoothStack bluetoothStack,
                                 @NonNull LoggerFacade logger) {
        this.bluetoothStack = bluetoothStack;
        this.logger = logger;

        peripherals.onNext(Collections.<GattPeripheral>emptyList());
    }

    /**
     * Starts a Bluetooth Low Energy peripheral scan, if one is not already underway.
     * Results are delivered through the {@link #peripherals} subject.
     */
    public void scan() {
        if (scanSubscription != null) {
            return;
        }

        final Observable<List<GattPeripheral>> scan =
                bind(bluetoothStack.discoverPeripherals(scanCriteria));
        this.scanSubscription = scan.subscribe(new Subscriber<List<GattPeripheral>>() {
            @Override
            public void onCompleted() {
                ScanPresenter.this.scanSubscription = null;
            }

            @Override
            public void onError(Throwable e) {
                logger.error(LOG_TAG, "Could not complete scan", e);
                peripherals.onNext(Collections.<GattPeripheral>emptyList());
                ScanPresenter.this.scanSubscription = null;
            }

            @Override
            public void onNext(List<GattPeripheral> gattPeripherals) {
                logger.info(LOG_TAG, "Scan completed with " +
                        gattPeripherals.size() + " results");
                peripherals.onNext(gattPeripherals);
            }
        });
    }

    //region Attributes

    /**
     * Sets the maximum number of peripherals to scan before ending the scan.
     */
    public void setLimit(int limit) {
        scanCriteria.setLimit(limit);
    }

    /**
     * Sets the maximum amount of time that can elapse before a scan is ended.
     */
    public void setDuration(long duration, @NonNull TimeUnit timeUnit) {
        scanCriteria.setDuration(timeUnit.toMillis(duration));
    }

    //endregion
}
