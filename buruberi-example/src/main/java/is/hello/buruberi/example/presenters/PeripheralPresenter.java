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

@Singleton public class PeripheralPresenter extends BasePresenter {
    private static final String LOG_TAG = PeripheralPresenter.class.getSimpleName();

    private final LoggerFacade logger;
    private @Nullable GattPeripheral peripheral;

    @Inject public PeripheralPresenter(@NonNull LoggerFacade logger) {
        this.logger = logger;
    }

    public void setPeripheral(@Nullable GattPeripheral peripheral) {
        this.peripheral = peripheral;
    }

    @Nullable
    public GattPeripheral getPeripheral() {
        return peripheral;
    }

    public Observable<GattPeripheral> connect() {
        if (peripheral == null) {
            return Observable.error(new ConnectionStateException());
        }

        final OperationTimeout timeout =
                peripheral.createOperationTimeout("Connect", 30L, TimeUnit.SECONDS);
        final Observable<GattPeripheral> connect = peripheral.connect(timeout);
        return bind(connect);
    }

    public Observable<GattPeripheral> disconnect() {
        if (peripheral == null) {
            return Observable.error(new ConnectionStateException());
        }

        return bind(peripheral.disconnect());
    }
}
