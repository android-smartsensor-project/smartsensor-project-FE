package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private Button buttonLogin;
    private TextView textViewSignUp;
    private TextView textViewFindPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        editTextEmail = findViewById(R.id.editTextBirth);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewSignUp = findViewById(R.id.textViewSignUp);
        textViewFindPassword = findViewById(R.id.textViewFindPassword);

        FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();

        if (buttonLogin != null) {
            buttonLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String email = editTextEmail.getText().toString();
                    String password = editTextPassword.getText().toString();
                    if (email == null || email.isEmpty()) {
                        Toast.makeText(LoginActivity.this, "이메일을 입력하세요!", Toast.LENGTH_SHORT).show();
                        return ;
                    }
                    if (password == null || password.isEmpty()) {
                        Toast.makeText(LoginActivity.this, "비밀번호를 입력하세요!", Toast.LENGTH_SHORT).show();
                        return ;
                    }
                    mFirebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                            } else {
                                Toast.makeText(LoginActivity.this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });
        }

        if (textViewSignUp != null) {
            textViewSignUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(LoginActivity.this, SignUp1Activity.class);
                    startActivity(intent);
                    Toast.makeText(LoginActivity.this, "회원가입 화면으로 이동!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (textViewFindPassword != null) {
            textViewFindPassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(LoginActivity.this, FindPw1Activity.class);
                    startActivity(intent);
                    Toast.makeText(LoginActivity.this, "비밀번호 찾기 화면으로 이동!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
