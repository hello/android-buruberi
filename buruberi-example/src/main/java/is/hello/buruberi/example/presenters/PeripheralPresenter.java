package is.hello.buruberi.example.presenters;

import android.support.annotation.CheckResult;
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

/**
 * Wraps the connectivity and bond state of a Bluetooth Low Energy
 * peripheral and provides observable hooks.
 */
@Singleton public class PeripheralPresenter extends BasePresenter {
    /**
     * The connected state of the peripheral.
     */
    public final ReplaySubject<Boolean> connected = ReplaySubject.createWithSize(1);

    /**
     * The bond state of the peripheral.
     */
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

    /**
     * Set the peripheral whose state the presenter should track.
     */
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

    /**
     * Attempts to connect to the peripheral, timing out after
     * 30 seconds if the remote peripheral does not respond.
     * <p>
     * Updates the value of {@link #connected} upon completion.
     */
    @CheckResult
    public Observable<GattPeripheral> connect() {
        logEvent("connect()");

        if (peripheral == null) {
            return Observable.error(new ConnectionStateException());
        }

        final OperationTimeout timeout =
                peripheral.createOperationTimeout("Connect", 30L, TimeUnit.SECONDS);
        final Observable<GattPeripheral> connect =
                peripheral.connect(GattPeripheral.CONNECT_FLAG_DEFAULTS, timeout)
                          .doOnTerminate(new Action0() {
                              @Override
                              public void call() {
                                  updateConnected();
                              }
                          });
        return bind(connect);
    }

    /**
     * Attempts to disconnect from the peripheral. Does nothing if
     * the peripheral is not currently connected.
     * <p>
     * Updates the value of {@link #connected} upon completion.
     */
    @CheckResult
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

    /**
     * Attempts to pair with the peripheral.
     * <p>
     * Updates the value of {@link #bonded} upon completion.
     *
     * @see GattPeripheral#createBond() for more info about this method's behavior.
     */
    @CheckResult
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

    /**
     * Attempts to un-pair with the peripheral, timing out after 30 seconds if
     * the peripheral does not respond.
     * <p>
     * Updates the value of {@link #bonded} upon completion.
     *
     * @see GattPeripheral#createBond() for more info about this method's behavior.
     */
    @CheckResult
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
