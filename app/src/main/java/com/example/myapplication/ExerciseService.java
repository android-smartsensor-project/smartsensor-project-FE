package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.myapplication.network.SendExerciseTraceTask;
import com.example.myapplication.utils.JsonUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ExerciseService extends Service implements SensorEventListener {

    private static final String CHANNEL_ID = "ExerciseServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_START_EXERCISE = "com.example.myapplication.action.START_EXERCISE";
    public static final String ACTION_STOP_EXERCISE = "com.example.myapplication.action.STOP_EXERCISE";
    public static final String ACTION_STEP_UPDATE = "com.example.myapplication.action.STEP_UPDATE";
    public static final String EXTRA_POINTS = "extra_points";
    public static final String ACTION_EXERCISE_END = "com.example.myapplication.action.EXERCISE_END";
    public static final String EXTRA_LAST_SPEED_KMH = "extra_last_speed_kmh";
    public static final String EXTRA_DURATION_MILLIS = "extra_duration_millis";

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private NotificationManager manager;


    private float dailyPoints = 0;
    private long lastStepTime = 0;

    private Location lastKnownLocation = null;
    private Location previousLocation = null;
    private List<LatLng> pathPoints = new ArrayList<>();

    private long startTimeMillis = 0;
    private float lastCalculatedSpeedKmh = 0f;

    private SharedPreferences pointsPrefs;
    private static final String PREFS_NAME = "PointsPrefs";
    private static final String KEY_POINTS = "points";
    private static final String KEY_LAST_RESET_DATE = "lastResetDate";
    private static final FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
    private static final FirebaseUser mFirebaseUser = mFirebaseAuth.getCurrentUser();

    @Override
    public void onCreate() {
        super.onCreate();
        if (mFirebaseUser == null) {
            Log.d("ExerciseService", "User is not authenticated. Stopping service.");
            stopSelf();
            return;
        }
        pointsPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        checkAndResetDailySteps(); // 서비스 시작 시 날짜 확인 및 걸음 수 초기화

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
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
                        LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());
                        pathPoints.add(newPoint);
                        lastKnownLocation = location;
                        Log.d("ExerciseService", "Location updated: " + newPoint.latitude + ", " + newPoint.longitude);
                    }
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("ExerciseService", "Service started");

        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START_EXERCISE.equals(action)) {
                startExerciseTracking();
            } else if (ACTION_STOP_EXERCISE.equals(action)) {
                stopExerciseTracking();
                stopSelf();
            }
        }

        createNotificationChannel();
        Notification notification = buildNotification("운동 기록 중...", "적립 포인트: " + dailyPoints);
        startForeground(NOTIFICATION_ID, notification);

        sendStepUpdateBroadcast(dailyPoints); // 서비스 시작 시 현재 포인트 액티비티로 전송

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("ExerciseService", "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void checkAndResetDailySteps() {
        String lastResetDate = pointsPrefs.getString(KEY_LAST_RESET_DATE, "");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN);
        String todayDate = sdf.format(new Date());

        if (!todayDate.equals(lastResetDate)) {
            dailyPoints = 0;
            savePoints(0);
            pointsPrefs.edit().putString(KEY_LAST_RESET_DATE, todayDate).apply();
            Log.d("ExerciseService", "Daily steps reset for " + todayDate);
        } else {
            dailyPoints = pointsPrefs.getFloat(KEY_POINTS, 0);
            Log.d("ExerciseService", "Loaded steps for today (" + todayDate + "): " + dailyPoints);
        }
    }

    private void savePoints(float points) {
        pointsPrefs.edit().putFloat(KEY_POINTS, points).apply();
        Log.d("ExerciseService", "Step count saved: " + points);
    }

    private void startExerciseTracking() {
        Log.d("ExerciseService", "Exercise tracking started");
        pathPoints.clear();
        lastKnownLocation = null;
        previousLocation = null;
        startTimeMillis = System.currentTimeMillis();

        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_GAME);
            Log.d("ExerciseService", "Step counter listener registered");
        } else {
            Toast.makeText(this, "걷기 센서를 찾을 수 없습니다 ㅠㅠ 적립 불가", Toast.LENGTH_LONG).show();
            Log.w("ExerciseService", "Step counter sensor not found");
        }

        startLocationUpdates();
        updateNotification("운동 기록 중...", "포인트: " + dailyPoints);
    }

    private void stopExerciseTracking() {
        Log.d("ExerciseService", "Exercise tracking stopped");
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            Log.d("ExerciseService", "Step counter listener unregistered");
        }
        stopLocationUpdates();

        long endTimeMillis = System.currentTimeMillis();
        long durationMillis = endTimeMillis - startTimeMillis;

        sendExerciseEndBroadcast(lastCalculatedSpeedKmh, durationMillis);

        updateNotification("운동 기록 종료", "총 포인트: " + dailyPoints);

        pathPoints.clear();
        lastKnownLocation = null;
        previousLocation = null;
        lastCalculatedSpeedKmh = 0f;
        startTimeMillis = 0;

        savePoints(dailyPoints); // 운동 종료 시 걸음 수 저장
        manager.cancel(NOTIFICATION_ID);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("ExerciseService", "Location permissions not granted");
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        Log.d("ExerciseService", "Location updates requested");
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        Log.d("ExerciseService", "Location updates stopped");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                if (lastKnownLocation != null) {
                    if (previousLocation == null) {
                        Toast.makeText(ExerciseService.this, "첫 위치 저장", Toast.LENGTH_LONG).show();
                    }
                    if (previousLocation != null) {
                        long currentTime = System.currentTimeMillis();
                        float distance = previousLocation.distanceTo(lastKnownLocation);
                        long timeDiffMillis = lastKnownLocation.getTime() - previousLocation.getTime();

                        if (timeDiffMillis > 0) {
                            float speedMps = distance / (timeDiffMillis / 1000f);
                            float speedKmh = speedMps * 3.6f;
                            lastCalculatedSpeedKmh = speedKmh;
                            SendExerciseTraceTask.sendRequest(mFirebaseUser.getUid(), speedKmh, currentTime, currentTime - lastStepTime, new SendExerciseTraceTask.Callback() {
                                @Override
                                public void onSuccess(int httpCode, String responseBody) {
                                    Log.d("ExerciseService", "Exercise trace sent successfully");
                                    HashMap<String, Object> body = JsonUtils.jsonToMap(responseBody);
                                    if (body == null) {
                                        Toast.makeText(ExerciseService.this, "Exercise trace sent successfully", Toast.LENGTH_LONG).show();
                                        return ;
                                    }
                                    Object data = body.get("data");
                                    HashMap<String, Object> trace = JsonUtils.jsonToMap(data.toString());
                                    Object p = trace.get("points");
                                    float points;
                                    if (p instanceof Number) {
                                        points = ((Number)p).floatValue();
                                    } else if (p instanceof String) {
                                        // 숫자 문자열이라면 파싱
                                        points = Float.parseFloat((String)p);
                                    } else {
                                        // 예외 처리나 기본값
                                        throw new IllegalStateException("points 타입이 Number/String이 아닙니다: " + p);
                                    }
                                    dailyPoints += points;
                                    lastStepTime = currentTime;
                                }

                                @Override
                                public void onFailure(String errorMsg) {
                                    Log.e("ExerciseService", "Failed to send exercise trace: " + errorMsg);
                                    Toast.makeText(ExerciseService.this, "데이터 전송을 실패했습니다. 기기에 활동 기록을 임시로 보관하겠습니다", Toast.LENGTH_LONG).show();
                                    lastStepTime = currentTime;
                                }
                            });
                        }
                    }
                    previousLocation = lastKnownLocation;
                    sendStepUpdateBroadcast(dailyPoints);
                    updateNotification("운동 기록 중...", "적립 포인트: " + dailyPoints);
                } else {
                    Log.w("SpeedCalculation", "lastKnownLocation이 null입니다. 위치 정보 수신 대기 중.");
                }
            }
        } catch (Exception e) {
            Log.e("ExerciseService", "Error: " + e.getMessage());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "운동 기록 서비스 채널",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            } else {
                Toast.makeText(this, "NotificationManager is null", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Notification buildNotification(String title, String text) {
        Intent notificationIntent = new Intent(this, FrontActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void updateNotification(String title, String text) {
        Notification notification = buildNotification(title, text);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void sendStepUpdateBroadcast(float points) {
        Intent intent = new Intent(ACTION_STEP_UPDATE);
        intent.putExtra(EXTRA_POINTS, points);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d("ExerciseService", "Step update broadcast sent: " + points);
    }

    private void sendExerciseEndBroadcast(float lastSpeedKmh, long durationMillis) {
        Intent intent = new Intent(ACTION_EXERCISE_END);
        intent.putExtra(EXTRA_LAST_SPEED_KMH, lastSpeedKmh);
        intent.putExtra(EXTRA_DURATION_MILLIS, durationMillis);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d("ExerciseService", "Exercise end broadcast sent. Speed: " + lastSpeedKmh + " km/h, Duration: " + durationMillis + " ms");
    }
}