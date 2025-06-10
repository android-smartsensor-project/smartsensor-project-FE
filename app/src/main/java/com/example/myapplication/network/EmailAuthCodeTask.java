package com.example.myapplication.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.myapplication.utils.JsonUtils;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 백엔드의 POST /auth/verify 엔드포인트로
 * JSON 형식({ "email": "..." })을 보내는 비동기 요청 클래스 (API 31+ 환경용).
 *
 * - AsyncTask 대신 ExecutorService + Handler(메인 스레드) 조합을 사용합니다.
 */
public class EmailAuthCodeTask {
    private static final String TAG = "EmailAuthCode";

    private static final String BASE_URL = "http://10.0.2.2:3000";
    private static final String ENDPOINT = "/auth/verify";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());


    public interface Callback {
        /** HTTP 응답 코드가 2xx(성공)일 때 호출됩니다. */
        void onSuccess(int httpCode, String responseBody);

        /** 네트워크 오류나 4xx/5xx 응답 시 호출됩니다. */
        void onFailure(String errorMsg);
    }

    /**
     * 이메일 인증 요청을 비동기로 보냅니다.
     *
     * @param email    사용자 이메일 (예: "user@example.com")
     * @param mode     인증 모드 (예: "signup")
     * @param authNumber 인증번호
     * @param callback 성공/실패 결과를 받을 콜백
     */
    public static void sendRequest(final String email, final String mode, final String authNumber, final Callback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BASE_URL + ENDPOINT);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("email", email);
                jsonBody.put("mode", mode);
                jsonBody.put("authNumber", authNumber);
                String requestBody = jsonBody.toString();

                OutputStream out = new BufferedOutputStream(conn.getOutputStream());
                out.write(requestBody.getBytes("UTF-8"));
                out.flush();
                out.close();

                int responseCode = conn.getResponseCode();
                BufferedReader reader;
                if (responseCode >= 200 && responseCode < 300) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                }

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                final int finalHttpCode = responseCode;
                final String finalBody = sb.toString();
                final HashMap<String, Object> map = JsonUtils.jsonToMap(finalBody);

                mainHandler.post(() -> {
                    if (finalHttpCode >= 200 && finalHttpCode < 300) {
                        callback.onSuccess(finalHttpCode, finalBody);
                    } else {
                        callback.onFailure(map.get("message").toString());
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "이메일 인증번호 확인 중 예외 발생", e);
                final String errMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                mainHandler.post(() -> callback.onFailure(errMsg));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }
}
