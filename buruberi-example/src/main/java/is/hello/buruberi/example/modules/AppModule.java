package is.hello.buruberi.example.modules;

import dagger.Module;
import is.hello.buruberi.example.activities.ScanActivity;

@Module(complete = false,
        includes = {
                BluetoothModule.class,
        },
        injects = {
                ScanActivity.class
        })
public class AppModule {
}
