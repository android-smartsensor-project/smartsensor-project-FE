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
    private String sex;
    private float weight;

    // 3) 필드별 getter / setter
    public String getIdToken() {
        return this.idToken;
    }
    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getEmail() {
        return this.email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return this.password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return this.phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBirth() { return this.birth; }
    public void setBirth(String birth) { this.birth = birth; }

    public String getSex() { return this.sex; }
    public void setSex(String sex) { this.sex = sex; }

    public Float getWeight() { return weight; }
    public void setWeight(Float weight) { this.weight = weight; }
}
