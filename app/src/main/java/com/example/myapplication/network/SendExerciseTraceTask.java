package com.example.myapplication.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SendExerciseTraceTask {
    private static final String BASE_URL = "http://10.18.220.184:3000"; // 안드로이드 기기에서 테스트할 때, 호스트에 연결된 네트워크 IP 사용
//    private static final String BASE_URL = "http://10.0.2.2:3000"; // 안드로이드 스튜디오 가상 애뮬레이터(AVD)를 사용할 때는 10.0.2.2 사용
    private static final String ENDPOINT = "/exercise/trace";
    // JSON 타입 정의
    private static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    // 싱글톤으로 OkHttpClient 생성
    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 콜백 인터페이스 (기존 것 그대로)
    public interface Callback {
        void onSuccess(int httpCode, String responseBody);
        void onFailure(String errorMsg);
    }

    public static void sendRequest(
            String userId,
            float speedKmh,
            long timestamp,
            long stepInterval,
            Callback callback
    ) {
        // 1) 보낼 JSON 페이로드 생성
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("uid", userId);
            data.put("velocity", speedKmh);
            data.put("date", timestamp);
            data.put("movetime", stepInterval);

            String jsonString = new JSONObject(data).toString();
            RequestBody body = RequestBody.create(jsonString, JSON);

//            Log.d("SendExerciseTraceTask", );

            // 2) Request 빌드
            Request request = new Request.Builder()
                    .url(BASE_URL + ENDPOINT) // 실제 엔드포인트로 교체
                    .post(body)
                    .build();

            // 3) 비동기 호출
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String respBody = response.body() != null
                            ? response.body().string()
                            : "";

                    mainHandler.post(() -> {
                        if (response.isSuccessful()) {
                            callback.onSuccess(response.code(), respBody);
                        } else {
                            callback.onFailure(
                                    "HTTP " + response.code() + " - " + respBody
                            );
                        }
                    });
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    // 네트워크 오류 등
                    mainHandler.post(() -> {
                        callback.onFailure(e.getMessage());
                    });
                }
            });

        } catch (Exception e) {
            // JSON 변환 중 예외
            callback.onFailure(e.getMessage());
        }
    }
}
