package is.hello.buruberi.example.activities;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.ActionBar;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.example.R;
import is.hello.buruberi.example.presenters.PeripheralPresenter;
import is.hello.buruberi.example.util.GattPeripherals;

public class PeripheralActivity extends BaseActivity {
    @Inject PeripheralPresenter peripheralPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final GattPeripheral peripheral = peripheralPresenter.getPeripheral();
        if (peripheral == null) {
            setResult(RESULT_CANCELED);
            finish();

            return;
        }

        setContentView(R.layout.activity_peripheral);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            final Resources resources = getResources();
            actionBar.setTitle(GattPeripherals.getDisplayName(peripheral, resources));
            actionBar.setSubtitle(GattPeripherals.getDetails(peripheral, resources));
        }
    }
}
