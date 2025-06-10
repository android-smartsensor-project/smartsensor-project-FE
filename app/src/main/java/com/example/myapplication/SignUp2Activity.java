package com.example.myapplication;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.network.EmailAuthCodeTask;
import com.example.myapplication.utils.InputValidator;
import com.example.myapplication.utils.SharedPreferencesUtil;
import com.google.android.material.textfield.TextInputEditText;

public class SignUp2Activity extends AppCompatActivity {

    private TextInputEditText editTextAuthCode;
    private Button buttonEmailAuthRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup2);

        editTextAuthCode = findViewById(R.id.editTextAuthCode);
        buttonEmailAuthRequest = findViewById(R.id.buttonEmailAuthRequest);

        if (buttonEmailAuthRequest != null) {
            buttonEmailAuthRequest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String authCode = editTextAuthCode.getText().toString();
                    if (authCode == null || authCode.isEmpty()) {
                        Toast.makeText(SignUp2Activity.this, "인증번호를 입력하세요", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!InputValidator.isValidAuthNumber(authCode)) {
                        Toast.makeText(SignUp2Activity.this, "올바른 인증번호(숫자 4자리)를 입력해주세요!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String email = SharedPreferencesUtil.getPendingEmail(SignUp2Activity.this);
                    String mode = "signup";
                    EmailAuthCodeTask.sendRequest(email, mode, authCode, new EmailAuthCodeTask.Callback() {
                        @Override
                        public void onSuccess(int httpCode, String responseBody) {
                            Toast.makeText(SignUp2Activity.this,
                                    "인증이 완료되었습니다!",
                                    Toast.LENGTH_LONG).show();
                            Log.d(TAG, "onSuccess: HTTP " + httpCode + ", body=" + responseBody);
                            SharedPreferencesUtil.savePendingEmail(SignUp2Activity.this, email);
                            Intent intent = new Intent(SignUp2Activity.this, SignUp3Activity.class);
                            startActivity(intent);
                        }

                        @Override
                        public void onFailure(String errorMsg) {
                            Toast.makeText(SignUp2Activity.this,
                                    "인증 요청 실패: " + errorMsg,
                                    Toast.LENGTH_LONG).show();
                            Log.e(TAG, "onFailure: " + errorMsg);
                        }
                    });
                }
            });
        }
    }
}