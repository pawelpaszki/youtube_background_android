package com.pawelpaszki.youtubeplus.utils;

/**
 * Basic configuration values used in app
 * Created by smedic on 2.2.16..
 */

public final class Config {

    public static final String SUGGESTIONS_URL = "http://suggestqueries.google.com/complete/search?client=youtube&ds=yt&q=";
    public static final String YOUTUBE_BASE_URL = "http://youtube.com/watch?v=";
    public static final String YOUTUBE_TYPE = "YT_MEDIA_TYPE";
    public static final String YOUTUBE_TYPE_VIDEO = "YT_VIDEO";
    public static final String LOCAL_MEDIA_FILEAME = "FILENAME";
    public static final String YOUTUBE_TYPE_PLAYLIST= "YT_PLAYLIST";
    public static final String YOUTUBE_TYPE_PLAYLIST_VIDEO_POS = "YT_PLAYLIST_VIDEO_POS";

    public static final String YOUTUBE_API_KEY = "YOUR_KEY_GOES_HERE";

    public static final long NUMBER_OF_VIDEOS_RETURNED = 50; //due to YouTube API rules - MAX 50

    public static final String ACTION_PLAYBACK_STARTED = "playbackStarted";
    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_SEEK = "action_seek";
    public static final String ACTION_SEEKBAR_UPDATE = "action_update";
    public static final String ACTION_VIDEO_UPDATE = "action_video_update";
    public static final String ACTION_STOP = "action_stop";
    public static final String ACITON_VIDEO_CHANGE = "action_change_media";
    public static final String ACITON_ACTIVITY_RESUMED = "action_activity_resumed";
    public static final String ACTION_MEDIA_PAUSED = "action_activity_paused";
    public static final String ACTION_LOOPING_SELECTED = "action_looping_selected";


}