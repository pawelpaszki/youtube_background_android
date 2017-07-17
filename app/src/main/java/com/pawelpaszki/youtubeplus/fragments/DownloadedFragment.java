package com.pawelpaszki.youtubeplus.fragments;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import com.pawelpaszki.youtubeplus.BackgroundAudioService;
import com.pawelpaszki.youtubeplus.MainActivity;
import com.pawelpaszki.youtubeplus.R;
import com.pawelpaszki.youtubeplus.YTApplication;
import com.pawelpaszki.youtubeplus.adapters.NoThumbnailAdapter;
import com.pawelpaszki.youtubeplus.adapters.VideosAdapter;
import com.pawelpaszki.youtubeplus.database.YouTubeSqlDb;
import com.pawelpaszki.youtubeplus.interfaces.ItemEventsListener;
import com.pawelpaszki.youtubeplus.interfaces.OnItemSelected;
import com.pawelpaszki.youtubeplus.model.ItemType;
import com.pawelpaszki.youtubeplus.model.YouTubeVideo;
import com.pawelpaszki.youtubeplus.utils.Config;
import com.pawelpaszki.youtubeplus.utils.SharedPrefs;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import static com.pawelpaszki.youtubeplus.MainActivity.setmControlsTouched;
import static com.pawelpaszki.youtubeplus.dialogs.AddToPlayListDialog.showPlaylistSelectionDialog;

/**
 * Created by PawelPaszki on 04/07/2017.
 */

public class DownloadedFragment extends BaseFragment implements ItemEventsListener<YouTubeVideo>, SurfaceHolder.Callback, MediaPlayer.OnPreparedListener, MediaController.MediaPlayerControl, MediaPlayer.OnCompletionListener {

    private ArrayList<YouTubeVideo> downloadedVideos;

    private RecyclerView downloadedListView;
    private NoThumbnailAdapter videoListAdapter;
    private OnItemSelected itemSelected;
    private Context context;
    private int mContainerHeight;
    private int mContainerWidth;
    private LinearLayout mVideosContainer;
    private int mTopMargin;
    private boolean mSeekAdjustmentRequired;
    private boolean mReceiversRegistered;

    public DownloadedFragment() {
        // Required empty public constructor
    }

    public static DownloadedFragment newInstance() {
        return new DownloadedFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        downloadedVideos = new ArrayList<>();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_list, container, false);
        vidSurface = (SurfaceView) v.findViewById(R.id.surfaceView);
        vidHolder = vidSurface.getHolder();
        vidHolder.addCallback(this);
        mVideosContainer = (LinearLayout) v.findViewById(R.id.videos_container);
        setPlayListSize(true);
//        if(SharedPrefs.getVideoContainerHeight(context) == 0) {
        mVideosContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mContainerHeight = mVideosContainer.getHeight();
                    mContainerWidth = mVideosContainer.getWidth();
                    SharedPrefs.setVideoContainerHeight(context, mContainerHeight);
                    SharedPrefs.setVideoContainerWidth(context, mContainerWidth);
                    Log.i("Container height", String.valueOf(mContainerHeight));
                    mVideosContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                    android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(mContainerWidth, mContainerWidth * 3/4);
                    //params.topMargin = mTopMargin;

                    vidSurface.setLayoutParams(params);
                }
            });
//        }
        LinearLayout spinner = (LinearLayout) v.findViewById(R.id.playlist_management);
        spinner.setVisibility(View.GONE);
        Button clearRecentButton = (Button) v.findViewById(R.id.clear_recent);
        clearRecentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(downloadedVideos.size() > 0) {
                    Iterator<YouTubeVideo> iter = downloadedVideos.iterator();

                    while (iter.hasNext()) {
                        YouTubeVideo item = iter.next();
                        iter.remove();
                        onRemoveClicked(item);
                    }
                }
            }
        });
        downloadedListView = (RecyclerView) v.findViewById(R.id.fragment_list_items);
        downloadedListView.setLayoutManager(new LinearLayoutManager(context));
        videoListAdapter = new NoThumbnailAdapter(context, downloadedVideos, "downloadedFragment");
        videoListAdapter.setOnItemEventsListener(this);
        downloadedListView.setAdapter(videoListAdapter);

        //disable swipe to refresh for this tab
        v.findViewById(R.id.swipe_to_refresh).setEnabled(false);
        return v;
    }

    public void resumeAllListeners() {
        if (mPauseReceiver != null) {
            IntentFilter intentFilter = new IntentFilter(ACTION_PAUSE);
            getActivity().registerReceiver(mPauseReceiver, intentFilter);
        }
        if (mPlayReceiver != null) {
            IntentFilter intentFilter = new IntentFilter(ACTION_PLAY);
            getActivity().registerReceiver(mPlayReceiver, intentFilter);
        }
        if (mMediaChangeReceiver != null) {
            IntentFilter intentFilter = new IntentFilter(ACITON_VIDEO_CHANGE);
            getActivity().registerReceiver(mMediaChangeReceiver, intentFilter);
        }
        if (mSeekToReceiver != null) {
            IntentFilter intentFilter = new IntentFilter(ACTION_SEEK);
            getActivity().registerReceiver(mSeekToReceiver, intentFilter);
        }
        if (mPlaybackUpdatedReceiver != null) {
            IntentFilter intentFilter = new IntentFilter(ACTION_SEEKBAR_UPDATE);
            getActivity().registerReceiver(mPlaybackUpdatedReceiver, intentFilter);
        }
        if (mVideoUpdateReceiver != null) {
            IntentFilter intentFilter = new IntentFilter(ACTION_VIDEO_UPDATE);
            getActivity().registerReceiver(mVideoUpdateReceiver, intentFilter);
        }
        if (mStopReceiver != null) {
            IntentFilter intentFilter = new IntentFilter(ACTION_STOP);
            getActivity().registerReceiver(mStopReceiver, intentFilter);
        }
        mReceiversRegistered = true;

        downloadedVideos.clear();
        downloadedVideos.addAll(YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.DOWNLOADED).readAll(null, context));
        videoListAdapter.notifyDataSetChanged();
    }

    public void stopAllListeners(boolean resetTitleTextView) {
        if(mReceiversRegistered) {
            if (mPauseReceiver != null) {
                getActivity().unregisterReceiver(mPauseReceiver);
            }
            if (mPlayReceiver != null) {
                getActivity().unregisterReceiver(mPlayReceiver);
            }
            if (mMediaChangeReceiver != null) {
                getActivity().unregisterReceiver(mMediaChangeReceiver);
            }
            if (mSeekToReceiver != null) {
                getActivity().unregisterReceiver(mSeekToReceiver);
            }
            if (mPlaybackUpdatedReceiver != null) {
                getActivity().unregisterReceiver(mPlaybackUpdatedReceiver);
            }
            if (mVideoUpdateReceiver != null) {
                getActivity().unregisterReceiver(mVideoUpdateReceiver);
            }
            if (mStopReceiver != null) {
                getActivity().unregisterReceiver(mStopReceiver);
            }
            if(mediaPlayer != null) {
                stopPlayer(resetTitleTextView);
            }
        }
        mReceiversRegistered = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("on resume", "downloaded");
        resumeAllListeners();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAllListeners(true);;

    }

    public void setTitle(String title) {
        String aTitle = title + " " + getString(R.string.downloaded_tab);
        ((MainActivity)getActivity()).getmTitleTextView().setText(aTitle);
        ((MainActivity)getActivity()).getmTitleTextView().setSelected(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            this.context = context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(mediaPlayer != null) {
            stopPlayer(true);
        }
        this.context = null;
    }

    @Override
    public void onRemoveClicked(YouTubeVideo video) {
        Log.i("remove clicked","true");
        downloadedVideos.remove(video);
        String filename = video.getId();
        File[] files = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).listFiles();
        for (File file1 : files) {
            if (file1.getAbsolutePath().contains(filename)) {
                String fileToRemove = file1.getAbsolutePath();
                File file = new File(fileToRemove);
                boolean ignored = file.delete();
                break;
            }
        }
        YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.DOWNLOADED).delete(video.getId(), YouTubeSqlDb.VIDEOS_TYPE.DOWNLOADED.toString(), context);
        videoListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAddClicked(YouTubeVideo video) {
        Log.i("add clicked","download fragment");
        showPlaylistSelectionDialog(context, video);
    }


    @Override
    public void onItemClick(final YouTubeVideo video) {
        Log.i("item clicked", "downloaded");
        setmControlsTouched(false);
        if (fileExists(video)) {
            Intent serviceIntent = new Intent(getActivity(), BackgroundAudioService.class);
            serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);
            serviceIntent.putExtra(Config.YOUTUBE_TYPE, ItemType.MEDIA_LOCAL);
            serviceIntent.putExtra(Config.YOUTUBE_TYPE_VIDEO, video);
            serviceIntent.putExtra(Config.LOCAL_MEDIA_FILEAME, video.getId());
            serviceIntent.putExtra(Config.YOUTUBE_TYPE_PLAYLIST, (ArrayList) downloadedVideos);
            getActivity().startService(serviceIntent);
            startVideo(video, -1);
        } else if (SharedPrefs.getDownloadInProgress(context, video.getId())) {
            Toast.makeText(YTApplication.getAppContext(), "Media is still being downloaded",
                    Toast.LENGTH_SHORT).show();
        } else {
            downloadedVideos.remove(video);
            YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.DOWNLOADED).delete(video.getId(), YouTubeSqlDb.VIDEOS_TYPE.DOWNLOADED.toString(), context);
            videoListAdapter.notifyDataSetChanged();
            Toast.makeText(YTApplication.getAppContext(), "Unable to find media",
                    Toast.LENGTH_SHORT).show();
        }
        YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).create(video);


    }

    private void stopPlayer(boolean resetTitleTextView) {
        if(mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            vidSurface.setVisibility(View.GONE);
            setPlayListSize(false);

        }
    }

    private void startVideo(YouTubeVideo video, int progress) {

        try {
            final File[] files = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).listFiles();
            int index = -1;
            for (int i = 0; i < files.length; i++) {
                if (files[i].getAbsolutePath().contains(video.getId())) {
                    Log.i("file in download", files[i].getAbsolutePath());
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                try {
                    mediaPlayer = new MediaPlayer();
                    vidSurface.setVisibility(View.VISIBLE);
                    mediaPlayer.setDisplay(vidHolder);
                    mediaPlayer.setDataSource(Uri.parse(files[index].getAbsolutePath()).getPath());
                    mediaPlayer.prepare();
                    mediaPlayer.setOnPreparedListener(this);

                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MediaPlayer mediaPlayer;
    private SurfaceHolder vidHolder;
    private SurfaceView vidSurface;
    private MediaController mController;
    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_STOP = "action_stop";
    public static final String ACTION_SEEK = "action_seek";
    public static final String ACITON_VIDEO_CHANGE = "action_change_media";
    public static final String ACTION_SEEKBAR_UPDATE = "action_update";
    public static final String ACTION_VIDEO_UPDATE = "action_video_update";

    private BroadcastReceiver mVideoUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleIntent(intent);
        }
    };

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

    private BroadcastReceiver mMediaChangeReceiver = new BroadcastReceiver() {
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

    private BroadcastReceiver mPlaybackUpdatedReceiver = new BroadcastReceiver() {
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

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null)
            return;
        String action = intent.getAction();
        Log.i("action", action);
        if (action.equalsIgnoreCase(ACTION_PLAY)) {
            if(mediaPlayer!= null) {
                mediaPlayer.start();
            }
        } else if (action.equalsIgnoreCase(ACTION_PAUSE)) {
            if(mediaPlayer!= null) {
                mediaPlayer.pause();
            }
        } else if (action.equalsIgnoreCase(ACITON_VIDEO_CHANGE)) {
            String value = intent.getStringExtra("videoId");
                for (YouTubeVideo video : downloadedVideos) {
                    if (video.getId().equals(value)) {
                        startVideo(video, -1);
                        break;
                    }
                }
        } else if (action.equalsIgnoreCase(ACTION_SEEKBAR_UPDATE)) {
            if(((MainActivity)getActivity()).getmTitleTextView().getText().toString().contains("(DOWNLOADED")) {
                if(mediaPlayer == null) {
                    String value = intent.getStringExtra("videoId");
                    int progress = intent.getIntExtra("progress", -1);
                    for (YouTubeVideo video : downloadedVideos) {
                        if (video.getId().equals(value)) {
                            startVideo(video, progress);
                            break;
                        }
                    }
                } else {
                    int progress = intent.getIntExtra("progress", 0);
                    Log.i("progress in fragment", String.valueOf(progress));
                    if(mSeekAdjustmentRequired) {
                        Log.i("progress ss", String.valueOf(progress));
                        mediaPlayer.seekTo(progress * 1000);
                        mSeekAdjustmentRequired = false;

                    }
                }
            }
        } else if (action.equalsIgnoreCase(ACTION_STOP)) {
            stopPlayer(true);
        } else if (action.equalsIgnoreCase(ACTION_SEEK)) {
            int value = intent.getIntExtra("seekTo", 0);
            if(mediaPlayer!= null) {
                mediaPlayer.seekTo(value * 1000);
            }
        } else if (action.equalsIgnoreCase(ACTION_VIDEO_UPDATE)) {
            mSeekAdjustmentRequired = true;
            Log.i("setaction", "vid update fragment");
        }
    }

    @Override
    public void onDownloadClicked(YouTubeVideo video) {
        //do nothing
    }

    private void setPlayListSize(boolean firstTimeSet) {
        TypedValue tv = new TypedValue();
        int height;
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            height = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        } else {
            float density = getResources().getDisplayMetrics().density;
            height = (int) (50 * density);
        }

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mVideosContainer.getLayoutParams();
        float density = context.getResources().getDisplayMetrics().density;
        mTopMargin = height + (int) (6 * density);
        params.topMargin = mTopMargin;
        if(!firstTimeSet) {
            params.height = mContainerHeight;
            params.width = mContainerWidth;
        }

        params.gravity = Gravity.TOP;

        mVideosContainer.setLayoutParams(params);
    }

    private boolean fileExists(YouTubeVideo video) {
        String filename = video.getId();
        File[] files = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).listFiles();
        for (File file : files) {
            if (file.getAbsolutePath().contains(filename)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mediaPlayer.start();
        mediaPlayer.setVolume(0,0);
        setVideoSize();
        vidSurface.bringToFront();
        vidSurface.requestFocus();
        mController = new MediaController(context);
        mController.setMediaPlayer(this);
        mController.setAnchorView(vidSurface);

//        mController.show(0);
        vidSurface.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
    }

    private void setVideoSize() {

        // // Get the dimensions of the video
        int videoWidth = mediaPlayer.getVideoWidth();
        int videoHeight = mediaPlayer.getVideoHeight();

        // Get the width of the screen
        int width = SharedPrefs.getVideoContainerWidth(context);
        int containerHeight = SharedPrefs.getVideoContainerHeight(context);
        int height;

        // Get the SurfaceView layout parameters
        android.view.ViewGroup.LayoutParams lp = vidSurface.getLayoutParams();
        lp.width = width;
        if(videoWidth >= width) {
            height = videoHeight * width / videoHeight;
            if(height > containerHeight) {
                height = containerHeight;
            }
            lp.height = height;
        } else {
            height = videoHeight * width / videoWidth;
            if(height > containerHeight) {
                height = containerHeight;
            }
            lp.height = height;
        }


        vidSurface.setLayoutParams(lp);
        FrameLayout.LayoutParams videoContainerParams = (FrameLayout.LayoutParams) mVideosContainer.getLayoutParams();
        videoContainerParams.height = mContainerHeight - height + mTopMargin;
        videoContainerParams.gravity = Gravity.BOTTOM;
        mVideosContainer.setLayoutParams(videoContainerParams);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void start() {
        mediaPlayer.start();
    }

    @Override
    public void pause() {
        mediaPlayer.pause();
    }

    @Override
    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        mediaPlayer.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        stopPlayer(true);
    }
}

