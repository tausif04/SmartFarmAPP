package com.iot.smartFarming;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;

public class CattleFarmActivity extends AppCompatActivity {

    // UI Components
    private TextView temperatureValue, humidityValue, waterLevelValue, statusText;
    private LineChart sensorChart;

    // Firebase
    private DatabaseReference cattleRef;
    private ValueEventListener cattleListener;

    // Sensor Data
    private double currentTemperature = 0;
    private double currentHumidity = 0;
    private double currentWaterLevel = 0;
    private final Random random = new Random();

    // Chart Data
    private final ArrayList<Entry> tempEntries = new ArrayList<>();
    private final ArrayList<Entry> humidityEntries = new ArrayList<>();
    private final ArrayList<Entry> waterLevelEntries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cattle_farm);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        );

        initializeViews();
        setupToolbar();
        setupChart();
        setupAnimations();
        setupFirebase();
    }

    private void setupFirebase() {
        cattleRef = FirebaseDatabase.getInstance().getReference("CattleFarm");
        setupFirebaseListener();
    }

    private void setupFirebaseListener() {
        Query query = cattleRef.orderByKey().limitToLast(1);

        cattleListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    if (snapshot.exists()) {
                        Iterator<DataSnapshot> iterator = snapshot.getChildren().iterator();
                        if (iterator.hasNext()) {
                            DataSnapshot latestEntry = iterator.next();
                            JSONObject farmData = new JSONObject(latestEntry.getValue().toString());

                            currentTemperature = farmData.optDouble("temperature", 0.0);
                            currentHumidity = farmData.optDouble("humidity", 0.0);
                        }
                        // Generate simulated water level (0-100L)
                        currentWaterLevel = random.nextDouble() * 100;
                    }
                    updateUI();
                    updateChart();
                } catch (JSONException | NullPointerException e) {
                    showError("Data processing error");
                    initializeDefaultValues();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError("Database error: " + error.getMessage());
            }
        };
        query.addValueEventListener(cattleListener);
    }

    private void initializeViews() {
        temperatureValue = findViewById(R.id.temperatureValue);
        humidityValue = findViewById(R.id.humidityValue);
        waterLevelValue = findViewById(R.id.waterLevelValue);
        statusText = findViewById(R.id.statusText);
        sensorChart = findViewById(R.id.sensorChart);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Only settings button setup remains
        ImageView settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v ->
                startActivity(new Intent(CattleFarmActivity.this, SettingsActivity.class))
        );
    }

    private void setupAnimations() {
        animateCardEntrance(R.id.temperatureCard);
        animateCardEntrance(R.id.humidityCard);
        animateCardEntrance(R.id.waterLevelCard);
    }

    private void animateCardEntrance(int viewId) {
        View card = findViewById(viewId);
        card.setAlpha(0f);
        card.setTranslationY(50f);
        card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(300)
                .start();
    }



    private void updateUI() {
        runOnUiThread(() -> {
            String temp = String.format(Locale.US, "%.1f°C", currentTemperature);
            String humidity = String.format(Locale.US, "%.1f%%", currentHumidity);
            String waterLevel = String.format(Locale.US, "%.1fL", currentWaterLevel);

            updateTextView(temperatureValue, temp);
            updateTextView(humidityValue, humidity);
            updateTextView(waterLevelValue, waterLevel);

            String status = "Last updated: " + new SimpleDateFormat("hh:mm a",
                    Locale.getDefault()).format(new Date());
            updateStatus(status);
        });
    }

    private void updateTextView(TextView textView, String value) {
        TransitionManager.beginDelayedTransition(
                (ViewGroup) textView.getParent(),
                new AutoTransition().setDuration(300)
        );
        textView.setText(value);
    }

    private void updateStatus(String message) {
        statusText.setText(message);
        statusText.setTextColor(ContextCompat.getColor(this, R.color.green_600));
    }

    private void setupChart() {
        sensorChart.setBackgroundColor(Color.WHITE);
        sensorChart.getDescription().setEnabled(false);
        sensorChart.setDrawGridBackground(false);
        sensorChart.setTouchEnabled(true);
        sensorChart.setPinchZoom(true);

        Legend legend = sensorChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setTextColor(Color.DKGRAY);
        legend.setTextSize(12f);

        XAxis xAxis = sensorChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.GRAY);
        xAxis.setAxisLineColor(Color.LTGRAY);
        xAxis.setGranularity(1f);

        YAxis leftAxis = sensorChart.getAxisLeft();
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setAxisLineColor(Color.LTGRAY);
        leftAxis.setGridColor(Color.parseColor("#EEEEEE"));
        sensorChart.getAxisRight().setEnabled(false);
    }

    private void updateChart() {
        try {
            if (tempEntries.size() > 100) tempEntries.remove(0);
            if (humidityEntries.size() > 100) humidityEntries.remove(0);
            if (waterLevelEntries.size() > 100) waterLevelEntries.remove(0);

            tempEntries.add(new Entry(tempEntries.size(), (float) currentTemperature));
            humidityEntries.add(new Entry(humidityEntries.size(), (float) currentHumidity));
            waterLevelEntries.add(new Entry(waterLevelEntries.size(), (float) currentWaterLevel));

            LineDataSet tempDataSet = createDataSet(tempEntries, "Temperature", Color.RED);
            LineDataSet humidityDataSet = createDataSet(humidityEntries, "Humidity", Color.BLUE);
            LineDataSet waterLevelDataSet = createDataSet(waterLevelEntries, "Water Level", Color.GREEN);

            LineData lineData = new LineData(tempDataSet, humidityDataSet, waterLevelDataSet);
            sensorChart.setData(lineData);
            sensorChart.animateY(500, Easing.EaseInOutQuad);
            sensorChart.invalidate();
        } catch (Exception e) {
            showError("Chart update error");
        }
    }

    private LineDataSet createDataSet(ArrayList<Entry> entries, String label, int color) {
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawValues(false);
        return dataSet;
    }

    private void initializeDefaultValues() {
        runOnUiThread(() -> {
            temperatureValue.setText("--");
            humidityValue.setText("--");
            waterLevelValue.setText("--");
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            statusText.setTextColor(ContextCompat.getColor(this, R.color.red_600));
            statusText.setText(message);
        });
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cattleRef != null && cattleListener != null) {
            cattleRef.removeEventListener(cattleListener);
        }
    }
}