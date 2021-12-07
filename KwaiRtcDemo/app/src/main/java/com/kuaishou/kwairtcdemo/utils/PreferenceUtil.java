package com.kuaishou.kwairtcdemo.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.kuaishou.kwairtcdemo.base.KWApplication;

/**
 * des: Preference管理工具类.
 * 主要用于存储一些临时数据
 */
public class PreferenceUtil {
    private static final String SHARE_PREFERENCE_NAME = "KWAI_APP_PREFERENCE";
    /**
     * 单例.
     */
    public static PreferenceUtil sInstance;
    private static SharedPreferences mSharedPreferences;

    private PreferenceUtil() {
        mSharedPreferences = KWApplication.application.getSharedPreferences(SHARE_PREFERENCE_NAME,
                Context.MODE_PRIVATE);
    }

    public static PreferenceUtil getInstance() {
        if (sInstance == null) {
            synchronized (PreferenceUtil.class) {
                if (sInstance == null) {
                    sInstance = new PreferenceUtil();
                }
            }
        }
        return sInstance;
    }

    public void setStringValue(String key, String value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public String getStringValue(String key, String defaultValue) {
        return mSharedPreferences.getString(key, defaultValue);
    }

    public void setBooleanValue(String key, boolean value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public boolean getBooleanValue(String key, boolean defaultValue) {
        return mSharedPreferences.getBoolean(key, defaultValue);
    }

    public void setIntValue(String key, int value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public int getIntValue(String key, int defaultValue) {
        return mSharedPreferences.getInt(key, defaultValue);
    }

    public void setLongValue(String key, long value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public long getLongValue(String key, long defaultValue) {
        return mSharedPreferences.getLong(key, defaultValue);
    }

    public void setFloatValue(String key, float value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putFloat(key, value);
        editor.apply();
    }

    public float getFloatValue(String key, float defaultValue) {
        return mSharedPreferences.getFloat(key, defaultValue);
    }
}