package com.iot.smartFarming;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import android.view.View;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    private CardView cardAgro, cardCattle, cardPoultry, cardFish;
    private FirebaseAuth auth;
    private DatabaseReference databaseRef;
    private Map<String, ValueEventListener> eventListeners = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        checkNetworkConnection();
        setupFirebaseAuth();
        initializeViews();
        setupFirebaseListeners();
        setupClickListeners();
    }

    private void initializeViews() {
        cardAgro = findViewById(R.id.card_agro);
        cardCattle = findViewById(R.id.card_cattle);
        cardPoultry = findViewById(R.id.card_poultry);
        cardFish = findViewById(R.id.card_fish);
    }

    private void setupFirebaseAuth() {
        auth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference();
        if (auth.getCurrentUser() == null) {
            redirectToLogin();
        }
    }

    private void setupFirebaseListeners() {
        setupFarmListener("AgroFarm");
        setupFarmListener("CattleFarm");
        setupFarmListener("PoultryFarm");
        setupFarmListener("FishFarm");
    }

    private void setupFarmListener(String farmPath) {
        Query query = databaseRef.child(farmPath)
                .orderByKey()
                .limitToLast(1);

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    if (snapshot.exists()) {
                        for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                            JSONObject farmData = new JSONObject(childSnapshot.getValue().toString());
                            handleFarmData(farmPath, farmData);
                        }
                    } else {
                        handleNullData(farmPath);
                    }
                } catch (JSONException | NullPointerException e) {
                    handleNullData(farmPath);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError("Database error: " + error.getMessage());
            }
        };

        eventListeners.put(farmPath, listener);
        query.addValueEventListener(listener);
    }

    private void handleFarmData(String farmPath, JSONObject farmData) {
        // Data handling if needed
    }

    private void handleNullData(String farmPath) {
        runOnUiThread(() -> {
            if (isNetworkAvailable()) {
                showError("No data available for " + farmPath);
            } else {
                showError("Offline mode - data not updated");
            }
        });
    }

    private void setupClickListeners() {
        cardAgro.setOnClickListener(v -> navigateToFarm(FarmType.AGRO));
        cardCattle.setOnClickListener(v -> navigateToFarm(FarmType.CATTLE));
        cardPoultry.setOnClickListener(v -> navigateToFarm(FarmType.POULTRY));
        cardFish.setOnClickListener(v -> navigateToFarm(FarmType.FISH));
    }

    private void navigateToFarm(FarmType farmType) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Offline mode - displaying last cached data",
                    Toast.LENGTH_SHORT).show();
        }
        startActivity(createFarmIntent(farmType));
    }

    private Intent createFarmIntent(FarmType farmType) {
        Class<?> activityClass;
        switch (farmType) {
            case AGRO:
                activityClass = AgroFarmActivity.class;
                break;
            case CATTLE:
                activityClass = CattleFarmActivity.class;
                break;
            case POULTRY:
                activityClass = PoultryFarmActivity.class;
                break;
            case FISH:
                activityClass = FishFarmActivity.class;
                break;
            default:
                throw new IllegalArgumentException("Invalid farm type");
        }
        return new Intent(this, activityClass);
    }

    private void checkNetworkConnection() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection - showing cached data",
                    Toast.LENGTH_LONG).show();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void redirectToLogin() {
        startActivity(new Intent(this, Login.class));
        finish();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private enum FarmType { AGRO, CATTLE, POULTRY, FISH }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (Map.Entry<String, ValueEventListener> entry : eventListeners.entrySet()) {
            databaseRef.child(entry.getKey()).removeEventListener(entry.getValue());
        }
        eventListeners.clear();
    }
}