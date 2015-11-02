package is.hello.buruberi.example.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import is.hello.buruberi.example.presenters.PeripheralPresenter;
import is.hello.buruberi.example.presenters.ScanPresenter;
import rx.Observable;
import rx.functions.Action1;

public class ScanActivity extends BaseActivity
        implements ScanResultsAdapter.OnItemClickListener {
    @Inject ScanPresenter scanPresenter;
    @Inject PeripheralPresenter peripheralPresenter;

    private MenuItem scanItem;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private ScanResultsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        this.progressBar = (ProgressBar) findViewById(R.id.activity_scan_progress);

        this.recyclerView = (RecyclerView) findViewById(R.id.activity_scan_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        this.adapter = new ScanResultsAdapter(this, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        final Observable<List<GattPeripheral>> peripherals = bind(scanPresenter.peripherals);
        peripherals.subscribe(new Action1<List<GattPeripheral>>() {
            @Override
            public void call(List<GattPeripheral> gattPeripherals) {
                adapter.addPeripherals(gattPeripherals);
            }
        });

        final Observable<Boolean> working = bind(scanPresenter.working);
        working.subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean working) {
                setWorking(working);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan, menu);
        this.scanItem = menu.findItem(R.id.action_scan);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scan: {
                scan();
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        scanItem.setEnabled(!scanPresenter.isScanning());

        return super.onPrepareOptionsMenu(menu);
    }

    private void setWorking(boolean isScanning) {
        if (isScanning) {
            recyclerView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }

        supportInvalidateOptionsMenu();
    }

    private void scan() {
        adapter.clear();
        scanPresenter.scan();
    }

    @Override
    public void onItemClick(int position, @NonNull GattPeripheral peripheral) {
        peripheralPresenter.setPeripheral(peripheral);
        startActivity(new Intent(this, PeripheralActivity.class));
    }
}
