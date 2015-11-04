package is.hello.buruberi.example.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.buruberi.bluetooth.errors.ConnectionStateException;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import rx.Observable;
import rx.functions.Action0;
import rx.subjects.ReplaySubject;

@Singleton public class PeripheralPresenter extends BasePresenter {
    public final ReplaySubject<Boolean> connected = ReplaySubject.createWithSize(1);
    public final ReplaySubject<Boolean> bonded = ReplaySubject.createWithSize(1);

    private final LoggerFacade logger;
    private @Nullable GattPeripheral peripheral;

    @Inject public PeripheralPresenter(@NonNull LoggerFacade logger) {
        this.logger = logger;

        updateConnected();
        updateBonded();
    }

    private void updateConnected() {
        if (peripheral != null) {
            connected.onNext(peripheral.getConnectionStatus() == GattPeripheral.STATUS_CONNECTED);
        } else {
            connected.onNext(false);
        }
    }

    private void updateBonded() {
        if (peripheral != null) {
            bonded.onNext(peripheral.getBondStatus() == GattPeripheral.BOND_BONDED);
        } else {
            bonded.onNext(false);
        }
    }

    private void logEvent(@NonNull String event) {
        logger.debug(getClass().getSimpleName(), event);
    }

    public void setPeripheral(@Nullable GattPeripheral peripheral) {
        this.peripheral = peripheral;

        updateConnected();
        updateBonded();
    }

    @Nullable
    public GattPeripheral getPeripheral() {
        return peripheral;
    }

    public boolean isConnected() {
        return (peripheral != null &&
                peripheral.getConnectionStatus() == GattPeripheral.STATUS_CONNECTED);
    }

    public boolean isBonded() {
        return (peripheral != null &&
                peripheral.getBondStatus() == GattPeripheral.BOND_BONDED);
    }

    public Observable<GattPeripheral> connect() {
        logEvent("connect()");

        if (peripheral == null) {
            return Observable.error(new ConnectionStateException());
        }

        final OperationTimeout timeout =
                peripheral.createOperationTimeout("Connect", 30L, TimeUnit.SECONDS);
        final Observable<GattPeripheral> connect =
                peripheral.connect(timeout)
                          .doOnTerminate(new Action0() {
                              @Override
                              public void call() {
                                  updateConnected();
                              }
                          });
        return bind(connect);
    }

    public Observable<GattPeripheral> disconnect() {
        logEvent("disconnect()");

        if (peripheral == null) {
            return Observable.error(new ConnectionStateException());
        }

        final Observable<GattPeripheral> disconnect =
                peripheral.disconnect()
                          .doOnTerminate(new Action0() {
                              @Override
                              public void call() {
                                  updateConnected();
                              }
                          });
        return bind(disconnect);
    }

    @NonNull
    public Observable<GattPeripheral> createBond() {
        logEvent("createBond()");

        if (peripheral == null) {
            return Observable.error(new ConnectionStateException());
        }

        final Observable<GattPeripheral> createBond =
                peripheral.createBond()
                          .doOnTerminate(new Action0() {
                              @Override
                              public void call() {
                                  updateBonded();
                              }
                          });
        return bind(createBond);
    }

    @NonNull
    public Observable<GattPeripheral> removeBond() {
        logEvent("removeBond()");
        if (peripheral == null) {
            return Observable.error(new ConnectionStateException());
        }

        final OperationTimeout timeout =
                peripheral.createOperationTimeout("Remove Bond", 30L, TimeUnit.SECONDS);
        final Observable<GattPeripheral> removeBond =
                peripheral.removeBond(timeout)
                          .doOnTerminate(new Action0() {
                              @Override
                              public void call() {
                                  updateBonded();
                              }
                          });
        return bind(removeBond);
    }
}
