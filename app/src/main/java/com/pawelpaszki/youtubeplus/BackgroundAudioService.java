/*
 * Copyright (C) 2016 SMedic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pawelpaszki.youtubeplus;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.facebook.network.connectionclass.ConnectionClassManager;
import com.facebook.network.connectionclass.ConnectionQuality;
import com.facebook.network.connectionclass.DeviceBandwidthSampler;
import com.pawelpaszki.youtubeplus.database.YouTubeSqlDb;
import com.pawelpaszki.youtubeplus.model.ItemType;
import com.pawelpaszki.youtubeplus.model.YouTubeVideo;
import com.pawelpaszki.youtubeplus.utils.Config;
import com.pawelpaszki.youtubeplus.utils.SharedPrefs;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

import static com.pawelpaszki.youtubeplus.utils.Config.ACITON_VIDEO_CHANGE;
import static com.pawelpaszki.youtubeplus.utils.Config.ACTION_NEXT;
import static com.pawelpaszki.youtubeplus.utils.Config.ACTION_PAUSE;
import static com.pawelpaszki.youtubeplus.utils.Config.ACTION_PLAY;
import static com.pawelpaszki.youtubeplus.utils.Config.ACTION_PREVIOUS;
import static com.pawelpaszki.youtubeplus.utils.Config.ACTION_SEEK;
import static com.pawelpaszki.youtubeplus.utils.Config.ACTION_SEEKBAR_UPDATE;
import static com.pawelpaszki.youtubeplus.utils.Config.ACTION_STOP;

/**
 * Service class for background youtube playback
 * Created by Stevan Medic on 9.3.16..
 */
public class BackgroundAudioService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener {

    private static final String TAG = "SMEDIC service";

    private Handler mSeekBarProgressHandler;
    private boolean mPreviousPressed;

    private MediaPlayer mMediaPlayer;
    private MediaSessionCompat mSession;
    private MediaControllerCompat mController;

    private ItemType mediaType = ItemType.YOUTUBE_MEDIA_NONE;

    private YouTubeVideo videoItem;

    private boolean isStarting = false;
    private int currentSongIndex = 0;
    private boolean mSeekToSet;

    private ArrayList<YouTubeVideo> youTubeVideos;

    private NotificationCompat.Builder builder = null;
    private int mSetSeekToPosition;

    private DeviceBandwidthSampler deviceBandwidthSampler;
    private ConnectionQuality connectionQuality = ConnectionQuality.MODERATE;
    private static final String ACTION_PLAYBACK_STARTED = "playbackStarted";

    private BroadcastReceiver mPauseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleIntent(intent);
        }
    };

    private BroadcastReceiver mPlayReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleIntent(intent);
        }
    };

    private BroadcastReceiver mNextReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleIntent(intent);
        }
    };

    private BroadcastReceiver mPreviousReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleIntent(intent);
        }
    };

    private BroadcastReceiver mSeekToReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleIntent(intent);
        }
    };

    private BroadcastReceiver mStopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleIntent(intent);
        }
    };


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("on create", "background audio service");
        videoItem = new YouTubeVideo();
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        initMediaSessions();
        initPhoneCallListener();
        deviceBandwidthSampler = DeviceBandwidthSampler.getInstance();

        if (mPauseReceiver != null) {
            IntentFilter intentFilter = new IntentFilter(ACTION_PAUSE);
            registerReceiver(mPauseReceiver, intentFilter);
        }
        if (mPlayReceiver != null) {
            IntentFilter intentFilter = new IntentFilter(ACTION_PLAY);
            registerReceiver(mPlayReceiver, intentFilter);
        }
        if (mNextReceiver != null) {
            IntentFilter intentFilter = new IntentFilter(ACTION_NEXT);
            registerReceiver(mNextReceiver, intentFilter);
        }
        if (mPreviousReceiver != null) {
            IntentFilter intentFilter = new IntentFilter(ACTION_PREVIOUS);
            registerReceiver(mPreviousReceiver, intentFilter);
        }
        if (mSeekToReceiver != null) {
            IntentFilter intentFilter = new IntentFilter(ACTION_SEEK);
            registerReceiver(mSeekToReceiver, intentFilter);
        }
        if (mStopReceiver != null) {
            IntentFilter intentFilter = new IntentFilter(ACTION_STOP);
            registerReceiver(mStopReceiver, intentFilter);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return START_STICKY;
    }

    private void initPhoneCallListener() {
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    //Incoming call: Pause music
                    pauseVideo();
                } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                    //Not in call: Play music
                    Log.d(TAG, "onCallStateChanged: ");
                    resumeVideo();
                } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    //A call is dialing, active or on hold
                }
                super.onCallStateChanged(state, incomingNumber);
            }
        };

        TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (mgr != null) {
            mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mNextReceiver != null) {
            unregisterReceiver(mNextReceiver);
        }
        if (mPauseReceiver != null) {
            unregisterReceiver(mPauseReceiver);
        }
        if (mPlayReceiver != null) {
            unregisterReceiver(mPlayReceiver);
        }
        if (mPreviousReceiver != null) {
            unregisterReceiver(mPreviousReceiver);
        }
        if (mSeekToReceiver != null) {
            unregisterReceiver(mSeekToReceiver);
        }
        if (mStopReceiver != null) {
            unregisterReceiver(mStopReceiver);
        }
        if(mMediaPlayer!=null) {
            if(mMediaPlayer.isPlaying())
                mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer=null;
        }
    }

    /**
     * Handles intent (player options play/pause/stop...)
     *
     * @param intent - intent to handle
     */
    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null)
            return;
        String action = intent.getAction();
        Log.i("action intent rec", "true");
        Log.i("media type", String.valueOf(mediaType));
        if (action.equalsIgnoreCase(ACTION_PLAY)) {
            handleMedia(intent);
            handleSeekBarChange(videoItem.getId());
            mController.getTransportControls().play();
        } else if (action.equalsIgnoreCase(ACTION_PAUSE)) {
            mController.getTransportControls().pause();
            removeAllHandlers();
        } else if (action.equalsIgnoreCase(ACTION_PREVIOUS)) {
            playPrevious();
        } else if (action.equalsIgnoreCase(ACTION_NEXT)) {
            playNext();
//            if(mediaType == ItemType.MEDIA_LOCAL) {
//                playNext();
//            } else {
//                mController.getTransportControls().skipToNext();
//            }
        } else if (action.equalsIgnoreCase(ACTION_STOP)) {
            Log.i("stop", "in service");
            mController.getTransportControls().stop();
            if(mMediaPlayer != null) {
                stopPlayer();
                if(mSeekBarProgressHandler != null) {
                    mSeekBarProgressHandler.removeCallbacksAndMessages(null);
                    mSeekBarProgressHandler = null;
                }
            }
            stopSelf();
        } else if (action.equalsIgnoreCase(ACTION_SEEK)) {
            int value = intent.getIntExtra("seekTo", 0);
                seekVideo(value * 1000);
                mSetSeekToPosition = value * 1000;
                handleSeekBarChange(videoItem.getId());
        }
    }

    private void handleSeekBarChange(final String videoId) {
        if(mSeekBarProgressHandler != null) {
            mSeekBarProgressHandler.removeCallbacksAndMessages(null);
            mSeekBarProgressHandler = null;
        }
        mSeekBarProgressHandler = new Handler();
        mSeekBarProgressHandler.postDelayed(new Runnable(){
            public void run(){
                //Log.i("seekbar handle", "active");
                if(mMediaPlayer != null) {
                    if((mSetSeekToPosition != mMediaPlayer.getCurrentPosition() * 1000 && mSeekToSet) || !mSeekToSet) {
                        if(mMediaPlayer.isPlaying()) {
                            if(mSetSeekToPosition != mMediaPlayer.getCurrentPosition() * 1000) {
                                Intent new_intent = new Intent();
                                new_intent.setAction(ACTION_SEEKBAR_UPDATE);
                                new_intent.putExtra("videoId", videoId);
                                new_intent.putExtra("progress", mMediaPlayer.getCurrentPosition());
                                sendBroadcast(new_intent);
                                mSeekToSet = false;
                                Log.i("progress value", String.valueOf(mMediaPlayer.getCurrentPosition()));
                            }
                        }
                    }
                    mSeekBarProgressHandler.postDelayed(this, 1000);
                }
//                else {
//                    if(mSeekBarProgressHandler != null) {
//                        mSeekBarProgressHandler.removeCallbacksAndMessages(null);
//                        mSeekBarProgressHandler = null;
//                    }
//                }
            }
        }, 1000);

    }

    private void removeAllHandlers() {
        if(mSeekBarProgressHandler != null) {
            mSeekBarProgressHandler.removeCallbacksAndMessages(null);
            mSeekBarProgressHandler = null;
        }
    }

    /**
     * Handles media - playlists and videos sent from fragments
     *
     * @param intent - intent to handle
     */
    private void handleMedia(Intent intent) {
        ItemType intentMediaType = ItemType.YOUTUBE_MEDIA_NONE;
        if (intent.getSerializableExtra(Config.YOUTUBE_TYPE) != null) {
            intentMediaType = (ItemType) intent.getSerializableExtra(Config.YOUTUBE_TYPE);
        }
        switch (intentMediaType) {
            case YOUTUBE_MEDIA_NONE: //video is paused,so no new playback requests should be processed
                mMediaPlayer.start();
                break;
            case YOUTUBE_MEDIA_TYPE_VIDEO:
                mediaType = ItemType.YOUTUBE_MEDIA_TYPE_VIDEO;
                videoItem = (YouTubeVideo) intent.getSerializableExtra(Config.YOUTUBE_TYPE_VIDEO);
                if (videoItem.getId() != null) {
                    playVideo();
                }
                break;
            case YOUTUBE_MEDIA_TYPE_PLAYLIST: //new playlist playback request
                mediaType = ItemType.YOUTUBE_MEDIA_TYPE_PLAYLIST;
                youTubeVideos = (ArrayList<YouTubeVideo>) intent.getSerializableExtra(Config.YOUTUBE_TYPE_PLAYLIST);
                int startPosition = intent.getIntExtra(Config.YOUTUBE_TYPE_PLAYLIST_VIDEO_POS, 0);
                videoItem = youTubeVideos.get(startPosition);
                currentSongIndex = startPosition;
                playVideo();
                break;
            case MEDIA_LOCAL:
                mediaType = ItemType.MEDIA_LOCAL;
                String filename = (String) intent.getSerializableExtra(Config.LOCAL_MEDIA_FILEAME);
                videoItem = (YouTubeVideo) intent.getSerializableExtra(Config.YOUTUBE_TYPE_VIDEO);
                youTubeVideos = (ArrayList<YouTubeVideo>) intent.getSerializableExtra(Config.YOUTUBE_TYPE_PLAYLIST);
                playLocalMedia(filename);
                break;
            default:
                Log.d(TAG, "Unknown command");
                break;
        }
    }

    /**
     * Initializes media sessions and receives media events
     */
    private void initMediaSessions() {
        // Make sure the media player will acquire a wake-lock while playing. If we don't do
        // that, the CPU might go to sleep while the song is playing, causing playback to stop.
        //
        // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
        // permission in AndroidManifest.xml.
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        PendingIntent buttonReceiverIntent = PendingIntent.getBroadcast(
                getApplicationContext(),
                0,
                new Intent(Intent.ACTION_MEDIA_BUTTON),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        mSession = new MediaSessionCompat(getApplicationContext(), "simple player session",
                null, buttonReceiverIntent);

        try {
            mController = new MediaControllerCompat(getApplicationContext(), mSession.getSessionToken());

            mSession.setCallback(
                    new MediaSessionCompat.Callback() {
                        @Override
                        public void onPlay() {
                            super.onPlay();
                            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
                        }

                        @Override
                        public void onPause() {

                            super.onPause();
                            pauseVideo();
                            buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY));
                        }

                        @Override
                        public void onSkipToNext() {
                            super.onSkipToNext();
                            if (!isStarting) {
                                playNext();
                            }
                            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
                        }

                        @Override
                        public void onSkipToPrevious() {
                            super.onSkipToPrevious();
                            if (!isStarting) {
                                playPrevious();
                            }
                            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
                        }

                        @Override
                        public void onStop() {
                            super.onStop();
                            stopPlayer();
                            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                            notificationManager.cancel(1);
                            Intent intent = new Intent(getApplicationContext(), BackgroundAudioService.class);
                            stopService(intent);
                        }

                        @Override
                        public void onSetRating(RatingCompat rating) {
                            super.onSetRating(rating);
                        }
                    }
            );
        } catch (RemoteException re) {
            re.printStackTrace();
        }
    }

    /**
     * Builds notification panel with buttons and info on it
     *
     * @param action Action to be applied
     */

    private void buildNotification(NotificationCompat.Action action) {

        final NotificationCompat.MediaStyle style = new NotificationCompat.MediaStyle();

        Intent intent = new Intent(getApplicationContext(), BackgroundAudioService.class);
        intent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);

        Intent clickIntent = new Intent(this, MainActivity.class);
        clickIntent.setAction(Intent.ACTION_MAIN);
        clickIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(this, 0, clickIntent, 0);

        builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(android.R.drawable.ic_media_play);
        //builder.setColor(getApplicationContext().getResources().getColor(R.color.colorPrimary));
        builder.setContentTitle(videoItem.getTitle());
        builder.setContentInfo(videoItem.getDuration());
        builder.setShowWhen(false);
        builder.setContentIntent(clickPendingIntent);
        builder.setDeleteIntent(stopPendingIntent);
        builder.setOngoing(false);
        builder.setSubText(videoItem.getViewCount());
        builder.setStyle(style);

        //load bitmap for largeScreen
        if (videoItem.getThumbnailURL() != null && !videoItem.getThumbnailURL().isEmpty()) {
            Picasso.with(this)
                    .load(videoItem.getThumbnailURL())
                    .into(target);
        }

        builder.addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", ACTION_PREVIOUS));
        builder.addAction(action);
        builder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", ACTION_NEXT));
        style.setShowActionsInCompactView(0, 1, 2);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());

    }

    /**
     * Field which handles image loading
     */
    private Target target = new Target() {

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
            updateNotificationLargeIcon(bitmap);
        }

        @Override
        public void onBitmapFailed(Drawable drawable) {
            Log.d(TAG, "Load bitmap... failed");
        }

        @Override
        public void onPrepareLoad(Drawable drawable) {
        }
    };

    /**
     * Updates only large icon in notification panel when bitmap is decoded
     *
     * @param bitmap - notification bitmap
     */
    private void updateNotificationLargeIcon(Bitmap bitmap) {
        builder.setLargeIcon(bitmap);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    /**
     * Generates specific action with parameters below
     *
     * @param icon - icon
     * @param title - title
     * @param intentAction - action
     * @return new notification
     */
    private NotificationCompat.Action generateAction(int icon, String title, String intentAction) {
        Intent intent = new Intent(getApplicationContext(), BackgroundAudioService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        return new NotificationCompat.Action.Builder(icon, title, pendingIntent).build();
    }

    /**
     * Plays next video in playlist
     */
    private void playNext() {
        //if media type is video not playlist, just loop it
        if (mediaType == ItemType.YOUTUBE_MEDIA_TYPE_VIDEO) {
            seekVideo(0);
            restartVideo();
            return;
        }

        if (mediaType == ItemType.MEDIA_LOCAL) {
            Log.i("media local", "next is local");
            Log.i("video item", videoItem.getId() + videoItem.getTitle());
            Log.i("videos", youTubeVideos.toString());
            int index = getNextLocalMediaIndex(videoItem.getId());
            startMediaPlayerWithLocalMedia(youTubeVideos.get(index).getId());
            videoItem = youTubeVideos.get(index);
            mPreviousPressed = false;
            return;
        }

        if (youTubeVideos.size() > currentSongIndex + 1) {
            currentSongIndex++;
        } else { //play 1st song
            currentSongIndex = 0;
        }

        videoItem = youTubeVideos.get(currentSongIndex);
        playVideo();
    }

    /**
     * Plays previous video in playlist
     */
    private void playPrevious() {
        //if media type is video not playlist, just loop it
        if (mediaType == ItemType.YOUTUBE_MEDIA_TYPE_VIDEO) {
            restartVideo();
            return;
        }

        if (mediaType == ItemType.MEDIA_LOCAL) {
            Log.i("media local", "previous is local");
            Log.i("video item", videoItem.getId() + videoItem.getTitle());
            int index = getPreviousLocalMediaIndex(videoItem.getId());
            startMediaPlayerWithLocalMedia(youTubeVideos.get(index).getId());
            videoItem = youTubeVideos.get(index);
            mPreviousPressed = true;
            return;
        }

        if (currentSongIndex - 1 >= 0) {
            currentSongIndex--;
        } else { //play last song
            currentSongIndex = youTubeVideos.size() - 1;
        }
        videoItem = youTubeVideos.get(youTubeVideos.size() - 1);
        playVideo();
    }

    /**
     * play local media
     */
    private void playLocalMedia(String filename){

        startMediaPlayerWithLocalMedia(filename);
    }

    private void startMediaPlayerWithLocalMedia(final String filename){
        Log.i("got here", "start local");
        try {
            if (mMediaPlayer != null) {
                stopPlayer();

                final File[] files =Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).listFiles();
                int index = -1;
                for(int i = 0; i < files.length; i++) {
                    if(files[i].getAbsolutePath().contains(filename)) {
                        Log.i("file in download", files[i].getAbsolutePath());
                        index = i;
                        break;
                    }
                }
                if(index != -1) {
                    mPreviousPressed = false;
                    mMediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(files[index].getAbsolutePath()));
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    Log.i("duration", String.valueOf(mMediaPlayer.getDuration()));
                    mMediaPlayer.setOnCompletionListener(this);
                    mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mMediaPlayer.start();

                            handleSeekBarChange(filename);

                            sendBroadcast(videoItem.getDuration());
                            Intent new_intent = new Intent();
                            new_intent.setAction(ACITON_VIDEO_CHANGE);
                            new_intent.putExtra("videoId", filename);
                            sendBroadcast(new_intent);

                        }
                    });


                } else {
                    if(anyLocalMediaExists()) {
                        int newIndex;
                        if(mPreviousPressed) {
                            newIndex = getPreviousLocalMediaIndex(filename);
                        } else {
                            newIndex = getNextLocalMediaIndex(filename);
                        }

                        videoItem = youTubeVideos.get(newIndex);
                        startMediaPlayerWithLocalMedia(youTubeVideos.get(newIndex).getId());
                    } else {
                        YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.DOWNLOADED).deleteAll();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean anyLocalMediaExists() {
        File[] files =Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).listFiles();
        for (File file : files) {
            for (int j = 0; j < youTubeVideos.size(); j++) {
                if (file.getAbsolutePath().contains(youTubeVideos.get(j).getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getNextLocalMediaIndex(String filename) {
        int index = 0;
        for(int i = 0; i < youTubeVideos.size(); i++) {
            if(youTubeVideos.get(i).getId().equals(filename)) {
                index = i;
                break;
            }
        }
        if(index + 1 == youTubeVideos.size()) {
            index = 0;
        } else {
            index++;
        }
        return index;
    }

    private int getPreviousLocalMediaIndex(String filename) {
        int index = 0;
        for(int i = 0; i < youTubeVideos.size(); i++) {
            if(youTubeVideos.get(i).getId().equals(filename)) {
                index = i;
                break;
            }
        }
        if(index == 0) {
            index = youTubeVideos.size()-1;
        } else {
            index--;
        }
        return index;
    }

    /**
     * Plays video
     */
    private void playVideo() {
        isStarting = true;
        extractUrlAndPlay();
    }

    /**
     * Pauses video
     */
    private void pauseVideo() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
    }

    /**
     * Resumes video
     */
    private void resumeVideo() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }
    }

    /**
     * Restarts video
     */
    private void restartVideo() {
        mMediaPlayer.start();
    }

    /**
     * Seeks to specific time
     *
     * @param seekTo - progress vlue
     */
    private void seekVideo(int seekTo) {
        mMediaPlayer.seekTo(seekTo);
    }

    /**
     * Stops video
     */
    private void stopPlayer() {
        if(mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
        }

    }

    /**
     * Get the best available audio stream
     * <p>
     * Itags:
     * 141 - mp4a - stereo, 44.1 KHz 256 Kbps
     * 251 - webm - stereo, 48 KHz 160 Kbps
     * 140 - mp4a - stereo, 44.1 KHz 128 Kbps
     * 17 - mp4 - stereo, 44.1 KHz 96-100 Kbps
     *
     * @param ytFiles Array of available streams
     * @return Audio stream with highest bitrate
     */
    private YtFile getBestStream(SparseArray<YtFile> ytFiles) {

        connectionQuality = ConnectionClassManager.getInstance().getCurrentBandwidthQuality();
        int[] itags = new int[]{22, 18, 36, 17};

        if (connectionQuality != null && connectionQuality != ConnectionQuality.UNKNOWN) {
            switch (connectionQuality) {
                case POOR:
                    itags = new int[]{17, 36, 18, 22};
                    break;
                case MODERATE:
                    itags = new int[]{36, 18, 22, 17};
                    break;
                case GOOD:
                case EXCELLENT:
                    itags = new int[]{22, 18, 36, 17};
                    break;
            }
        }

        if (ytFiles.get(itags[0]) != null) {
            return ytFiles.get(itags[0]);
        } else if (ytFiles.get(itags[1]) != null) {
            return ytFiles.get(itags[1]);
        } else if (ytFiles.get(itags[2]) != null) {
            return ytFiles.get(itags[2]);
        }
        return ytFiles.get(itags[3]);
    }

    /**
     * Extracts link from youtube video ID, so mediaPlayer can play it
     */
    private void extractUrlAndPlay() {
        String youtubeLink = Config.YOUTUBE_BASE_URL + videoItem.getId();
        deviceBandwidthSampler.startSampling();

        new YouTubeExtractor(this) {
            @Override
            protected void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta videoMeta) {
                if (ytFiles == null) {
                    // Something went wrong we got no urls. Always check this.
                    Toast.makeText(YTApplication.getAppContext(), R.string.failed_playback,
                            Toast.LENGTH_SHORT).show();

                }
                deviceBandwidthSampler.stopSampling();
                YtFile ytFile = getBestStream(ytFiles);
                try {
                    if (mMediaPlayer != null) {
                        mMediaPlayer.reset();
                        mMediaPlayer.setDataSource(ytFile.getUrl());
                        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mMediaPlayer.prepare();
                        mMediaPlayer.start();
                        handleSeekBarChange(videoItem.getId());
                        sendBroadcast(videoItem.getDuration());
                    }
                } catch (IOException io) {
                    io.printStackTrace();
                }
            }
        }.execute(youtubeLink);
    }

    private void sendBroadcast(String duration) {
        Intent new_intent = new Intent();
        new_intent.setAction(ACTION_PLAYBACK_STARTED);
        new_intent.putExtra("duration", duration);
        new_intent.putExtra("title", videoItem.getTitle());
        new_intent.putExtra("playlist", videoItem.getTitle());
        sendBroadcast(new_intent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);
    }

    @Override
    public void onCompletion(MediaPlayer _mediaPlayer) {
        if (mediaType == ItemType.YOUTUBE_MEDIA_TYPE_PLAYLIST || !SharedPrefs.getIsLooping(getApplicationContext())) {
            playNext();
            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
        } else {
            restartVideo();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isStarting = false;
    }

}