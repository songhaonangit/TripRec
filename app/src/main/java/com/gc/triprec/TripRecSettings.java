package com.gc.triprec;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class TripRecSettings {
    private SharedPreferences m_settings;

    private static final String TAG = "TripRecSettings";

    public TripRecSettings(Context context) {
        m_settings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setRecordEnable(boolean enable) {
        SharedPreferences.Editor editor = m_settings.edit();
        editor.putBoolean(Constants.SettingConstants.KEY_RECORD_ENABLE, enable);
        editor.apply();
    }

    public void setRecordEnable(String time) {
        SharedPreferences.Editor editor = m_settings.edit();
        editor.putString(Constants.SettingConstants.KEY_RECORD_TIME, time);
        editor.apply();
    }

    public boolean getRecordEnable() {
        return m_settings.getBoolean(Constants.SettingConstants.KEY_RECORD_ENABLE, false);
    }

    public int getRecordTime() {
        String time = m_settings.getString(Constants.SettingConstants.KEY_RECORD_TIME, "15");
        int recordtime = Integer.parseInt(time);
        Log.i(TAG, "get recordtime: " + String.valueOf(recordtime));

        return recordtime;
    }
}
