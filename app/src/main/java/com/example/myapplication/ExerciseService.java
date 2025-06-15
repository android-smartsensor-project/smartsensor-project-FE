package com.example.myapplication;

import static android.content.ContentValues.TAG;

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
import android.os.Handler;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ExerciseService extends Service implements SensorEventListener {

    private static final String BASE_URL = "http://10.18.220.184:3000";  // 실제 엔드포인트
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient = new OkHttpClient();

    private static final String CHANNEL_ID = "ExerciseServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_START_EXERCISE = "com.example.myapplication.action.START_EXERCISE";
    public static final String ACTION_STOP_EXERCISE = "com.example.myapplication.action.STOP_EXERCISE";
    public static final String ACTION_STEP_UPDATE = "com.example.myapplication.action.STEP_UPDATE";
    public static final String EXTRA_POINTS = "extra_points";
    public static final String ACTION_EXERCISE_END = "com.example.myapplication.action.EXERCISE_END";
    public static final String EXTRA_LAST_SPEED_KMH = "extra_last_speed_kmh";
    public static final String EXTRA_DURATION_MILLIS = "extra_duration_millis";
    private static final String PREFS_SERVICE = "ExerciseServicePrefs";
    private static final String KEY_RUNNING = "isServiceRunning";
    public static final String EXTRA_AVG_SPEED       = "extra_avg_speed";
    public static final String EXTRA_SESSION_POINTS = "extra_session_points";
    public static final String EXTRA_KCAL            = "extra_kcal";
    private float pointsAtSessionStart;
    private SharedPreferences servicePrefs;


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
        servicePrefs = getSharedPreferences(PREFS_SERVICE, Context.MODE_PRIVATE);

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
        createNotificationChannel();
        startForeground(NOTIFICATION_ID,
                buildNotification("운동 기록 중...", "포인트: " + dailyPoints));

        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START_EXERCISE.equals(action)) {
                syncStartExercise();
            } else if (ACTION_STOP_EXERCISE.equals(action)) {
                stopExerciseTracking();
            }
        }

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

    /** 운동 시작 API를 동기 호출하고, HTTP 상태에 따라 로그/서비스 중단 처리 */
    private void syncStartExercise() {
        new Thread(() -> {
            RequestBody body = RequestBody.create(
                    "{\"uid\":\"" + mFirebaseUser.getUid() + "\"}", JSON);
            Request req = new Request.Builder()
                    .url(BASE_URL + "/exercise/start")
                    .post(body)
                    .build();

            try (Response resp = httpClient.newCall(req).execute()) {
                // 1) 본문 전체를 문자열로 읽어두고
                String respBodyStr = resp.body().string();

                // 2) JSONObject로 파싱
                JSONObject root = new JSONObject(respBodyStr);
                int apiStatus = root.optInt("statusCode", resp.code());

                if (apiStatus == 200) {
                    // 3) 본문에서 message나 data가 필요하면 추가로 읽을 수 있음
                    new Handler(Looper.getMainLooper()).post(this::startExerciseTracking);
                } else {
                    String msg = root.optString("message",
                            "운동 시작 실패: HTTP " + resp.code());
                    Log.e(TAG, msg);
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    );
                    getSystemService(NotificationManager.class)
                            .cancel(NOTIFICATION_ID);
                    stopSelf();
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "운동 시작 API 호출 예외", e);
                new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(this, "운동 시작 중 네트워크 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
                );
                getSystemService(NotificationManager.class)
                        .cancel(NOTIFICATION_ID);
                stopSelf();
            }
        }).start();
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
        pointsAtSessionStart = dailyPoints;    // <- 세션 시작 시점 저장
        servicePrefs.edit().putBoolean(KEY_RUNNING, true).apply();
        updateNotification("운동 기록 중...", "포인트: " + dailyPoints);

        // **추가**: 시작 성공 알림 브로드캐스트
        Intent started = new Intent("com.example.myapplication.action.EXERCISE_STARTED");
        LocalBroadcastManager.getInstance(this).sendBroadcast(started);
    }

    private void stopExerciseTracking() {
        Log.d("ExerciseService", "Exercise tracking stopped");
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            Log.d("ExerciseService", "Step counter listener unregistered");
        }
        stopLocationUpdates();
        servicePrefs.edit().putBoolean(KEY_RUNNING, false).apply();

        long duration = System.currentTimeMillis() - startTimeMillis;
        float sessionPts = dailyPoints - pointsAtSessionStart;

        // 3) 서버에 종료 API 동기 호출 (별도 스레드)
        // 서버 종료 API + 브로드캐스트 + 후처리
        new Thread(() -> {
            RequestBody body = RequestBody.create(
                    "{\"uid\":\"" + mFirebaseUser.getUid() + "\"}", JSON);
            Request req = new Request.Builder()
                    .url(BASE_URL + "/exercise/finish")
                    .post(body)
                    .build();

            try (Response resp = httpClient.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    Log.e(TAG, "finish error: " + resp.code());
                    Log.e(TAG, "finish error: " + resp.message());
                    return;
                }

                JSONObject data = new JSONObject(resp.body().string())
                        .getJSONObject("data");
                float avgSpeed = (float) data.getDouble("velocity");
                float pts      = (float) data.getDouble("points");
                float kcal     = (float) data.getDouble("kcal");

                Intent br = new Intent(ACTION_EXERCISE_END)
                        .putExtra(EXTRA_SESSION_POINTS, pts)
                        .putExtra(EXTRA_AVG_SPEED, avgSpeed)
                        .putExtra(EXTRA_KCAL, kcal)
                        .putExtra(EXTRA_DURATION_MILLIS, duration);
                LocalBroadcastManager.getInstance(this)
                        .sendBroadcast(br);
            } catch (Exception e) {
                Log.e(TAG, "finish API fail", e);
            } finally {
                // UI 토스트·노티·저장·종료는 이곳에서 한 번만
                servicePrefs.edit().putBoolean(KEY_RUNNING, false).apply();
                savePoints(dailyPoints);
                sendStepUpdateBroadcast(dailyPoints);
                getSystemService(NotificationManager.class)
                        .cancel(NOTIFICATION_ID);
                stopSelf();
            }
        }).start();

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
                            SendExerciseTraceTask.sendRequest(mFirebaseUser.getUid(), speedKmh, currentTime, timeDiffMillis, new SendExerciseTraceTask.Callback() {
                                @Override
                                public void onSuccess(int httpCode, String responseBody) {
                                    Log.d("ExerciseService", "Exercise trace sent successfully");
                                    Log.d("ExerciseService", responseBody);
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
                                        Log.d("ExerciseService", "points 타입: " + p.getClass().getSimpleName());
                                        points = ((Number)p).floatValue();
                                    } else if (p instanceof String) {
                                        Log.d("ExerciseService", "points 타입: " + p.getClass().getSimpleName());
                                        points = Float.parseFloat((String)p);
                                    } else {
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
}