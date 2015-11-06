package is.hello.buruberi.example.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
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
import is.hello.buruberi.example.util.DividerItemDecoration;
import rx.Observable;
import rx.functions.Action1;

/**
 * Allows the user to conduct a Bluetooth Low Energy peripheral scan,
 * and select peripherals to interact with.
 */
public class ScanActivity extends BaseActivity
        implements ScanResultsAdapter.OnItemClickListener {
    private static final int LOCATION_REQUEST_CODE = 0x1C;

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
        recyclerView.addItemDecoration(new DividerItemDecoration(getResources(), true));

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
        scanItem.setEnabled(!scanPresenter.isWorking());

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
        final int permissionStatus =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (permissionStatus == PermissionChecker.PERMISSION_GRANTED) {
            adapter.clear();
            scanPresenter.scan();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            showLocationPermissionExplanation();
        } else {
            requestLocationPermission();
        }
    }

    private void showLocationPermissionExplanation() {
        final AlertDialog.Builder explanation = new AlertDialog.Builder(this);
        explanation.setTitle(R.string.location_permission_explanation_title);
        explanation.setMessage(R.string.location_permission_explanation_message);
        explanation.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestLocationPermission();
            }
        });
        explanation.show();
    }

    private void requestLocationPermission() {
        final String[] permissions = { Manifest.permission.ACCESS_COARSE_LOCATION };
        ActivityCompat.requestPermissions(this, permissions, LOCATION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE &&
                grantResults[0] == PermissionChecker.PERMISSION_GRANTED) {
            scan();
        }
    }

    @Override
    public void onItemClick(@NonNull GattPeripheral peripheral) {
        peripheralPresenter.setPeripheral(peripheral);
        startActivity(new Intent(this, PeripheralActivity.class));
    }
}
