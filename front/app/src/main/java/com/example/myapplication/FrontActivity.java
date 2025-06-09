package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.lang.Math;
import android.util.Log; // Log 찍기 위해 추가!

public class FrontActivity extends AppCompatActivity implements SensorEventListener {

    private TextView dateTextView;
    private TextView timeTextView;
    private TextView currentStepsTextView;
    private TextView maxGoalTextView;
    private ProgressBar progressBar;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.front);

        dateTextView = findViewById(R.id.dateTextView);
        timeTextView = findViewById(R.id.timeTextView);
        currentStepsTextView = findViewById(R.id.currentStepsTextView);
        maxGoalTextView = findViewById(R.id.maxGoalTextView);
        progressBar = findViewById(R.id.progressBar);

        progressBar.setMax(10000);
        maxGoalTextView.setText(String.valueOf(10000));

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if (sensorManager != null) {
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            if (accelerometerSensor == null) {
                Toast.makeText(this, "가속도 센서를 찾을 수 없습니다 ㅠㅠ", Toast.LENGTH_LONG).show();
            }
        }

        updateDateTime();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        handler.post(updateTimeRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        handler.removeCallbacks(updateTimeRunnable);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // 센서 값 로그 찍기! 이걸로 값이 들어오는지 확인해봐!
            Log.d("SensorTest", "X: " + x + ", Y: " + y + ", Z: " + z);
            Log.d("SensorTest", "Magnitude: " + Math.sqrt(x*x + y*y + z*z));

            double magnitude = Math.sqrt(x * x + y * y + z * z);

            long currentTime = System.nanoTime();

            if (magnitude > STEP_THRESHOLD && !isPeakDetected && (currentTime - lastStepTime > MIN_STEP_INTERVAL_NS)) {
                isPeakDetected = true;
            }
            else if (magnitude < LOW_THRESHOLD && isPeakDetected) {
                // 이 로그가 찍히면 걸음이 하나 세진다는 뜻이야!
                Log.d("SensorTest", "Step Detected! Current Steps: " + (currentDailySteps + 1));
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
}