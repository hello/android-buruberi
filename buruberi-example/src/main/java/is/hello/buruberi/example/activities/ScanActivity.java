package is.hello.buruberi.example.activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import java.util.List;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.example.R;
import is.hello.buruberi.example.adapters.ScanResultsAdapter;
import is.hello.buruberi.example.modules.Injection;
import is.hello.buruberi.example.presenters.ScanPresenter;
import is.hello.buruberi.util.Rx;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

public class ScanActivity extends AppCompatActivity {
    private static final Func1<Activity, Boolean> IS_VALID = new Func1<Activity, Boolean>() {
        @Override
        public Boolean call(Activity activity) {
            return !activity.isDestroyed();
        }
    };

    @Inject ScanPresenter scanPresenter;

    private ProgressBar progressBar;
    private ScanResultsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injection.inject(this);

        setContentView(R.layout.activity_scan);

        this.progressBar = (ProgressBar) findViewById(R.id.activity_scan_progress);

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.activity_scan_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        this.adapter = new ScanResultsAdapter(this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        final Observable<List<GattPeripheral>> peripherals = scanPresenter.peripherals
                .lift(new Rx.OperatorConditionalBinding<List<GattPeripheral>, Activity>(this, IS_VALID));
        peripherals.subscribe(new Action1<List<GattPeripheral>>() {
            @Override
            public void call(List<GattPeripheral> gattPeripherals) {
                adapter.setPeripherals(gattPeripherals);
                setScanning(false);
            }
        });

        setScanning(scanPresenter.isScanning());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scan: {

                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    private void setScanning(boolean isScanning) {
        if (isScanning) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }
}
