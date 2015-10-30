package is.hello.buruberi.example.presenters;

import android.support.annotation.NonNull;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import rx.subjects.ReplaySubject;

@Singleton public class ScanPresenter {
    public final ReplaySubject<List<GattPeripheral>> peripherals = ReplaySubject.createWithSize(1);

    private final PeripheralPresenter peripheralPresenter;

    @Inject public ScanPresenter(@NonNull PeripheralPresenter peripheralPresenter) {
        this.peripheralPresenter = peripheralPresenter;
    }
}
