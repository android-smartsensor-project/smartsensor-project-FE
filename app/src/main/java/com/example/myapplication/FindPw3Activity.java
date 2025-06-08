package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

public class FindPw3Activity extends AppCompatActivity {

    private TextInputEditText editTextPassword;
    private TextInputEditText editTextConfirmPassword;
    private Button buttonFindPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.findpw3);

        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonFindPassword = findViewById(R.id.buttonFindPassword);

        if (buttonFindPassword != null) {
            buttonFindPassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String newPassword = editTextPassword.getText().toString();
                    String confirmPassword = editTextConfirmPassword.getText().toString();

                    if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                        Toast.makeText(FindPw3Activity.this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!newPassword.equals(confirmPassword)) {
                        Toast.makeText(FindPw3Activity.this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Toast.makeText(FindPw3Activity.this, "비밀번호 재설정 시도: " + newPassword, Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(FindPw3Activity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
        }
    }
}