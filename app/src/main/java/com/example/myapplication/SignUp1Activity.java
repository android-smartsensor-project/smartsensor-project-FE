package com.example.myapplication;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.network.EmailAuthRequestTask;
import com.example.myapplication.utils.InputValidator;
import com.example.myapplication.utils.SharedPreferencesUtil;
import com.google.android.material.textfield.TextInputEditText;

public class SignUp1Activity extends AppCompatActivity {

    private TextInputEditText editTextUsernameFindSignUp;
    private Button buttonEmailAuthRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup1);

        editTextUsernameFindSignUp = findViewById(R.id.editTextUseremailFindSignUp);
        buttonEmailAuthRequest = findViewById(R.id.buttonEmailRequest);

        if (buttonEmailAuthRequest != null) {
            buttonEmailAuthRequest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String email = editTextUsernameFindSignUp.getText().toString();
                    String mode = "signup";
                    if (email == null || email.isEmpty()) {
                        Toast.makeText(SignUp1Activity.this, "이메일을 입력하세요", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!InputValidator.isValidEmail(email)) {
                        Toast.makeText(SignUp1Activity.this, "올바른 형식의 이메일을 입력하세요", Toast.LENGTH_LONG).show();
                        return;
                    }
                    EmailAuthRequestTask.sendRequest(email, mode, new EmailAuthRequestTask.Callback() {
                        @Override
                        public void onSuccess(int httpCode, String responseBody) {
                            SharedPreferencesUtil.savePendingEmail(SignUp1Activity.this, email);
                            Intent intent = new Intent(SignUp1Activity.this, SignUp2Activity.class);
                            startActivity(intent);
                            Toast.makeText(SignUp1Activity.this, "이메일: " + email + " 로 비밀번호 찾기 시도", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onFailure(String errorMsg) {
                            Toast.makeText(SignUp1Activity.this,
                                    "인증 요청 실패: " + errorMsg,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }
    }
}