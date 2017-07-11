package com.pawelpaszki.youtubeplus.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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

    public static void setDownloadInProgress(String id, boolean value, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        String key = "downloading" + id;
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static boolean getDownloadInProgress(Context context, String id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String key = "downloading" + id;
        return prefs.getBoolean(key, false);
    }

    // number of playlists with given video
    public static void setVideoCounter(String videoId, int counter, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        String key = videoId;
        editor.putInt(key, counter);
        editor.apply();
    }

    // number of playlists with given video
    public static int getVideoCounter(String videoId, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String key = videoId;
        return prefs.getInt(key, 0);
    }

    public static void savePlaylistVideoIds(Context context, ArrayList<String> videoIds, String playListName){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs != null){
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < videoIds.size(); i++) {
                if(i + 1 < videoIds.size()) {
                    sb.append(videoIds.get(i)).append(",");
                } else {
                    sb.append(videoIds.get(i));
                }
            }
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("playlist" + playListName, sb.toString());
            editor.apply();
        }
    }

    public static void clearPlaylistVideoIds(Context context, String playListName) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs != null){
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("playlist" + playListName).apply();
        }
    }

    public static ArrayList<String> getPlaylistVideoIds(Context context, String playListName){
        ArrayList<String> videoIds = new ArrayList<>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs != null) {
            String concatenatedString = prefs.getString("playlist" + playListName, "");
            if (concatenatedString.length() == 0) {
                return videoIds;
            } else {
                String[] ids = concatenatedString.split(",");
                for (String id : ids) {
                    try {
                        videoIds.add(id);
                    } catch (Exception e) {
                        return new ArrayList<>();
                    }

                }
            }
        }
        return videoIds;
    }

    public static void savePlaylistNames(Context context, Set<String> playlists){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs != null){

            SharedPreferences.Editor editor = prefs.edit();
            editor.putStringSet("playLists", (Set)playlists);
            editor.apply();
        }
    }

    public static ArrayList<String> getPlayListNames(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String key = "playLists";
        Set<String> set = prefs.getStringSet(key, null);
        ArrayList<String> playlListNames = new ArrayList<>();
        if(set != null) {
            for(String item: set) {
                playlListNames.add(item);
            }
            return playlListNames;
        } else {
            return null;
        }

    }

}
