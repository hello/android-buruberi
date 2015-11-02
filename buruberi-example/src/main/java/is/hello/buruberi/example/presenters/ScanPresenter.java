package is.hello.buruberi.example.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.subjects.ReplaySubject;

@Singleton public class ScanPresenter {
    static final String LOG_TAG = ScanPresenter.class.getSimpleName();

    public final ReplaySubject<List<GattPeripheral>> peripherals = ReplaySubject.createWithSize(1);

    private final PeripheralPresenter peripheralPresenter;
    private final LoggerFacade logger;

    private @NonNull PeripheralCriteria scanCriteria = new PeripheralCriteria();
    private @Nullable Subscription scanSubscription;

    @Inject public ScanPresenter(@NonNull PeripheralPresenter peripheralPresenter,
                                 @NonNull LoggerFacade logger) {
        this.peripheralPresenter = peripheralPresenter;
        this.logger = logger;

        peripherals.onNext(Collections.<GattPeripheral>emptyList());
    }

    public void scan() {
        if (scanSubscription != null) {
            return;
        }

        final Observable<List<GattPeripheral>> scan = peripheralPresenter.scan(scanCriteria);
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

    public boolean isScanning() {
        return (scanSubscription != null);
    }

    //region Attributes

    public void setPeripheralAddresses(@NonNull List<String> addresses) {
        scanCriteria.peripheralAddresses.clear();
        scanCriteria.peripheralAddresses.addAll(addresses);
    }

    public void clearAdvertisingDataMatches() {
        scanCriteria.predicates.clear();
    }

    public void addAdvertisingDataMatch(int type, @NonNull String toMatch) {
        scanCriteria.addExactMatchPredicate(type, toMatch);
    }

    public void setLimit(int limit) {
        scanCriteria.setLimit(limit);
    }

    public void setDuration(long duration) {
        scanCriteria.setDuration(duration);
    }

    public void setWantsHighPowerPreScan(boolean wantsHighPowerPreScan) {
        scanCriteria.setWantsHighPowerPreScan(wantsHighPowerPreScan);
    }

    //endregion
}
