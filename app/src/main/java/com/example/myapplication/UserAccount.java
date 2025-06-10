package com.example.myapplication;

public class UserAccount {
    // 1) 반드시 기본 생성자(파라미터 없는 생성자)가 있어야 합니다.
    public UserAccount() { }

    // 2) 저장할 필드 선언 (private 권장)
    private String idToken;
    private String email;
    private String password;
    private String name;
    private String phone;
    private String birth;

    // 3) 필드별 getter / setter
    public String getIdToken() {
        return idToken;
    }
    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getEmailId() {
        return email;
    }

    public void setEmailId(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public String getBirth() {
        return birth;
    }
    public void setBirth(String birth) {
        this.birth = birth;
    }
}
