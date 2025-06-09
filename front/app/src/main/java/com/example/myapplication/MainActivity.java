package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;
import android.content.Intent;
import android.widget.ImageView;
import android.view.View;


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


import java.util.ArrayList;
import java.util.List;

import com.example.myapplication.ItemActivity;
import com.example.myapplication.ItemActivity.Item;

import com.example.myapplication.CustomMarkerView;


public class MainActivity extends AppCompatActivity {

    private BarChart barChart1;
    private BarChart barChart2;
    private BarChart barChart3;

    private RecyclerView recyclerViewActivities;
    private ItemActivity activityAdapter;
    private List<ItemActivity.Item> activityItems;

    private ArrayList<BarEntry> walkEntriesExample;
    private ArrayList<BarEntry> timeEntriesExample;
    private ArrayList<BarEntry> calorieEntriesExample;

    private ImageView cashIconImageView; // ✨ 이 줄이 꼭 있어야 해!

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        barChart1 = findViewById(R.id.barChart1);
        barChart2 = findViewById(R.id.barChart2);
        barChart3 = findViewById(R.id.barChart3);

        recyclerViewActivities = findViewById(R.id.recyclerViewActivities);

        walkEntriesExample = getExampleChartEntries(24, 100);
        timeEntriesExample = getExampleChartEntries(24, 60);
        calorieEntriesExample = getExampleChartEntries(24, 500);


        setupBarChart(barChart1, walkEntriesExample, Color.GREEN, "걸음 수");
        setupBarChart(barChart2, timeEntriesExample, Color.BLUE, "활동 시간");
        setupBarChart(barChart3, calorieEntriesExample, Color.parseColor("#FFC0CB"), "활동 칼로리");

        CustomMarkerView markerView1 = new CustomMarkerView(this, R.layout.custom_marker_view);
        markerView1.setChartView(barChart1);
        markerView1.setActivityLabel("걸음 수");
        barChart1.setMarkerView(markerView1);

        CustomMarkerView markerView2 = new CustomMarkerView(this, R.layout.custom_marker_view);
        markerView2.setChartView(barChart2);
        markerView2.setActivityLabel("활동 시간");
        barChart2.setMarkerView(markerView2);

        CustomMarkerView markerView3 = new CustomMarkerView(this, R.layout.custom_marker_view);
        markerView3.setChartView(barChart3);
        markerView3.setActivityLabel("활동 칼로리");
        barChart3.setMarkerView(markerView3);


        barChart1.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                barChart2.highlightValue(h.getX(), h.getDataSetIndex(), false);
                barChart3.highlightValue(h.getX(), h.getDataSetIndex(), false);
            }

            @Override
            public void onNothingSelected() {
                barChart2.highlightValue(null);
                barChart3.highlightValue(null);
            }
        });

        barChart2.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                barChart1.highlightValue(h.getX(), h.getDataSetIndex(), false);
                barChart3.highlightValue(h.getX(), h.getDataSetIndex(), false);
            }

            @Override
            public void onNothingSelected() {
                barChart1.highlightValue(null);
                barChart3.highlightValue(null);
            }
        });

        barChart3.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                barChart1.highlightValue(h.getX(), h.getDataSetIndex(), false);
                barChart2.highlightValue(h.getX(), h.getDataSetIndex(), false);
            }

            @Override
            public void onNothingSelected() {
                barChart1.highlightValue(null);
                barChart2.highlightValue(null);
            }
        });


        recyclerViewActivities.setLayoutManager(new LinearLayoutManager(this));
        activityItems = new ArrayList<>();
        activityAdapter = new ItemActivity(activityItems);
        recyclerViewActivities.setAdapter(activityAdapter);

        activityAdapter.addItem(new ItemActivity.Item("걷기(자동)", "00:01:00 | 0.1 km", "오후 3:00"));
        activityAdapter.addItem(new ItemActivity.Item("달리기", "00:10:00 | 1.5 km", "오후 4:00"));
        activityAdapter.addItem(new ItemActivity.Item("자전거", "00:30:00 | 5 km", "오후 5:00"));

        cashIconImageView = findViewById(R.id.cashIcon);

        cashIconImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FrontActivity.class);
                startActivity(intent);
            }
        });

    }


    private void setupBarChart(BarChart chart, ArrayList<BarEntry> entries, int barColor, String label) {
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

    private ArrayList<BarEntry> getExampleChartEntries(int numberOfHours, int maxValue) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < numberOfHours; i++) {
            float yValue = (float) (Math.random() * maxValue);
            entries.add(new BarEntry(i, yValue));
        }
        return entries;
    }
}
