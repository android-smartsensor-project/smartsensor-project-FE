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
        int startHour = (int) e.getX();
        int endHour   = (startHour + 1) % 24;                                       // ▶ 수정
        String timeRange = String.format("%d시~%d시", startHour, endHour);

        // 2) 실제 값 가져오기 (float 으로)
        float val = e.getY();                                                        // ▶ 수정

        // 3) activityLabel 에 따라 단위와 포맷 분기
        String infoText;
        switch (activityLabel) {
            case "평균 속도":                                                       // ▶ 수정: MainActivity 에서 설정한 Label 과 일치시킵니다
                // 소수점 한 자리까지 km/h 로
                infoText = String.format("%.1f km/h", val);                         // ▶ 수정
                break;
            case "총 활동 시간":                                                   // ▶ 수정
                if (val < 1f) {
                    float seconds = val * 60f;
                    infoText = String.format("%.2f초 활동", seconds);
                } else {
                    infoText = String.format("%d분 활동", Math.round(val));
                }
                break;
            case "총 칼로리":                                                      // ▶ 수정
                // 정수 kcal 소모
                infoText = String.format("%dkcal 소모", Math.round(val));         // ▶ 수정
                break;
            default:
                // 혹시 다른 Label 이 들어오면 원래 텍스트로
                infoText = String.format("%s: %.1f", activityLabel, val);
        }

        // 4) 최종 문자열
        tvContent.setText(timeRange + "\n" + infoText);

        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return MPPointF.getInstance(-(getWidth() / 2f), -getHeight());
    }
}