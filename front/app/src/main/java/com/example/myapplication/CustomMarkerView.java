package com.example.myapplication;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

public class CustomMarkerView extends MarkerView {

    private TextView tvContent;
    private String activityLabel = ""; // 운동량 라벨을 저장할 변수 추가

    public CustomMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvContent);
    }

    // 외부에서 운동량 라벨을 설정할 수 있는 메소드 추가
    public void setActivityLabel(String label) {
        this.activityLabel = label;
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int hour = (int) e.getX(); // 시간 (0~23)
        int activityValue = (int) e.getY(); // 운동량 값 (정수로 형변환)

        // 시간대 정보 문자열
        String timeRange = hour + "시 ~ " + (hour + 1) + "시";

        // 운동량 정보 문자열 생성
        String activityInfo = activityValue + " " + activityLabel.replace("수", "").replace("시간", " 활동").replace("칼로리", " 소모"); // 라벨에 따라 단위를 붙여줌

        // 최종적으로 MarkerView에 표시할 텍스트 설정 (시간대와 운동량 정보를 줄 바꿈으로 연결)
        tvContent.setText(timeRange + "\n" + activityInfo);

        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        // MarkerView가 막대 위 중앙에 오도록 오프셋 설정
        return MPPointF.getInstance(-(getWidth() / 2), -getHeight());
    }
}
