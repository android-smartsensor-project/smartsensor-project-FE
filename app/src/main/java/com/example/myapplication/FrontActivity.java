package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.location.Priority;

import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.util.FusedLocationSource;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.lang.Math;
import android.location.Location;

public class FrontActivity extends AppCompatActivity implements SensorEventListener, com.google.android.gms.maps.OnMapReadyCallback, com.naver.maps.map.OnMapReadyCallback {

    private TextView dateTextView;
    private TextView timeTextView;
    private TextView currentStepsTextView;
    private TextView maxGoalTextView;
    private ProgressBar progressBar;
    private Button buttonChangeMap;

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;

    private int currentDailySteps = 0;
    private float[] accelerometerValues = new float[3];
    private static final double STEP_THRESHOLD = 10.5;
    private static final double LOW_THRESHOLD = 9.5;
    private boolean isPeakDetected = false;
    private long lastStepTime = 0;
    private static final long MIN_STEP_INTERVAL_NS = 250 * 1000000;

    private Handler handler = new Handler();
    private Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            updateDateTime();
            handler.postDelayed(this, 1000);
        }
    };

    private static final String PREFS_NAME = "MapPrefs";
    private static final String KEY_MAP_TYPE = "mapType";
    private static final String MAP_TYPE_GOOGLE = "Google";
    private static final String MAP_TYPE_NAVER = "Naver";
    private SharedPreferences mapPrefs;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private List<com.google.android.gms.maps.model.LatLng> pathPoints = new ArrayList<>();

    private GoogleMap googleMap;
    private NaverMap naverMap;

    private Polyline googlePolyline;
    private PolylineOverlay naverPolylineOverlay;

    private static final int REQUEST_LOCATION_PERMISSION = 100;
    private static final int REQUEST_CHECK_SETTINGS = 200;

    private FusedLocationSource naverLocationSource;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.front);

        mapPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        dateTextView = findViewById(R.id.dateTextView);
        timeTextView = findViewById(R.id.timeTextView);
        currentStepsTextView = findViewById(R.id.currentStepsTextView);
        maxGoalTextView = findViewById(R.id.maxGoalTextView);
        progressBar = findViewById(R.id.progressBar);
        buttonChangeMap = findViewById(R.id.buttonChangeMap);

        progressBar.setMax(10000);
        maxGoalTextView.setText(String.valueOf(10000));

        if (buttonChangeMap != null) {
            buttonChangeMap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showMapChoiceDialog();
                }
            });
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateIntervalMillis(500)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        com.google.android.gms.maps.model.LatLng newPoint = new com.google.android.gms.maps.model.LatLng(location.getLatitude(), location.getLongitude());
                        pathPoints.add(newPoint);

                        Log.d("LocationUpdate", "New Location: " + newPoint.latitude + ", " + newPoint.longitude);

                        updateMapPolyline();
                    }
                }
            }
        };

        naverLocationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        String savedMapType = mapPrefs.getString(KEY_MAP_TYPE, null);
        if (savedMapType == null) {
            showMapChoiceDialog();
        } else {
            loadMapFragment(savedMapType);
        }

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometerSensor == null) {
                Toast.makeText(this, "가속도 센서를 찾을 수 없습니다 ㅠㅠ", Toast.LENGTH_LONG).show();
            }
        }

        updateDateTime();

        checkLocationPermissions();
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (naverLocationSource != null) {
            naverLocationSource.onRequestPermissionsResult(
                    requestCode, permissions, grantResults);
        }

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "위치 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
                startLocationUpdates();
            } else {
                Toast.makeText(this, "위치 권한이 거부되어 경로를 표시할 수 없습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        Log.d("LocationUpdate", "Attempting to start location updates.");

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> {
            Log.d("LocationUpdate", "Location settings check successful. Requesting updates.");
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                LocationRequest testLocationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500)
                        .setMinUpdateIntervalMillis(250)
                        .build();

                fusedLocationClient.requestLocationUpdates(testLocationRequest,
                        locationCallback,
                        Looper.getMainLooper());
                Log.d("LocationUpdate", "Location updates started.");
            }
        });

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(FrontActivity.this,
                            REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                }
            }
        });
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        Log.d("LocationUpdate", "Location updates stopped.");
    }

    private void updateMapPolyline() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.map_container);

        if (currentFragment instanceof SupportMapFragment && googleMap != null) {
            if (pathPoints.size() > 1) {
                if (googlePolyline != null) {
                    googlePolyline.remove();
                }
                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(pathPoints)
                        .color(android.graphics.Color.RED)
                        .width(10);

                googlePolyline = googleMap.addPolyline(polylineOptions);
            }
        } else if (currentFragment instanceof MapFragment && naverMap != null) {
            if (pathPoints.size() > 1) {
                List<com.naver.maps.geometry.LatLng> naverPathPoints = new ArrayList<>();
                for (com.google.android.gms.maps.model.LatLng googleLatLng : pathPoints) {
                    naverPathPoints.add(new com.naver.maps.geometry.LatLng(googleLatLng.latitude, googleLatLng.longitude));
                }

                if (naverPolylineOverlay != null) {
                    naverPolylineOverlay.setMap(null);
                }

                naverPolylineOverlay = new PolylineOverlay();
                naverPolylineOverlay.setCoords(naverPathPoints);
                naverPolylineOverlay.setColor(android.graphics.Color.RED);
                naverPolylineOverlay.setWidth(10);
                naverPolylineOverlay.setMap(naverMap);
            }
        }
    }

    private void showMapChoiceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("사용할 지도 선택");
        builder.setItems(new CharSequence[]{"Google 지도", "Naver 지도"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String selectedMapType;
                if (which == 0) {
                    selectedMapType = MAP_TYPE_GOOGLE;
                } else {
                    selectedMapType = MAP_TYPE_NAVER;
                }
                mapPrefs.edit().putString(KEY_MAP_TYPE, selectedMapType).apply();
                loadMapFragment(selectedMapType);
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void loadMapFragment(String mapType) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        Fragment existingFragment = fragmentManager.findFragmentById(R.id.map_container);
        if (existingFragment != null) {
            fragmentTransaction.remove(existingFragment);
            if (existingFragment instanceof SupportMapFragment) {
                googleMap = null;
                googlePolyline = null;
            } else if (existingFragment instanceof MapFragment) {
                naverMap = null;
                naverPolylineOverlay = null;
            }
        }

        Fragment newFragment = null;
        if (MAP_TYPE_GOOGLE.equals(mapType)) {
            newFragment = new SupportMapFragment();
            ((SupportMapFragment) newFragment).getMapAsync(this);
            Toast.makeText(this, "Google 지도를 로드합니다.", Toast.LENGTH_SHORT).show();
        } else if (MAP_TYPE_NAVER.equals(mapType)) {
            newFragment = new MapFragment();
            ((MapFragment) newFragment).getMapAsync(this);
            Toast.makeText(this, "Naver 지도를 로드합니다.", Toast.LENGTH_SHORT).show();
        }

        if (newFragment != null) {
            fragmentTransaction.add(R.id.map_container, newFragment);
            fragmentTransaction.commit();
            pathPoints.clear();
        } else {
            Toast.makeText(this, "지도 로드 실패!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        Log.d("MapReady", "Google Map is ready.");
    }

    @Override
    public void onMapReady(@NonNull NaverMap map) {
        naverMap = map;
        Log.d("MapReady", "Naver Map is ready.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        handler.post(updateTimeRunnable);
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        handler.removeCallbacks(updateTimeRunnable);
        stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            double magnitude = Math.sqrt(x * x + y * y + z * z);

            long currentTime = System.nanoTime();

            if (magnitude > STEP_THRESHOLD && !isPeakDetected && (currentTime - lastStepTime > MIN_STEP_INTERVAL_NS)) {
                isPeakDetected = true;
            }
            else if (magnitude < LOW_THRESHOLD && isPeakDetected) {
                currentDailySteps++;
                isPeakDetected = false;
                lastStepTime = currentTime;
            }

            currentStepsTextView.setText(String.valueOf(currentDailySteps));
            progressBar.setProgress(currentDailySteps);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void updateDateTime() {
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN);
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        String dateString = dateFormat.format(now);
        dateTextView.setText(dateString);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.KOREAN);
        timeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        String timeString = timeFormat.format(now);
        timeTextView.setText(timeString);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                Log.d("LocationUpdate", "User enabled location settings.");
                startLocationUpdates();
            } else {
                Log.d("LocationUpdate", "User did not enable location settings.");
                Toast.makeText(this, "위치 설정이 꺼져 있어 경로를 표시할 수 없습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }
}