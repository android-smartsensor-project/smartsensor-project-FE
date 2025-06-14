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

public class SendExerciseTraceTask {
    private static final String BASE_URL = "http://10.18.220.184:3000";
    private static final String ENDPOINT = "/exercise/trace";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(int httpCode, String responseBody);
        void onFailure(String errorMsg);
    }
    public static void sendRequest(final String uid, final float velocity, final long date, final long movetime, final SendExerciseTraceTask.Callback callback) {
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
                jsonBody.put("uid", uid);
                jsonBody.put("velocity", velocity);
                jsonBody.put("date", date);
                jsonBody.put("movetime", movetime);
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
