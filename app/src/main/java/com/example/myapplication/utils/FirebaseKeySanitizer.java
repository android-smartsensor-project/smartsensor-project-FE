package com.example.myapplication.utils;

public class FirebaseKeySanitizer {
    private static final String INVALID_CHARS_REGEX = "[\\.\\#\\$\\[\\]/]";
    public static String sanitizeKey(String rawKey) {
        if (rawKey == null || rawKey.isEmpty()) {
            return "";
        }

        // 1) Firebase에서 허용되지 않는 문자들(., #, $, [, ], /)을 "_"로 치환
        String sanitized = rawKey.replaceAll(INVALID_CHARS_REGEX, "_");

        // 2) 키가 빈 문자열이 되면 최소한 "_" 하나로 설정
        if (sanitized.isEmpty()) {
            return "_";
        }

        return sanitized;
    }
}
