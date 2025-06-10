package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.myapplication.utils.InputValidator;
import com.example.myapplication.utils.SharedPreferencesUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class SignUp3Activity extends AppCompatActivity {

    private TextInputEditText editTextName;
    private TextInputEditText editTextPassword;
    private TextInputEditText editTextConfirmPassword;
    private TextInputEditText editTextBirth;
    private TextInputEditText editTextPhone;
    private Button buttonSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup3);

        editTextName = findViewById(R.id.editTextName);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        editTextBirth = findViewById(R.id.editTextBirth);
        editTextPhone = findViewById(R.id.editTextPhone);
        buttonSignUp = findViewById(R.id.buttonSignUp);

        if (buttonSignUp != null) {
            buttonSignUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String email = SharedPreferencesUtil.getPendingEmail(SignUp3Activity.this);
                    if (email == null) {
                        Toast.makeText(SignUp3Activity.this, "이메일 인증이 필요합니다.", Toast.LENGTH_LONG).show();
                        Intent intent1 = new Intent(SignUp3Activity.this, SignUp1Activity.class);
                        startActivity(intent1);
                        finish();
                    }
                    String name = editTextName.getText().toString();
                    String password = editTextPassword.getText().toString();
                    String confirmPassword = editTextConfirmPassword.getText().toString();
                    String birth = editTextBirth.getText().toString();
                    String phone = editTextPhone.getText().toString();

                    if (!checkUserInfoValidation(name, password, confirmPassword, birth, phone)) {
                        return;
                    }
                    FirebaseAuth mAuth = FirebaseAuth.getInstance();
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(SignUp3Activity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                UserAccount userInfo = new UserAccount();
                                userInfo.setIdToken(user.getUid());
                                userInfo.setEmailId(email);
                                userInfo.setPassword(password);
                                userInfo.setName(name);
                                userInfo.setPhone(phone);
                                database.getReference("users")
                                        .child(user.getUid())
                                        .setValue(userInfo).addOnSuccessListener(aVoid -> {
                                            // DB 저장 성공 시
                                            Toast.makeText(SignUp3Activity.this, "회원가입 및 DB 저장 성공!", Toast.LENGTH_SHORT).show();
                                            SharedPreferencesUtil.clearPendingEmail(SignUp3Activity.this);
                                            Intent intent = new Intent(SignUp3Activity.this, MainActivity.class);
                                            startActivity(intent);
                                        })
                                        .addOnFailureListener(e -> {
                                            // DB 저장 실패 시
                                            Log.e("FirebaseDB", "setValue 실패: " + e.getMessage());
                                            Toast.makeText(SignUp3Activity.this, "DB 저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                            } else {
                                // If sign in fails, display a message to the user.
                                Toast.makeText(SignUp3Activity.this, "회원가입 실패", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });
        }
    }
    private boolean checkUserInfoValidation(String name, String pwd, String pwdConfirm, String birth, String phone) {
        if (!InputValidator.isValidName(name)) {
            Toast.makeText(SignUp3Activity.this, "이름을 입력해주세요.", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!InputValidator.isValidPassword(pwd)) {
            Toast.makeText(SignUp3Activity.this, "비밀번호는 숫자/대문자/소문자/특수문자 각각 1개씩, 12~20자리로 입력해주세요.", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!pwdConfirm.equals(pwd)) {
            Toast.makeText(SignUp3Activity.this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!InputValidator.isValidBirthDate(birth)) {
            Toast.makeText(SignUp3Activity.this, "올바른 생년월일을 입력해주세요(1900년생-2006년생)", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!InputValidator.isValidPhoneNumber(phone)) {
            Toast.makeText(SignUp3Activity.this, "올바른 전화번호를 입력해주세요.(01000000000)", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }
}