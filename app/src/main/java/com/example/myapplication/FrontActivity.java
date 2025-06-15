package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.widget.ImageView;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.UiSettings;

import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.CameraUpdate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.lang.Math;
import android.location.Location;
import android.app.PendingIntent;
import java.util.concurrent.TimeUnit;
import android.graphics.Color;

public class FrontActivity extends AppCompatActivity implements com.google.android.gms.maps.OnMapReadyCallback, com.naver.maps.map.OnMapReadyCallback {

    private TextView dateTextView;
    private TextView timeTextView;
    private TextView currentStepsTextView;
    private TextView maxGoalTextView;
    private ProgressBar progressBar;
    private Button buttonChangeMap;
    private Button buttonStartExercise;
    private Button buttonEndExercise;
    private ImageView imageViewRabbit;
    private TextView textViewStepBubble;

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
    private static final String PREFS_SERVICE = "ExerciseServicePrefs";
    private static final String KEY_RUNNING = "isServiceRunning";
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

    private BroadcastReceiver startReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            // prefs는 이미 true로 바뀌어 있으므로
            updateButtonsByServiceState();
        }
    };

    private BroadcastReceiver stepUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ExerciseService.ACTION_STEP_UPDATE.equals(intent.getAction())) {
                float points = intent.getFloatExtra(ExerciseService.EXTRA_POINTS, 0);
                updateStepUI(points);
            }
        }
    };

    private BroadcastReceiver exerciseEndReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ExerciseService.ACTION_EXERCISE_END.equals(intent.getAction())) {
                float avgSpeed       = intent.getFloatExtra(ExerciseService.EXTRA_AVG_SPEED, 0f);
                float sessionPoints  = intent.getFloatExtra(ExerciseService.EXTRA_SESSION_POINTS, 0f);
                float kcal           = intent.getFloatExtra(ExerciseService.EXTRA_KCAL, 0f);
                long  durationMillis = intent.getLongExtra(ExerciseService.EXTRA_DURATION_MILLIS, 0L);

                long hours   = TimeUnit.MILLISECONDS.toHours(durationMillis);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60;
                long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60;
                String timeStr = String.format(Locale.KOREAN, "%02d시간 %02d분 %02d초", hours, minutes, seconds);

                String message =
                        "획득 포인트: " + sessionPoints + "\n" +
                                "평균 속도: "   + String.format(Locale.KOREAN, "%.1f km/h", avgSpeed) + "\n" +
                                "소모 칼로리: " + String.format(Locale.KOREAN, "%.0f kcal", kcal) + "\n" +
                                "운동 시간: "  + timeStr;
                new AlertDialog.Builder(FrontActivity.this)
                        .setTitle("운동 종료")
                        .setMessage(message)
                        .setPositiveButton("확인", (d, w) -> {
                            d.dismiss();
                            resetExerciseUI();
                        })
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();
            }
        }
    };

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
        buttonStartExercise = findViewById(R.id.buttonStartExercise);
        buttonEndExercise = findViewById(R.id.buttonEndExercise);
        imageViewRabbit = findViewById(R.id.imageViewRabbit);
        textViewStepBubble = findViewById(R.id.textViewStepBubble);
        updateButtonsByServiceState();

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

        if (buttonStartExercise != null) {
            buttonStartExercise.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkRequiredPermissions();
                }
            });
        }

        if (buttonEndExercise != null) {
            buttonEndExercise.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    endExercise();
                }
            });
        }

        if (buttonStartExercise != null && buttonEndExercise != null) {
            buttonStartExercise.setVisibility(View.VISIBLE);
            buttonEndExercise.setVisibility(View.GONE);
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

        updateDateTime();
        textViewStepBubble.setText("준비 중");
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateButtonsByServiceState();
        handler.post(updateTimeRunnable);

        LocalBroadcastManager.getInstance(this).registerReceiver(stepUpdateReceiver,
                new IntentFilter(ExerciseService.ACTION_STEP_UPDATE));
        Log.d("FrontActivity", "StepUpdateReceiver registered");

        LocalBroadcastManager.getInstance(this).registerReceiver(exerciseEndReceiver,
                new IntentFilter(ExerciseService.ACTION_EXERCISE_END));
        Log.d("FrontActivity", "ExerciseEndReceiver registered");

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(startReceiver,
                        new IntentFilter("com.example.myapplication.action.EXERCISE_STARTED"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateTimeRunnable);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(stepUpdateReceiver);
        Log.d("FrontActivity", "StepUpdateReceiver unregistered");

        LocalBroadcastManager.getInstance(this).unregisterReceiver(exerciseEndReceiver);
        Log.d("FrontActivity", "ExerciseEndReceiver unregistered");

        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(startReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

    private void updateStepUI(float points) {
        try {
            currentStepsTextView.setText(String.valueOf(points));
            progressBar.setProgress((int)points);
            updateRabbitAndBubblePosition();
            Log.d("FrontActivity", "UI updated with points: " + points);
        } catch (Exception e) {
            Log.e("FrontActivity", "Error: " + e.getMessage());
        }
    }

    private void updateRabbitAndBubblePosition() {
        if (progressBar == null || imageViewRabbit == null || textViewStepBubble == null) {
            return;
        }

        if (progressBar.getWidth() == 0 || imageViewRabbit.getWidth() == 0 || textViewStepBubble.getWidth() == 0) {
            progressBar.post(this::updateRabbitAndBubblePosition);
            return;
        }

        final int progressBarWidth = progressBar.getWidth();
        final int maxProgress = progressBar.getMax();
        final int currentProgress = progressBar.getProgress();

        if (progressBarWidth <= 0 || maxProgress <= 0) {
            Log.w("RabbitPosition", "ProgressBar width or max is 0. Cannot update rabbit position.");
            return;
        }

        float progressRatio = (float) currentProgress / maxProgress;

        final int rabbitWidth = imageViewRabbit.getWidth();
        final int bubbleWidth = textViewStepBubble.getWidth();

        float idealRabbitTranslationX = (progressBarWidth * progressRatio) - (rabbitWidth / 2f);

        float minRabbitTranslationX = 0;
        float maxRabbitTranslationX = progressBarWidth - rabbitWidth;
        float clampedRabbitTranslationX = Math.max(minRabbitTranslationX, Math.min(idealRabbitTranslationX, maxRabbitTranslationX));

        final float spacingDp = 4f;
        final float density = getResources().getDisplayMetrics().density;
        final float spacingPx = spacingDp * density;

        float idealBubbleTranslationX = clampedRabbitTranslationX + rabbitWidth + spacingPx;

        float minBubbleTranslationX = 0;
        float maxBubbleTranslationX = progressBarWidth - bubbleWidth;
        float clampedBubbleTranslationX = Math.max(minBubbleTranslationX, Math.min(idealBubbleTranslationX, maxBubbleTranslationX));

        imageViewRabbit.setTranslationX(clampedRabbitTranslationX);
        textViewStepBubble.setTranslationX(clampedBubbleTranslationX);

        Log.d("RabbitPosition", "Progress: " + currentProgress + ", Width: " + progressBarWidth + ", RabbitWidth: " + rabbitWidth + ", BubbleWidth: " + bubbleWidth + ", ClampedRabbitX: " + clampedRabbitTranslationX + ", ClampedBubbleX: " + clampedBubbleTranslationX);
    }

    private void updateButtonsByServiceState() {
        SharedPreferences servicePrefs =
                getSharedPreferences(PREFS_SERVICE, Context.MODE_PRIVATE);
        boolean isRunning = servicePrefs.getBoolean(KEY_RUNNING, false);

        if (isRunning) {
            buttonStartExercise.setVisibility(View.GONE);
            buttonEndExercise.setVisibility(View.VISIBLE);
            textViewStepBubble.setText("운동 중…");
        } else {
            buttonStartExercise.setVisibility(View.VISIBLE);
            buttonEndExercise.setVisibility(View.GONE);
            textViewStepBubble.setText("준비 중");
        }
    }

    private void startExercise() {
        Intent serviceIntent = new Intent(this, ExerciseService.class);
        serviceIntent.setAction(ExerciseService.ACTION_START_EXERCISE);
        ContextCompat.startForegroundService(this, serviceIntent);

        pathPoints.clear();
        updateMapPolyline();
    }

    private void endExercise() {
        Intent serviceIntent = new Intent(this, ExerciseService.class);
        serviceIntent.setAction(ExerciseService.ACTION_STOP_EXERCISE);
        startService(serviceIntent);

        Toast.makeText(this, "운동 종료 중...", Toast.LENGTH_SHORT).show();
    }

    private void checkRequiredPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION);
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_LOCATION_PERMISSION);
        } else {
            startExerciseAndLocationUpdates();
        }
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
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                Toast.makeText(this, "필요한 모든 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
                startExerciseAndLocationUpdates();

            } else {
                Toast.makeText(this, "필요한 권한이 거부되었습니다. 일부 기능을 사용할 수 없습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startExerciseAndLocationUpdates() {
        startExercise();
        checkLocationPermissions();
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

                fusedLocationClient.requestLocationUpdates(locationRequest,
                        locationCallback,
                        Looper.getMainLooper());
                Log.d("LocationUpdate", "Location updates started.");
            } else {
                Log.e("LocationUpdate", "Permission ACCESS_FINE_LOCATION not granted when attempting to start updates.");
            }
        });

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(FrontActivity.this,
                            REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    Log.e("LocationUpdate", "Error starting resolution for location settings", sendEx);
                }
            } else {
                Log.e("LocationUpdate", "Error checking location settings", e);
            }
        });
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        Log.d("LocationUpdate", "Location updates stopped.");
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        Log.d("MapReady", "Google Map is ready.");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                com.google.android.gms.maps.model.LatLng currentLocation = new com.google.android.gms.maps.model.LatLng(location.getLatitude(), location.getLongitude());
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                                Log.d("MapReady", "Moved camera to last known location.");
                            } else {
                                Log.d("MapReady", "Last known location is null.");
                            }
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("MapReady", "Error getting last known location", e);
                        }
                    });
        } else {
            Log.w("MapReady", "Location permission not granted for Google Map MyLocation layer.");
        }
    }

    @Override
    public void onMapReady(@NonNull NaverMap map) {
        naverMap = map;
        Log.d("MapReady", "Naver Map is ready.");

        naverMap.setLocationSource(naverLocationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        if (!pathPoints.isEmpty()) {
            naverMap.moveCamera(CameraUpdate.scrollTo(new com.naver.maps.geometry.LatLng(pathPoints.get(pathPoints.size() - 1).latitude, pathPoints.get(pathPoints.size() - 1).longitude)));
        }
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
                Toast.makeText(this, "위치 설정을 켜야 경로를 표시할 수 있습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showMapChoiceDialog() {
        Log.d("FrontActivity", "showMapChoiceDialog called. Implementation needed.");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("지도 선택")
                .setItems(new CharSequence[]{"Google 지도", "Naver 지도"}, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String selectedMapType = (which == 0) ? MAP_TYPE_GOOGLE : MAP_TYPE_NAVER;
                        mapPrefs.edit().putString(KEY_MAP_TYPE, selectedMapType).apply();
                        loadMapFragment(selectedMapType);
                    }
                });
        builder.create().show();
    }

    private void loadMapFragment(String mapType) {
        Log.d("FrontActivity", "loadMapFragment called with type: " + mapType + ". Implementation needed.");
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        Fragment existingFragment = fragmentManager.findFragmentById(R.id.map_container);
        if (existingFragment != null) {
            fragmentTransaction.remove(existingFragment);
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
        } else {
            Toast.makeText(this, "지도 로드 실패!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateMapPolyline() {
        Log.d("FrontActivity", "updateMapPolyline called.");

        if (pathPoints.size() >= 2) {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.map_container);

            if (currentFragment instanceof SupportMapFragment && googleMap != null) {
                if (googlePolyline != null) {
                    googlePolyline.remove();
                }
                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(pathPoints)
                        .color(android.graphics.Color.RED)
                        .width(10);
                googlePolyline = googleMap.addPolyline(polylineOptions);
                Log.d("MapUpdate", "Google Map polyline updated with " + pathPoints.size() + " points.");

            } else if (currentFragment instanceof MapFragment && naverMap != null) {
                if (naverPolylineOverlay != null) {
                    naverPolylineOverlay.setMap(null);
                }
                List<com.naver.maps.geometry.LatLng> naverPathPoints = new ArrayList<>();
                for (com.google.android.gms.maps.model.LatLng googleLatLng : pathPoints) {
                    naverPathPoints.add(new com.naver.maps.geometry.LatLng(googleLatLng.latitude, googleLatLng.longitude));
                }
                naverPolylineOverlay = new PolylineOverlay();
                naverPolylineOverlay.setCoords(naverPathPoints);
                naverPolylineOverlay.setColor(android.graphics.Color.RED);
                naverPolylineOverlay.setWidth(10);
                naverPolylineOverlay.setMap(naverMap);
                Log.d("MapUpdate", "Naver Map polyline updated with " + pathPoints.size() + " points.");
            }
        } else {
            if (googlePolyline != null) {
                googlePolyline.remove();
                googlePolyline = null;
                Log.d("MapUpdate", "Removed Google Map polyline (less than 2 points).");
            }
            if (naverPolylineOverlay != null) {
                naverPolylineOverlay.setMap(null);
                naverPolylineOverlay = null;
                Log.d("MapUpdate", "Removed Naver Map polyline (less than 2 points).");
            }
            Log.d("MapUpdate", "Not enough points (" + pathPoints.size() + ") to draw polyline.");
        }
    }

    private void resetExerciseUI() {
        if (buttonStartExercise != null && buttonEndExercise != null) {
            buttonStartExercise.setVisibility(View.VISIBLE);
            buttonEndExercise.setVisibility(View.GONE);
        }
        textViewStepBubble.setText("운동 종료");
        updateStepUI(0);
        pathPoints.clear();
        updateMapPolyline();
        Log.d("FrontActivity", "Exercise UI reset.");
    }
}