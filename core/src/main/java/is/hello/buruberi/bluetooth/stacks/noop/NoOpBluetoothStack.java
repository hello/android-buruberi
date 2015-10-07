package is.hello.buruberi.bluetooth.stacks.noop;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import is.hello.buruberi.bluetooth.errors.ChangePowerStateException;
import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.buruberi.util.Rx;
import rx.Observable;
import rx.Scheduler;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class NoOpBluetoothStack implements BluetoothStack {
    private final LoggerFacade logger;

    public NoOpBluetoothStack(@NonNull LoggerFacade logger) {
        this.logger = logger;

        logger.error(LOG_TAG, "Device does not support Bluetooth", null);
    }

    @NonNull
    @Override
    public Observable<List<GattPeripheral>> discoverPeripherals(@NonNull PeripheralCriteria peripheralCriteria) {
        return Observable.just(Collections.<GattPeripheral>emptyList());
    }

    @NonNull
    @Override
    public Scheduler getScheduler() {
        return Rx.mainThreadScheduler();
    }

    @Override
    public <T> Observable<T> newConfiguredObservable(Observable.OnSubscribe<T> onSubscribe) {
        return Observable.create(onSubscribe)
                         .subscribeOn(getScheduler());
    }

    @Override
    public Observable<Boolean> enabled() {
        return Observable.just(false);
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public Observable<Void> turnOn() {
        return Observable.error(new ChangePowerStateException());
    }

    @Override
    public Observable<Void> turnOff() {
        return Observable.error(new ChangePowerStateException());
    }

    @Override
    public boolean errorRequiresReconnect(@Nullable Throwable e) {
        return false;
    }

    @NonNull
    @Override
    public LoggerFacade getLogger() {
        return logger;
    }

    @Override
    public SupportLevel getDeviceSupportLevel() {
        return SupportLevel.UNSUPPORTED;
    }
}
