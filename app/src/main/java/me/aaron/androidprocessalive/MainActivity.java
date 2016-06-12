package me.aaron.androidprocessalive;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import me.aaron.androidprocessalive.service.ForegroundService;
import me.aaron.androidprocessalive.service.GrayService;
import me.aaron.androidprocessalive.service.NormalService;
import me.aaron.androidprocessalive.service.NotifyService;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initViews();
    }

    private void initViews() {
        findViewById(R.id.btn_start_normal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NormalService.actionStart(MainActivity.this);
            }
        });

        findViewById(R.id.btn_start_foreground).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ForegroundService.actionStart(MainActivity.this);
            }
        });

        findViewById(R.id.btn_start_gray).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GrayService.actionStart(MainActivity.this);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        NotifyService.actionStart(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
