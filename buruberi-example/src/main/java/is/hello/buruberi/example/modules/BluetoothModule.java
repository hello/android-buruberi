package is.hello.buruberi.example.modules;

import android.content.Context;
import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.buruberi.bluetooth.Buruberi;
import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.example.presenters.PeripheralPresenter;
import is.hello.buruberi.example.presenters.ScanPresenter;

@Module(complete = false,
        injects = {
                ScanPresenter.class,
                PeripheralPresenter.class,
        })
public class BluetoothModule {
    private final Context context;

    public BluetoothModule(@NonNull Context context) {
        this.context = context;
    }

    @Singleton @Provides BluetoothStack provideBluetoothStack() {
        return new Buruberi()
                .setApplicationContext(context)
                .build();
    }
}
