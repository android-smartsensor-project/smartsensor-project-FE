package com.example.myapplication.utils;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputValidator {
    private static final String EMAIL_REGEX =
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    private static final String PASSWORD_REGEX =
            "^(?=.*[0-9])" +         // 숫자 1개 이상
                    "(?=.*[A-Z])" +         // 대문자 1개 이상
                    "(?=.*[a-z])" +         // 소문자 1개 이상
                    "(?=.*[^A-Za-z0-9])" +  // 특수문자 1개 이상
                    "(?=.{12,20}$).*$";     // 길이 12~20자
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);
    private static final int MIN_YEAR = 1900;
    private static final int ADULT_AGE = 19;
    private static final String PHONE_REGEX = "^010\\d{8}$";

    // (2) Pattern 객체 생성 (컴파일 비용을 줄이기 위해 상수로 미리 생성)
    private static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);
    private static final String AUTH_NUMBER_REGEX = "^\\d{6}$";

    // (2) Pattern 객체 생성 (컴파일 비용을 줄이기 위해 상수로 미리 생성)
    private static final Pattern AUTH_NUMBER_PATTERN = Pattern.compile(AUTH_NUMBER_REGEX);
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }
    public static boolean isValidName(String name) {
        return (name != null && !name.isEmpty());
    }

    public static boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        Matcher matcher = PASSWORD_PATTERN.matcher(password);
        return matcher.matches();
    }

    /**
     * yyyyMMdd 형식의 문자열을 받아,
     * 1) 숫자 8자리
     * 2) 연도 범위 [1900, 현재년도-ADULT_AGE]
     * 3) 해당 연·월·일이 유효한 날짜(윤년 포함)
     * 을 모두 만족하면 true, 아니면 false를 반환.
     */
    public static boolean isValidBirthDate(String birthDate) {
        // 1) 널 체크 및 숫자 8자리 여부
        if (birthDate == null || birthDate.length() != 8 || !birthDate.matches("\\d{8}")) {
            return false;
        }

        try {
            // 2) 연·월·일 분리
            int year  = Integer.parseInt(birthDate.substring(0, 4));
            int month = Integer.parseInt(birthDate.substring(4, 6));
            int day   = Integer.parseInt(birthDate.substring(6, 8));

            // 3) LocalDate.of로 일자 생성 시 윤년·월별 일수 자동 검증
            LocalDate date = LocalDate.of(year, month, day);

            // 4) 연도 범위 검사 (올해 성인이 되는 출생 연도까지 허용)
            int currentYear = LocalDate.now().getYear();
            int maxYear = currentYear - ADULT_AGE;
            if (year < MIN_YEAR || year > maxYear) {
                return false;
            }
            return true;
        } catch (DateTimeException | NumberFormatException e) {
            // 파싱 실패(월 13월, 일 31일 초과, 윤년 오류 등) 시 false
            return false;
        }
    }

    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        Matcher matcher = PHONE_PATTERN.matcher(phone);
        return matcher.matches();
    }

    public static boolean isValidAuthNumber(String number) {
        if (number == null || number.isEmpty())
            return false;
        Matcher matcher = AUTH_NUMBER_PATTERN.matcher(number);
        return matcher.matches();
    }
}
