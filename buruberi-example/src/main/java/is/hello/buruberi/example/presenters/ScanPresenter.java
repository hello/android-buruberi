package is.hello.buruberi.example.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.List;

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

@Singleton public class ScanPresenter extends BasePresenter {
    static final String LOG_TAG = ScanPresenter.class.getSimpleName();

    public final ReplaySubject<List<GattPeripheral>> peripherals = ReplaySubject.createWithSize(1);

    private final BluetoothStack bluetoothStack;
    private final LoggerFacade logger;

    private @NonNull PeripheralCriteria scanCriteria = new PeripheralCriteria();
    private @Nullable Subscription scanSubscription;

    @Inject public ScanPresenter(@NonNull BluetoothStack bluetoothStack,
                                 @NonNull LoggerFacade logger) {
        this.bluetoothStack = bluetoothStack;
        this.logger = logger;

        peripherals.onNext(Collections.<GattPeripheral>emptyList());
    }

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

    public void setLimit(int limit) {
        scanCriteria.setLimit(limit);
    }

    public void setDuration(long duration) {
        scanCriteria.setDuration(duration);
    }

    //endregion
}
