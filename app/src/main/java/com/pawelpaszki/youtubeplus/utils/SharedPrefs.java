package com.pawelpaszki.youtubeplus.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by PawelPaszki on 04/07/2017.
 */

public class SharedPrefs {

    public static void setIsLooping(boolean value, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        String key = "isLooping";
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static boolean getIsLooping(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String key = "isLooping";
        return prefs.getBoolean(key, false);
    }
}
