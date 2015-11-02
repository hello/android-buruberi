package is.hello.buruberi.example.presenters;

import android.support.annotation.NonNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.buruberi.example.util.Optional;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;

@Singleton public class PeripheralPresenter {
    public final ReplaySubject<Optional<GattPeripheral>> peripheral = ReplaySubject.createWithSize(1);

    private final BluetoothStack stack;
    private final LoggerFacade logger;

    @Inject public PeripheralPresenter(@NonNull BluetoothStack stack,
                                       @NonNull LoggerFacade logger) {
        this.stack = stack;
        this.logger = logger;

        peripheral.onNext(Optional.<GattPeripheral>empty());
    }

    public Observable<List<GattPeripheral>> scan(@NonNull PeripheralCriteria criteria) {
        return stack.discoverPeripherals(criteria);
    }

    public Observable<GattPeripheral> connect(@NonNull final GattPeripheral toConnect) {
        return toConnect.connect(toConnect.createOperationTimeout("Connect", 30L, TimeUnit.SECONDS))
                        .doOnNext(new Action1<GattPeripheral>() {
                            @Override
                            public void call(GattPeripheral gattPeripheral) {
                                peripheral.onNext(Optional.of(gattPeripheral));
                            }
                        })
                        .doOnError(new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                peripheral.onNext(Optional.<GattPeripheral>empty());
                            }
                        });
    }

    public Observable<GattPeripheral> disconnect() {
        return peripheral.take(1)
                         .flatMap(new Func1<Optional<GattPeripheral>, Observable<GattPeripheral>>() {
                             @Override
                             public Observable<GattPeripheral> call(Optional<GattPeripheral> toDisconnect) {
                                 if (toDisconnect.isPresent()) {
                                     return toDisconnect.get()
                                                        .disconnect()
                                                        .doOnCompleted(new Action0() {
                                                            @Override
                                                            public void call() {
                                                                peripheral.onNext(Optional.<GattPeripheral>empty());
                                                            }
                                                        });
                                 } else {
                                     return Observable.just(null);
                                 }
                             }
                         });
    }
}
