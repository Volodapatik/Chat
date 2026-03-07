package com.example.floatingai;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity {

    private SwitchMaterial enableSwitch;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enableSwitch = findViewById(R.id.enableSwitch);
        statusText = findViewById(R.id.statusText);

        enableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    requestOverlayPermission();
                    enableSwitch.setChecked(false);
                    return;
                }
                startFloatingService();
                statusText.setText(getString(R.string.status_on));
                Toast.makeText(this, "AI Assistant activated", Toast.LENGTH_SHORT).show();
            } else {
                stopFloatingService();
                statusText.setText(getString(R.string.status_off));
                Toast.makeText(this, "AI Assistant deactivated", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean running = FloatingViewService.isRunning();
        enableSwitch.setChecked(running);
        statusText.setText(running ? getString(R.string.status_on) : getString(R.string.status_off));
    }

    private void requestOverlayPermission() {
        Toast.makeText(this, "Please enable overlay permission", Toast.LENGTH_LONG).show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void startFloatingService() {
        Intent intent = new Intent(this, FloatingViewService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void stopFloatingService() {
        Intent intent = new Intent(this, FloatingViewService.class);
        stopService(intent);
    }
}
