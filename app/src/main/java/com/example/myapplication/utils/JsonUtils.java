package com.example.myapplication.utils;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

public class JsonUtils {

    /**
     * Android SDK 내장 org.json을 사용해서
     * JSON 문자열을 HashMap<String, Object>로 변환합니다.
     */
    public static HashMap<String, Object> jsonToMap(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            HashMap<String, Object> map = new HashMap<>();

            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = jsonObject.get(key);
                map.put(key, value);
            }

            return map;
        } catch (JSONException ex) {
            Log.d("Not JSON Object",ex.toString());
            return null;
        }
    }
}