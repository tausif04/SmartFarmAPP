package com.iot.smartFarming;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    private SwitchMaterial notificationSwitch;
    private SwitchMaterial wifiOnlySwitch;
    private RadioGroup themeRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        setupToolbar();
        initViews();
        setupClickListeners();
        loadCurrentSettings();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void initViews() {
        notificationSwitch = findViewById(R.id.notificationSwitch);
        wifiOnlySwitch = findViewById(R.id.wifiOnlySwitch);
        themeRadioGroup = findViewById(R.id.themeRadioGroup);
    }

    private void setupClickListeners() {
        // Sync frequency click listener
        findViewById(R.id.syncFrequency).setOnClickListener(v -> showSyncFrequencyDialog());

        // Theme radio group listener
        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.themeSystem) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            } else if (checkedId == R.id.themeLight) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else if (checkedId == R.id.themeDark) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        });

        // Logout button listener
        findViewById(R.id.btn_logout).setOnClickListener(v -> showLogoutConfirmation());
    }

    private void loadCurrentSettings() {
        // Load current settings from SharedPreferences
        notificationSwitch.setChecked(true); // Replace with actual preference value
        wifiOnlySwitch.setChecked(false);    // Replace with actual preference value

        // Set current theme
        int nightMode = AppCompatDelegate.getDefaultNightMode();
        switch (nightMode) {
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
                themeRadioGroup.check(R.id.themeSystem);
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                themeRadioGroup.check(R.id.themeDark);
                break;
            default:
                themeRadioGroup.check(R.id.themeLight);
        }
    }

    private void showSyncFrequencyDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Sync Frequency")
                .setItems(new String[]{"5 minutes", "15 minutes", "30 minutes", "1 hour"}, (dialog, which) -> {
                    String[] values = {"5 min", "15 min", "30 min", "1 hr"};
                    TextView syncValue = findViewById(R.id.syncFrequencyValue);
                    syncValue.setText(values[which]);
                    // Save selection to SharedPreferences here
                })
                .show();
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, Login.class));
        finishAffinity(); // Close all activities
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Save settings here if needed
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}