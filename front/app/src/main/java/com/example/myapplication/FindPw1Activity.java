package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;

public class FindPw1Activity extends AppCompatActivity {

    private TextInputEditText editTextUsernameFindPw;
    private Button buttonFindPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.findpw1);

        editTextUsernameFindPw = findViewById(R.id.editTextUseremailFindPw);
        buttonFindPassword = findViewById(R.id.buttonFindPassword);

        if (buttonFindPassword != null) {
            buttonFindPassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(FindPw1Activity.this, FindPw2Activity.class);
                    String username = editTextUsernameFindPw.getText().toString();
                    startActivity(intent);
                    Toast.makeText(FindPw1Activity.this, "이메일: " + username + " 로 비밀번호 찾기 시도", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}