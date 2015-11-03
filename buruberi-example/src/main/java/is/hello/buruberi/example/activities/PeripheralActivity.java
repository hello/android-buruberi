package is.hello.buruberi.example.activities;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import java.util.List;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.PeripheralService;
import is.hello.buruberi.example.R;
import is.hello.buruberi.example.adapters.PeripheralDetailsAdapter;
import is.hello.buruberi.example.presenters.PeripheralPresenter;
import is.hello.buruberi.example.util.DividerItemDecoration;
import is.hello.buruberi.example.util.GattPeripherals;
import rx.Observable;
import rx.functions.Action1;

public class PeripheralActivity extends BaseActivity
        implements PeripheralDetailsAdapter.OnItemClickListener {
    @Inject PeripheralPresenter peripheralPresenter;

    private View busyShield;
    private Button connectionButton;
    private Button bondButton;
    private Button discoverServicesButton;
    private PeripheralDetailsAdapter adapter;

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

        this.busyShield = findViewById(R.id.activity_peripheral_busy_shield);
        this.connectionButton = (Button) findViewById(R.id.activity_peripheral_connection);
        this.bondButton = (Button) findViewById(R.id.activity_peripheral_bond);
        this.discoverServicesButton = (Button) findViewById(R.id.activity_peripheral_discover_services);

        final RecyclerView recyclerView =
                (RecyclerView) findViewById(R.id.activity_peripheral_details_recycler);
        recyclerView.addItemDecoration(new DividerItemDecoration(resources, false));

        this.adapter = new PeripheralDetailsAdapter(this, this);
        adapter.bindAdvertisingData(peripheral.getAdvertisingData());
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        final Observable<Boolean> working = bind(peripheralPresenter.working);
        working.subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean isWorking) {
                if (isWorking) {
                    busyShield.setVisibility(View.VISIBLE);
                } else {
                    busyShield.setVisibility(View.GONE);
                }
            }
        });

        final Observable<Boolean> connected = bind(peripheralPresenter.connected);
        connected.subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean isConnected) {
                if (isConnected) {
                    discoverServicesButton.setEnabled(true);
                    connectionButton.setText(R.string.action_disconnect);
                } else {
                    discoverServicesButton.setEnabled(false);
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

        final Observable<List<PeripheralService>> services = bind(peripheralPresenter.services);
        services.subscribe(new Action1<List<PeripheralService>>() {
            @Override
            public void call(List<PeripheralService> services) {
                adapter.bindServices(services);
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

    public void discoverServices(@NonNull View ignored) {
        presentError(peripheralPresenter.discoverServices());
    }

    @Override
    public void onAdvertisingRecordClick(int type, @NonNull String value) {

    }

    @Override
    public void onServiceClick(@NonNull PeripheralService service) {

    }
}
