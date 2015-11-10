package is.hello.buruberi.example;

import android.app.Application;
import android.support.annotation.NonNull;

import dagger.ObjectGraph;
import is.hello.buruberi.example.modules.AppModule;
import is.hello.buruberi.example.modules.BluetoothModule;
import is.hello.buruberi.example.modules.Injector;

public class ExampleApplication extends Application implements Injector {
    private ObjectGraph objectGraph;

    @Override
    public void onCreate() {
        super.onCreate();

        this.objectGraph = ObjectGraph.create(new BluetoothModule(getBaseContext()),
                                              new AppModule());
    }

    @Override
    public <T> void inject(@NonNull T target) {
        objectGraph.inject(target);
    }
}
