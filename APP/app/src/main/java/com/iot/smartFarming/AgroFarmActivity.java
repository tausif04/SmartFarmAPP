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

public class AgroFarmActivity extends AppCompatActivity {

    // UI Components
    private TextView temperatureValue, humidityValue, moistureValue, statusText;
    private LineChart sensorChart;

    // Firebase
    private DatabaseReference agroRef;
    private ValueEventListener agroListener;

    // Sensor Data
    private double currentTemperature = 0;
    private double currentHumidity = 0;
    private double currentMoisture = 0;

    // Chart Data
    private final ArrayList<Entry> tempEntries = new ArrayList<>();
    private final ArrayList<Entry> humidityEntries = new ArrayList<>();
    private final ArrayList<Entry> moistureEntries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agro_farm);

        // Enable hardware acceleration
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
        agroRef = FirebaseDatabase.getInstance().getReference("AgroFarm");
        setupFirebaseListener();
    }

    private void setupFirebaseListener() {
        Query query = agroRef.orderByKey().limitToLast(1);

        agroListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    if (snapshot.exists()) {
                        // Get the latest pushID entry
                        Iterator<DataSnapshot> iterator = snapshot.getChildren().iterator();
                        if (iterator.hasNext()) {
                            DataSnapshot latestEntry = iterator.next();
                            JSONObject farmData = new JSONObject(latestEntry.getValue().toString());

                            currentTemperature = farmData.optDouble("temperature", 0.0);
                            currentHumidity = farmData.optDouble("humidity", 0.0);
                            currentMoisture = farmData.optDouble("moisture", 0.0);
                        }
                    } else {
                        initializeDefaultValues();
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
        query.addValueEventListener(agroListener);
    }

    private void initializeViews() {
        temperatureValue = findViewById(R.id.temperatureValue);
        humidityValue = findViewById(R.id.humidityValue);
        moistureValue = findViewById(R.id.moistureValue);
        statusText = findViewById(R.id.statusText);
        sensorChart = findViewById(R.id.sensorChart);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Configure navigation button

        // Handle settings button click
        ImageView settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v ->
                startActivity(new Intent(AgroFarmActivity.this, SettingsActivity.class))
        );
    }

    private void setupAnimations() {
        animateCardEntrance(R.id.temperatureCard);
        animateCardEntrance(R.id.humidityCard);
        animateCardEntrance(R.id.moistureCard);
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
            String moisture = String.format(Locale.US, "%.1f%%", currentMoisture);

            updateTextView(temperatureValue, temp);
            updateTextView(humidityValue, humidity);
            updateTextView(moistureValue, moisture);

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
            // Limit to 100 data points
            if (tempEntries.size() > 100) tempEntries.remove(0);
            if (humidityEntries.size() > 100) humidityEntries.remove(0);
            if (moistureEntries.size() > 100) moistureEntries.remove(0);

            tempEntries.add(new Entry(tempEntries.size(), (float) currentTemperature));
            humidityEntries.add(new Entry(humidityEntries.size(), (float) currentHumidity));
            moistureEntries.add(new Entry(moistureEntries.size(), (float) currentMoisture));

            LineDataSet tempDataSet = createDataSet(tempEntries, "Temperature", Color.RED);
            LineDataSet humidityDataSet = createDataSet(humidityEntries, "Humidity", Color.BLUE);
            LineDataSet moistureDataSet = createDataSet(moistureEntries, "Moisture", Color.GREEN);

            LineData lineData = new LineData(tempDataSet, humidityDataSet, moistureDataSet);
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
            moistureValue.setText("--");
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
        if (agroRef != null && agroListener != null) {
            agroRef.removeEventListener(agroListener);
        }
    }
}