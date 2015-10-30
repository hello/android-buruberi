package is.hello.buruberi.example.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import javax.inject.Inject;

import is.hello.buruberi.example.R;
import is.hello.buruberi.example.modules.Injection;
import is.hello.buruberi.example.presenters.ScanPresenter;

public class ScanActivity extends AppCompatActivity {
    @Inject ScanPresenter scanPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injection.inject(this);

        setContentView(R.layout.activity_scan);

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.activity_scan_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
}
