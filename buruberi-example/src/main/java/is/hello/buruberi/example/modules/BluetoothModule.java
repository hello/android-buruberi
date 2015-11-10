package is.hello.buruberi.example.modules;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.buruberi.bluetooth.Buruberi;
import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.util.LoggerFacade;
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

    @Singleton @Provides LoggerFacade provideLoggerFacade() {
        return new LoggerFacade() {
            @Override
            public void error(@NonNull String tag, @Nullable String message, @Nullable Throwable e) {
                Log.e(tag, message, e);
            }

            @Override
            public void warn(@NonNull String tag, @Nullable String message, @Nullable Throwable e) {
                Log.w(tag, message, e);
            }

            @Override
            public void warn(@NonNull String tag, @Nullable String message) {
                Log.w(tag, message);
            }

            @Override
            public void info(@NonNull String tag, @Nullable String message) {
                Log.i(tag, message);
            }

            @Override
            public void debug(@NonNull String tag, @Nullable String message) {
                Log.d(tag, message);
            }
        };
    }

    @Singleton @Provides BluetoothStack provideBluetoothStack(@NonNull LoggerFacade logger) {
        return new Buruberi()
                .setApplicationContext(context)
                .setLoggerFacade(logger)
                .build();
    }
}
