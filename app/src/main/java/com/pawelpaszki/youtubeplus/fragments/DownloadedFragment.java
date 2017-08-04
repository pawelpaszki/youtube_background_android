package com.pawelpaszki.youtubeplus.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.DisplayMetrics;
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
import android.widget.Toast;

import com.pawelpaszki.youtubeplus.BackgroundAudioService;
import com.pawelpaszki.youtubeplus.MainActivity;
import com.pawelpaszki.youtubeplus.R;
import com.pawelpaszki.youtubeplus.YTApplication;
import com.pawelpaszki.youtubeplus.adapters.NoThumbnailAdapter;
import com.pawelpaszki.youtubeplus.adapters.SimpleItemTouchHelperCallback;
import com.pawelpaszki.youtubeplus.database.YouTubeSqlDb;
import com.pawelpaszki.youtubeplus.interfaces.ItemEventsListener;
import com.pawelpaszki.youtubeplus.model.ItemType;
import com.pawelpaszki.youtubeplus.model.YouTubeVideo;
import com.pawelpaszki.youtubeplus.utils.Config;
import com.pawelpaszki.youtubeplus.utils.SharedPrefs;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

import static com.pawelpaszki.youtubeplus.MainActivity.DOWNLOADED;
import static com.pawelpaszki.youtubeplus.MainActivity.fragmentName;
import static com.pawelpaszki.youtubeplus.adapters.NoThumbnailAdapter.downloadedRearranged;
import static com.pawelpaszki.youtubeplus.adapters.SimpleItemTouchHelperCallback.setIsLongPressEnabled;
import static com.pawelpaszki.youtubeplus.dialogs.AddToPlayListDialog.showPlaylistSelectionDialog;
import static com.pawelpaszki.youtubeplus.utils.Config.ACITON_VIDEO_CHANGE;
import static com.pawelpaszki.youtubeplus.utils.Config.ACTION_PAUSE;
import static com.pawelpaszki.youtubeplus.utils.Config.ACTION_PLAY;
import static com.pawelpaszki.youtubeplus.utils.Config.ACTION_SEEK;
import static com.pawelpaszki.youtubeplus.utils.Config.ACTION_SEEKBAR_UPDATE;
import static com.pawelpaszki.youtubeplus.utils.Config.ACTION_STOP;
import static com.pawelpaszki.youtubeplus.utils.Config.ACTION_VIDEO_UPDATE;

/**
 * Created by PawelPaszki on 04/07/2017.
 *
 * Used to handle downloaded media
 */

public class DownloadedFragment extends BaseFragment implements ItemEventsListener<YouTubeVideo>, SurfaceHolder.Callback, MediaPlayer.OnPreparedListener, MediaController.MediaPlayerControl, MediaPlayer.OnCompletionListener {

    private ArrayList<YouTubeVideo> downloadedVideos;

    private NoThumbnailAdapter videoListAdapter;
    private Context context;
    private int mContainerHeight;
    private int mContainerWidth;
    private LinearLayout mVideosContainer;
    private int mTopMargin;
    private boolean mReceiversRegistered;
    private RecyclerView downloadedListView;

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

        ItemTouchHelper.Callback callback =
                new SimpleItemTouchHelperCallback(videoListAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(downloadedListView);

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
            intentFilter.setPriority(999);
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
            intentFilter.setPriority(999);
            getActivity().registerReceiver(mPlaybackUpdatedReceiver, intentFilter);
        }
        if (mVideoUpdateReceiver != null) {
            IntentFilter intentFilter = new IntentFilter(ACTION_VIDEO_UPDATE);
            intentFilter.setPriority(999);
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
                stopPlayer();
            }
        }
        mReceiversRegistered = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeAllListeners();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAllListeners(true);

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
            stopPlayer();
        }
        this.context = null;
    }

    @Override
    public void onRemoveClicked(YouTubeVideo video) {
        Log.i("remove clicked","true");
        if(!fragmentName.contains(video.getTitle())) {
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
        } else {
            Toast.makeText(YTApplication.getAppContext(), "Cannot remove currently played video",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAddClicked(YouTubeVideo video) {
        //Log.i("add clicked","download fragment");
        showPlaylistSelectionDialog(context, video);
    }


    @Override
    public void onItemClick(final YouTubeVideo video) {
        //Log.i("item clicked", "downloaded");
        fragmentName = DOWNLOADED;
        if (fileExists(video)) {
            Intent serviceIntent = new Intent(getActivity(), BackgroundAudioService.class);
            serviceIntent.setAction(ACTION_PLAY);
            serviceIntent.putExtra(Config.YOUTUBE_TYPE, ItemType.MEDIA_LOCAL);
            serviceIntent.putExtra(Config.YOUTUBE_TYPE_VIDEO, video);
            serviceIntent.putExtra(Config.LOCAL_MEDIA_FILEAME, video.getId());
            serviceIntent.putExtra(Config.YOUTUBE_TYPE_PLAYLIST, (ArrayList) downloadedVideos);
            getActivity().startService(serviceIntent);
            setIsLongPressEnabled(false);
            if(downloadedRearranged) {
                downloadedRearranged = false;
                refreshDB();
            }
            startVideo(video);
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

    public void refreshDB() {
        YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.DOWNLOADED).deleteAll();
        for(int i = videoListAdapter.getVideoList().size() - 1; i >= 0; i--) {
            YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.DOWNLOADED).create(videoListAdapter.getIds().get(i));
        }
        videoListAdapter.notifyDataSetChanged();
    }

    private void stopPlayer() {
        if(mediaPlayer!=null)
        {
            if(mediaPlayer.isPlaying()){
                mediaPlayer.stop();// Stop it
                mediaPlayer.release();// Release it
                mediaPlayer=null; // Initilize to null so it can be used later
            }
        }
        vidSurface.setVisibility(View.GONE);
        setPlayListSize(false);

    }

    private void startVideo(YouTubeVideo video) {
        stopPlayer();
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
        //Log.i("action", action);
        if (action.equalsIgnoreCase(ACTION_PLAY)) {
            if (fragmentName.equals(DOWNLOADED)) {
                if (mediaPlayer != null) {
                    mediaPlayer.start();
                }
            } else {
                if (mediaPlayer != null) {
                    stopPlayer();
                }
            }
        } else if (action.equalsIgnoreCase(ACTION_PAUSE)) {
            if(mediaPlayer!= null && fragmentName.equals(DOWNLOADED)) {
                mediaPlayer.pause();
            }
        } else if (action.equalsIgnoreCase(ACITON_VIDEO_CHANGE)) {
            if(fragmentName.equals(DOWNLOADED)) {
                String value = intent.getStringExtra("videoId");
                for (YouTubeVideo video : downloadedVideos) {
                    if (video.getId().equals(value)) {
                        startVideo(video);
                        break;
                    }
                }
            }

        } else if (action.equalsIgnoreCase(ACTION_SEEKBAR_UPDATE)) {
            if(fragmentName.equals(DOWNLOADED)) {
                if(mediaPlayer == null) {
                    String value = intent.getStringExtra("videoId");
                    for (YouTubeVideo video : downloadedVideos) {
                        if (video.getId().equals(value)) {
                            startVideo(video);
                            break;
                        }
                    }
                } else {
                    int progress = intent.getIntExtra("progress", 0);
//                    long fragmentTime = System.currentTimeMillis();
//                    long broadcastTime= intent.getLongExtra("timestamp", System.currentTimeMillis());
//                    Log.i("timestamp broadcast", String.valueOf(broadcastTime));
//                    Log.i("timestamp fragment", String.valueOf(fragmentTime));
//                    Log.i("progress in fragment", String.valueOf(progress));
                    int mediaPlayerProgress = mediaPlayer.getCurrentPosition();
                    Log.i("mplayer progress before", String.valueOf(mediaPlayerProgress));


//                    if(mSeekAdjustmentRequired) {
//                        mediaPlayer.seekTo(progress);
//                        mSeekAdjustmentRequired = false;
//                    } else
                        if(progress > 10000 && Math.abs(progress - mediaPlayerProgress) > 300) {
                        mediaPlayer.seekTo(progress);
                    }
                    Log.i("mplayer progress after", String.valueOf(mediaPlayerProgress));
                }
            } else {
                if (mediaPlayer != null) {
                    stopPlayer();
                }
            }
        } else if (action.equalsIgnoreCase(ACTION_STOP)) {
            stopPlayer();
        } else if (action.equalsIgnoreCase(ACTION_SEEK)) {
            int value = intent.getIntExtra("seekTo", 0);
            if(mediaPlayer!= null && fragmentName.equals(DOWNLOADED)) {
                mediaPlayer.seekTo(value);
            }
        } else if (action.equalsIgnoreCase(ACTION_VIDEO_UPDATE)) {
            //Log.i("setaction", "vid update fragment");
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
        MediaController mController = new MediaController(context);
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
        stopPlayer();
    }
}

