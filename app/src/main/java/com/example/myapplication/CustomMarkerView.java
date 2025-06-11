package com.example.myapplication;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

public class CustomMarkerView extends MarkerView {

    private TextView tvContent;
    private String activityLabel = "";

    public CustomMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvContent);
    }

    public void setActivityLabel(String label) {
        this.activityLabel = label;
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int hour = (int) e.getX();
        int activityValue = (int) e.getY();

        String timeRange = hour + "시 ~ " + (hour + 1) + "시";

        String activityInfo;
        if ("걸음 수".equals(activityLabel)) {
            activityInfo = activityValue + " km/s";
        } else if ("활동 시간".equals(activityLabel)) {
            activityInfo = activityValue + " 분 활동";
        } else if ("활동 칼로리".equals(activityLabel)) {
            activityInfo = activityValue + " 칼로리 소모";
        } else {
            activityInfo = activityLabel + ": " + activityValue;
        }

        tvContent.setText(timeRange + "\n" + activityInfo);

        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return MPPointF.getInstance(-(getWidth() / 2), -getHeight());
    }
}