package com.example.myapplication.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesUtil {
    private static final String PREFS_NAME = "email_link_prefs";
    private static final String KEY_PENDING_EMAIL = "PENDING_EMAIL";

    public static void savePendingEmail(Context context, String email) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_PENDING_EMAIL, email).apply();
    }

    public static String getPendingEmail(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PENDING_EMAIL, null); // 기본값은 null
    }

    public static void clearPendingEmail(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_PENDING_EMAIL).apply();
    }
}
