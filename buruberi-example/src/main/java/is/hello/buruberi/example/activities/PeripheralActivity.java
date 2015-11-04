package is.hello.buruberi.example.activities;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.example.R;
import is.hello.buruberi.example.adapters.PeripheralDetailsAdapter;
import is.hello.buruberi.example.dialogs.LoadingDialog;
import is.hello.buruberi.example.presenters.PeripheralPresenter;
import is.hello.buruberi.example.util.DividerItemDecoration;
import is.hello.buruberi.example.util.GattPeripherals;
import rx.Observable;
import rx.functions.Action1;

public class PeripheralActivity extends BaseActivity
        implements PeripheralDetailsAdapter.OnItemClickListener {
    @Inject PeripheralPresenter peripheralPresenter;

    private Button connectionButton;
    private Button bondButton;

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

        final Resources resources = getResources();
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(GattPeripherals.getDisplayName(peripheral, resources));
        }

        this.connectionButton = (Button) findViewById(R.id.activity_peripheral_connection);
        this.bondButton = (Button) findViewById(R.id.activity_peripheral_bond);

        final RecyclerView recyclerView =
                (RecyclerView) findViewById(R.id.activity_peripheral_details_recycler);
        recyclerView.addItemDecoration(new DividerItemDecoration(resources, false));

        final PeripheralDetailsAdapter adapter = new PeripheralDetailsAdapter(this, this);
        adapter.bindAdvertisingData(peripheral.getAdvertisingData());
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        final Observable<Boolean> working = bind(peripheralPresenter.working);
        working.subscribe(new Action1<Boolean>() {
            LoadingDialog loadingDialog = null;

            @Override
            public void call(Boolean isWorking) {
                if (isWorking) {
                    if (loadingDialog == null) {
                        this.loadingDialog = new LoadingDialog(PeripheralActivity.this);
                        loadingDialog.show();
                    }
                } else {
                    if (loadingDialog != null) {
                        loadingDialog.dismiss();
                        this.loadingDialog = null;
                    }
                }
            }
        });

        final Observable<Boolean> connected = bind(peripheralPresenter.connected);
        connected.subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean isConnected) {
                if (isConnected) {
                    connectionButton.setText(R.string.action_disconnect);
                } else {
                    connectionButton.setText(R.string.action_connect);
                }
            }
        });

        final Observable<Boolean> bonded = bind(peripheralPresenter.bonded);
        bonded.subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean isBonded) {
                if (isBonded) {
                    bondButton.setText(R.string.action_unbond);
                } else {
                    bondButton.setText(R.string.action_bond);
                }
            }
        });
    }

    public void changeConnection(@NonNull View ignored) {
        if (peripheralPresenter.isConnected()) {
            presentError(peripheralPresenter.disconnect());
        } else {
            presentError(peripheralPresenter.connect());
        }
    }

    public void changeBond(@NonNull View ignored) {
        if (peripheralPresenter.isBonded()) {
            presentError(peripheralPresenter.removeBond());
        } else {
            presentError(peripheralPresenter.createBond());
        }
    }

    @Override
    public void onAdvertisingRecordClick(int type, @NonNull String value) {
    }

}
