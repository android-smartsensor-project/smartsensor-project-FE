package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler; // Handler 추가

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {
    private FirebaseAuth mFirebaseAuth;
    private static final int SPLASH_DISPLAY_LENGTH = 2000; // 2초 딜레이 (밀리초 단위)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash); // splash.xml 레이아웃 설정

        mFirebaseAuth = FirebaseAuth.getInstance();

        // Handler를 사용해서 일정 시간 뒤에 다음 액티비티로 이동
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 딜레이 시간이 지난 후 실행될 코드
                if (mFirebaseAuth.getCurrentUser() != null) {
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );
                    startActivity(intent);
                }
                finish(); // 현재 액티비티(스플래시) 종료
            }
        }, SPLASH_DISPLAY_LENGTH); // 설정한 딜레이 시간만큼 기다림
    }
}