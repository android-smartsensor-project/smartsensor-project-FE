package com.example.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileActivity extends AppCompatActivity {

    private ImageButton btnHome;
    private TextView tvName, tvEmail, tvBirthdate, tvGender;
    private TextView tvDailyPoints, tvMonthPoints, tvAccumulatedCash, tvProfileDescription;
    private Button btnLogout;
    private TextView btnWithdraw;
    private AlertDialog loadingDialog;
    private OkHttpClient client;
    private String uid;
    private static final String BASE_URL = "http://10.18.220.184:3000"; // TODO: 실제 서버 URL로 변경

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

        client = new OkHttpClient();
        setupLoadingDialog();

        // 뷰 초기화
        btnHome = findViewById(R.id.btnHome);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvBirthdate = findViewById(R.id.tvBirthdate);
        tvGender = findViewById(R.id.tvGender);
        tvDailyPoints = findViewById(R.id.tvDailyPoints);
        tvMonthPoints = findViewById(R.id.tvMonthPoints);
        tvAccumulatedCash = findViewById(R.id.tvAccumulatedCash);
        tvProfileDescription = findViewById(R.id.tvProfileDescription);
        btnLogout = findViewById(R.id.btnLogout);
        btnWithdraw = findViewById(R.id.btnWithdraw);

        // Firebase 유저 가져오기
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            uid = currentUser.getUid();
            fetchUserInfo();
        } else {
            navigateToLogin();
        }

        // 홈 버튼 -> MainActivity
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // 로그아웃
        btnLogout.setOnClickListener(v -> performLogout());

        // 회원탈퇴: 확인 다이얼로그
        btnWithdraw.setOnClickListener(v -> {
            new AlertDialog.Builder(ProfileActivity.this)
                    .setMessage("정말 회원탈퇴를 하시겠습니까?")
                    .setPositiveButton("네", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            performWithdraw();
                        }
                    })
                    .setNegativeButton("아니오", null)
                    .show();
        });
    }

    private void setupLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ProgressBar progressBar = new ProgressBar(this);
        builder.setView(progressBar);
        builder.setCancelable(false);
        loadingDialog = builder.create();
    }

    // 유저 정보 조회
    private void fetchUserInfo() {
        loadingDialog.show();
        String url = BASE_URL + "/users/info/" + uid;
        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    Toast.makeText(ProfileActivity.this, "네트워크 에러", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    try {
                        JSONObject json = new JSONObject(body);
                        int statusCode = json.getInt("statusCode");
                        if (statusCode == 200) {
                            JSONObject data = json.getJSONObject("data");
                            String name = data.getString("name");
                            tvName.setText("이름: " + name);
                            tvEmail.setText("이메일: " + data.getString("email"));
                            tvBirthdate.setText("생년월일: " + data.getString("birth"));
                            String sex = data.getString("sex");
                            tvGender.setText("성별: " + ("M".equalsIgnoreCase(sex) ? "남" : "여"));
                            tvDailyPoints.setText("일일 포인트: " + data.getInt("dailyPoints"));
                            tvMonthPoints.setText("월 포인트: " + data.getInt("monthPoints"));
                            tvAccumulatedCash.setText("적립 캐쉬: " + data.getInt("cashes"));

                            int minGet = data.getInt("minGetPoint");
                            int maxGet = data.getInt("maxGetPoint");
                            String desc = String.format(
                                    "%s 님의 나이와 성별을 고려하여\n%dkm/h 이상부터 1포인트 이상 적립,\n%dkm/h 까지 최대 2포인트를 적립할 수 있습니다.",
                                    name, minGet, maxGet
                            );
                            tvProfileDescription.setText(desc);
                        } else {
                            Toast.makeText(ProfileActivity.this, json.optString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(ProfileActivity.this, "파싱 에러", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // 로그아웃 처리
    private void performLogout() {
        loadingDialog.show();
        String url = BASE_URL + "/users/doing/" + uid;
        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    Toast.makeText(ProfileActivity.this, "네트워크 에러", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    try {
                        JSONObject json = new JSONObject(body);
                        if (json.getInt("statusCode") == 200) {
                            boolean doing = json.getJSONObject("data").getBoolean("doing");
                            if (!doing) {
                                FirebaseAuth.getInstance().signOut();
                                navigateToLogin();
                            } else {
                                Toast.makeText(ProfileActivity.this,
                                        "운동 중에는 로그아웃할 수 없습니다.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ProfileActivity.this, json.optString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(ProfileActivity.this, "파싱 에러", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // 회원탈퇴 처리
    private void performWithdraw() {
        loadingDialog.show();
        String url = BASE_URL + "/users/user";
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("uid", uid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), jsonBody.toString());
        Request request = new Request.Builder().url(url).delete(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    Toast.makeText(ProfileActivity.this, "네트워크 에러", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bodyStr = response.body().string();
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    try {
                        JSONObject json = new JSONObject(bodyStr);
                        if (json.getInt("statusCode") == 200) {
                            Toast.makeText(ProfileActivity.this, json.optString("message"), Toast.LENGTH_SHORT).show();
                            FirebaseAuth.getInstance().signOut();
                            navigateToLogin();
                        } else {
                            Toast.makeText(ProfileActivity.this, json.optString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(ProfileActivity.this, "파싱 에러", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // 로그인 화면으로 이동
    private void navigateToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}