package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;
import android.content.Intent;
import android.view.View;
import android.util.Log;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.data.Entry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;
import org.json.JSONException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Call;
import okhttp3.Callback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {
    private static final String BASE_URL = "http://10.18.220.184:3000";
    private BarChart barChart1, barChart2, barChart3;
    private RecyclerView recyclerViewActivities;
    private ItemActivity activityAdapter;
    private List<ItemActivity.Item> activityItems;
    private FirebaseAuth mAuth;

    // API로부터 파싱한 데이터를 시간별로 담아둘 맵
    private Map<Integer, List<Record>> recordsByHour = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // --- 차트 & 리사이클러뷰 초기화 ---
        barChart1 = findViewById(R.id.barChart1);
        barChart2 = findViewById(R.id.barChart2);
        barChart3 = findViewById(R.id.barChart3);

        recyclerViewActivities = findViewById(R.id.recyclerViewActivities);
        recyclerViewActivities.setLayoutManager(new LinearLayoutManager(this));
        activityItems = new ArrayList<>();
        activityAdapter = new ItemActivity(activityItems);
        recyclerViewActivities.setAdapter(activityAdapter);

        // --- 차트 클릭 리스너 (시간대별 레코드 표시) ---
        OnChartValueSelectedListener listener = new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int hour = (int)e.getX();
                displayRecordsForHour(hour);
                // 하이라이트 동기화 (세 차트 모두)
                barChart1.highlightValue(h);
                barChart2.highlightValue(h);
                barChart3.highlightValue(h);
            }
            @Override
            public void onNothingSelected() {
                activityItems.clear();
                activityAdapter.notifyDataSetChanged();
                barChart1.highlightValue(null);
                barChart2.highlightValue(null);
                barChart3.highlightValue(null);
            }
        };

        // ▶ 여기에 Marker 설정을 추가합니다 ◀
        CustomMarkerView mv1 = new CustomMarkerView(MainActivity.this, R.layout.custom_marker_view);
        mv1.setActivityLabel("평균 속도");
        barChart1.setMarker(mv1);

        CustomMarkerView mv2 = new CustomMarkerView(MainActivity.this, R.layout.custom_marker_view);
        mv2.setActivityLabel("총 활동 시간");
        barChart2.setMarker(mv2);

        CustomMarkerView mv3 = new CustomMarkerView(MainActivity.this, R.layout.custom_marker_view);
        mv3.setActivityLabel("총 칼로리");
        barChart3.setMarker(mv3);

        barChart1.setOnChartValueSelectedListener(listener);
        barChart2.setOnChartValueSelectedListener(listener);
        barChart3.setOnChartValueSelectedListener(listener);

        // --- 맨 위 프로필·캐시 버튼 (기존 코드) ---
        findViewById(R.id.profileIcon).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });
        findViewById(R.id.cashIcon).setOnClickListener(v -> {
            startActivity(new Intent(this, FrontActivity.class));
        });
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // 로그인 상태가 아니면 로그인 화면으로 (또는 토스트 후 종료)
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String uid = currentUser.getUid();
        Log.d("MainActivity", "User ID: " + uid);

        // --- API 콜 시작 ---
        fetchExerciseInfo(uid);
    }

    /**
     * 1) REST API 호출
     * 2) JSON 파싱 → recordsByHour 에 적재
     * 3) 시간별 BarEntry 생성 → setupBarChart 호출
     */
    private void fetchExerciseInfo(String userId) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(BASE_URL + "/exercise/info/" + userId)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this,
                                "네트워크 에러: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                try {
                    JSONObject json = new JSONObject(body);
                    int statusCode = json.getInt("statusCode");
                    String message   = json.getString("message");

                    if (statusCode == 200) {
                        JSONObject dataObj = json.optJSONObject("data");
                        if (dataObj == null || dataObj.length() == 0) {
                            // 기록이 아예 없을 때
                            runOnUiThread(() -> {
                                clearAllChartsAndList();
                            });
                            return;
                        }

                        // 시간별 맵 초기화
                        recordsByHour.clear();
                        Iterator<String> keys = dataObj.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();  // ex) "161718193"
                            JSONObject rec = dataObj.getJSONObject(key);
                            double velocity = rec.getDouble("velocity");
                            double points   = rec.getDouble("points");
                            double kcal     = rec.getDouble("kcal");
                            long   interval = rec.getLong("interval");

                            int hour = Integer.parseInt(key.substring(0,2));
                            recordsByHour
                                    .computeIfAbsent(hour, k->new ArrayList<>())
                                    .add(new Record(key, velocity, points, kcal, interval));
                        }

                        // BarEntry 리스트 준비
                        ArrayList<BarEntry> speedEntries   = new ArrayList<>();
                        ArrayList<BarEntry> timeEntries    = new ArrayList<>();
                        ArrayList<BarEntry> calorieEntries = new ArrayList<>();

                        for (int h = 0; h < 24; h++) {
                            List<Record> recs = recordsByHour.get(h);
                            if (recs != null && !recs.isEmpty()) {
                                float sumV=0, sumMin=0, sumK=0;
                                for (Record r: recs) {
                                    sumV    += r.velocity;
                                    sumMin  += r.interval/60f;
                                    sumK    += r.kcal;
                                }
                                speedEntries  .add(new BarEntry(h, sumV/recs.size()));
                                timeEntries   .add(new BarEntry(h, sumMin));
                                calorieEntries.add(new BarEntry(h, sumK));
                            } else {
                                // 값이 없으면 0으로
                                speedEntries  .add(new BarEntry(h, 0f));
                                timeEntries   .add(new BarEntry(h, 0f));
                                calorieEntries.add(new BarEntry(h, 0f));
                            }
                        }

                        runOnUiThread(() -> {
                            setupBarChart(barChart1, speedEntries,   Color.GREEN,           "평균 속도");
                            setupBarChart(barChart2, timeEntries,    Color.BLUE,            "총 활동 시간");
                            setupBarChart(barChart3, calorieEntries, Color.parseColor("#FFC0CB"), "총 칼로리");

                            YAxis timeLeft = barChart2.getAxisLeft();
                            timeLeft.setAxisMinimum(0f);
                            timeLeft.setAxisMaximum(60f);
                            barChart2.invalidate();
                        });

                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this,
                                        message, Toast.LENGTH_SHORT).show()
                        );
                    }
                } catch (JSONException e) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "파싱 에러: "+e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    /** 차트 초기화 로직 (기존과 동일) */
    private void setupBarChart(BarChart chart,
                               ArrayList<BarEntry> entries,
                               int barColor,
                               String label) {
        chart.setDoubleTapToZoomEnabled(false);
        chart.setPinchZoom(false);
        chart.setScaleEnabled(false);

        chart.setBackgroundColor(Color.WHITE);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);

        BarDataSet dataSet = new BarDataSet(entries, label);
        dataSet.setColor(barColor);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawValues(false);

        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);
        BarData data = new BarData(dataSets);
        data.setBarWidth(0.9f);

        chart.setData(data);
        chart.setFitBars(true);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineColor(Color.BLACK);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(24, false);

        String[] timeLabels = new String[24];
        for(int i=0; i<24; i++) {
            timeLabels[i] = "";
        }
        timeLabels[0] = "오전 12";
        timeLabels[6] = "오전 6";
        timeLabels[12] = "오후 12";
        timeLabels[18] = "오후 6";

        xAxis.setValueFormatter(new IndexAxisValueFormatter(timeLabels));
        xAxis.setLabelRotationAngle(0);
        xAxis.setYOffset(10f);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(false);
        leftAxis.setDrawAxisLine(true);
        leftAxis.setAxisLineColor(Color.WHITE);
        leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        chart.animateY(800);
        chart.invalidate();
    }

    /** 선택된 시(hour)에 해당하는 레코드를 RecyclerView에 표시 */
    private void displayRecordsForHour(int hour) {
        activityItems.clear();
        List<Record> recs = recordsByHour.get(hour);
        if (recs != null) {
            for (Record r: recs) {
                // 1) 속도 라벨
                String speedLabel = String.format("%.1f km/h", r.velocity);
                // 2) 포인트 | 칼로리
                String detail = String.format("%.2f포인트 | %.1f kcal", r.points, r.kcal);
                // 3) 시간 ("HH:MM:SS")
                String t = r.key.substring(0,2) + ":" +
                        r.key.substring(2,4) + ":" +
                        r.key.substring(4,6);
                activityItems.add(new ItemActivity.Item(speedLabel, detail, t));
            }
        }
        activityAdapter.notifyDataSetChanged();
    }

    /** 차트·리스트 완전 초기화 */
    private void clearAllChartsAndList() {
        barChart1.clear(); barChart2.clear(); barChart3.clear();
        activityItems.clear();
        activityAdapter.notifyDataSetChanged();
    }

    /** 내부 데이터 모델 */
    private static class Record {
        String  key;
        double  velocity;
        double  points;
        double  kcal;
        long    interval;
        Record(String key, double v, double p, double k, long i){
            this.key=key; velocity=v; points=p; kcal=k; interval=i;
        }
    }
}
