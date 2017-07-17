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

import android.accounts.AccountManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.pawelpaszki.youtubeplus.database.YouTubeSqlDb;
import com.pawelpaszki.youtubeplus.fragments.DownloadedFragment;
import com.pawelpaszki.youtubeplus.fragments.PlayListsFragment;
import com.pawelpaszki.youtubeplus.fragments.RecentlyWatchedFragment;
import com.pawelpaszki.youtubeplus.fragments.SearchFragment;
import com.pawelpaszki.youtubeplus.interfaces.OnItemSelected;
import com.pawelpaszki.youtubeplus.model.ItemType;
import com.pawelpaszki.youtubeplus.model.YouTubeVideo;
import com.pawelpaszki.youtubeplus.utils.Config;
import com.pawelpaszki.youtubeplus.utils.NetworkConf;
import com.pawelpaszki.youtubeplus.utils.SharedPrefs;
import com.pawelpaszki.youtubeplus.viewPagers.NonSwipeableViewPager;
import com.pawelpaszki.youtubeplus.youtube.SuggestionsLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

import static com.pawelpaszki.youtubeplus.R.layout.suggestions;
import static com.pawelpaszki.youtubeplus.youtube.YouTubeSingleton.getCredential;

/**
 * Activity that manages fragments and action bar
 */
public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks,
        OnItemSelected{

    private static final String TAG = "SMEDIC MAIN ACTIVITY";
    private Toolbar toolbar;
    private ViewPager viewPager;

    public static final String PREF_ACCOUNT_NAME = "accountName";

    static final int REQUEST_ACCOUNT_PICKER = 1000;

    private SeekBar mDurationSeekbar;

    public TextView getmTitleTextView() {
        return mTitleTextView;
    }

    private TextView mTitleTextView;
    private ImageView mPreviousVideo;
    private ImageView mPlay;
    private ImageView mStop;
    private ImageView mNextVideo;
    private ImageView mLoopVideo;
    private boolean mIsPlaying;
    private boolean mHasPlaybackStarted;
    private int mProgressSet;
    private int mPausedAt;
    private int mControlsTouchedAt;

    private ArrayList<FloatingActionButton> mControls = new ArrayList<>();
    private FloatingActionButton mGoToDownloads;
    private FloatingActionButton mGoToRecent;
    private FloatingActionButton mGoToSearch;
    private FloatingActionButton mGoToPlaylist;

    private SearchFragment searchFragment;
    private RecentlyWatchedFragment recentlyPlayedFragment;
    private PlayListsFragment playListsFragment;
    private DownloadedFragment downloadedFragment;

    public static void setmControlsTouched(boolean mControlsTouched) {
        MainActivity.mControlsTouched = mControlsTouched;
    }

    private static boolean mControlsTouched;


    private static final String ACTION_PLAYBACK_STARTED = "playbackStarted";
    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_SEEK = "action_seek";
    public static final String ACTION_SEEKBAR_UPDATE = "action_update";
    public static final String ACTION_VIDEO_UPDATE = "action_video_update";
    public static final String ACTION_STOP = "action_stop";

    private BroadcastReceiver mPlaybackStartedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("extras", intent.getStringExtra("duration"));
            String currentlyPlayedFragment = mTitleTextView.getText().toString();

            setDuration(intent.getStringExtra("duration"));
            String title = intent.getStringExtra("title");

            Log.i("test ### title", currentlyPlayedFragment);
            Log.i("test ### ctrl touched", String.valueOf(mControlsTouched));
            if(mControlsTouched) {
                mControlsTouched = false;
                if(currentlyPlayedFragment.contains("(DOWNLOADED")) {
                    downloadedFragment.setTitle(title);
                } else if (currentlyPlayedFragment.contains("(PLAYLISTS")) {
                    playListsFragment.setTitle(title);
                } else if (currentlyPlayedFragment.contains("(RECENT")) {
                    recentlyPlayedFragment.setTitle(title);
                } else if (currentlyPlayedFragment.contains("(SEARCH")) {
                    searchFragment.setTitle(title);
                }
            } else {
                switch(viewPager.getCurrentItem()) {
                    case 0:
                        downloadedFragment.setTitle(title);
                        break;
                    case 1:
                        playListsFragment.setTitle(title);
                        break;
                    case 2:
                        recentlyPlayedFragment.setTitle(title);
                        break;
                    case 3:
                        searchFragment.setTitle(title);
                        break;
                }
            }
        }
    };

    private BroadcastReceiver mPlaybackUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int progress = intent.getIntExtra("progress", 0);
            //Log.i("progress received", String.valueOf(progress));
            if(progress != mProgressSet && progress != mPausedAt) {
                setPauseIcon();
                setControlsEnabled(true);
                mDurationSeekbar.setProgress(progress);
            }

        }
    };

    private NetworkConf networkConf;
    private GestureDetectorCompat mGestureDetector;
    private boolean mControlsVisible;
    private FloatingActionButton mHideControls;
    private RelativeLayout mHomeContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        YouTubeSqlDb.getInstance().init(this);

        mTitleTextView = (TextView) findViewById(R.id.title);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setBackground(new ColorDrawable(Color.parseColor("#980000")));

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        minimiseToolbar();

        mGestureDetector = new GestureDetectorCompat(this, new MyGestureListener());
        mHomeContainer = (RelativeLayout) findViewById(R.id.main_container);
        mHomeContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mGestureDetector.onTouchEvent(event);
                return false;
            }
        });

        viewPager = (NonSwipeableViewPager) findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(3);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if(position == 3) {
                    expandToolbar();
                } else {
                    ViewGroup.LayoutParams params = toolbar.getLayoutParams();
                    params.height = 0;
                    toolbar.setLayoutParams(params);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // do nothing
            }
        });
        setupViewPager(viewPager);

        networkConf = new NetworkConf(this);

        if (mPlaybackStartedReceiver != null) {
            IntentFilter intentFilter = new IntentFilter(ACTION_PLAYBACK_STARTED);
            registerReceiver(mPlaybackStartedReceiver, intentFilter);
        }

        if (mPlaybackUpdated != null) {
            IntentFilter intentFilter = new IntentFilter(ACTION_SEEKBAR_UPDATE);
            registerReceiver(mPlaybackUpdated, intentFilter);
        }

        mDurationSeekbar = (SeekBar) findViewById(R.id.seekBar);

        mDurationSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(mIsPlaying) {
                    //Log.i("progress set", String.valueOf(mDurationSeekbar.getProgress()));
                    mProgressSet = mDurationSeekbar.getProgress();
                    sendBroadcast("seek");
                    if(viewPager.getCurrentItem() != 0) {
                        setPlayIconAndDisableControls(true);
                    }
                }
            }
        });
        mDurationSeekbar.setEnabled(false);

        mPreviousVideo = (ImageView) findViewById(R.id.previous);
        mPreviousVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mHasPlaybackStarted) {
                    mControlsTouchedAt = viewPager.getCurrentItem();
                    mControlsTouched = true;
                    sendBroadcast("previous");
                    mDurationSeekbar.setProgress(0);
                    setPlayIconAndDisableControls(true);
                }
            }
        });

        mPlay = (ImageView) findViewById(R.id.play);

        mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mHasPlaybackStarted) {
                    if(mIsPlaying) {
                        mIsPlaying = false;
                        mDurationSeekbar.setEnabled(false);
                        sendBroadcast("pause");
                        mPausedAt = mDurationSeekbar.getProgress();
                        setPlayIconAndDisableControls(false);
                    } else {
                        sendBroadcast("play");
                        setPauseIcon();
                        mDurationSeekbar.setEnabled(true);
                        mIsPlaying = true;
                    }
                }
            }
        });
        mNextVideo = (ImageView) findViewById(R.id.next);
        mNextVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mHasPlaybackStarted) {
                    mControlsTouchedAt = viewPager.getCurrentItem();
                    mControlsTouched = true;
                    sendBroadcast("next");
                    setPlayIconAndDisableControls(true);
                    mDurationSeekbar.setProgress(0);
                }
            }
        });

        mStop = (ImageView) findViewById(R.id.stop);
        mStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mHasPlaybackStarted) {
                    sendBroadcast("stop");
                    setPlayIconAndDisableControls(true);
                    mDurationSeekbar.setProgress(0);
                }
            }
        });

        mLoopVideo = (ImageView) findViewById(R.id.loop);
        setIsLoopingIcon(false);
        mLoopVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setIsLoopingIcon(true);
            }
        });

        mGoToDownloads = (FloatingActionButton) findViewById(R.id.downloaded);
        mGoToPlaylist = (FloatingActionButton) findViewById(R.id.playlist);
        mGoToRecent = (FloatingActionButton) findViewById(R.id.recent);
        mGoToSearch = (FloatingActionButton) findViewById(R.id.search);
        mHideControls = (FloatingActionButton) findViewById(R.id.hide);
        mControls.add(mGoToDownloads);
        mControls.add(mGoToPlaylist);
        mControls.add(mGoToRecent);
        mControls.add(mGoToSearch);
        mControls.add(mHideControls);
        for(int i = 0; i < mControls.size() - 1; i++) {
            final int j = i;
            mControls.get(i).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // hide keyboard
//                    InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//                    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    Log.i("page selected", String.valueOf(j));
                    if(j!= 0) {
                        downloadedFragment.stopAllListeners(false);
                    } else {
                        if(!mTitleTextView.getText().toString().contains("(RECENT)") && !mTitleTextView.getText().toString().contains("(SEARCH)") &&
                        !mTitleTextView.getText().toString().contains("(PLAYLISTS)")) {
                            Log.i("resume listeners", "true");
                            downloadedFragment.resumeAllListeners();
                        }

                    }

                    viewPager.setCurrentItem(j, false);
                    setButtonBackgroundTint();
                }
            });
            mControls.get(i).setVisibility(View.GONE);
        }
        mGoToDownloads.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF8A80")));
        mHideControls.setVisibility(View.GONE);

    }

    private void setButtonBackgroundTint() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                for(int i = 0; i < mControls.size() - 1; i++) {
                    if(i == viewPager.getCurrentItem()) {
                        mControls.get(i).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF8A80")));
                    } else {
                        mControls.get(i).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#00000000")));
                    }
                }
            }
        }, 1);

    }

    private void minimiseToolbar() {
        ViewGroup.LayoutParams params = toolbar.getLayoutParams();
        params.height = 0;
        toolbar.setLayoutParams(params);
    }

    private void expandToolbar() {
        TypedValue tv = new TypedValue();
        int height;
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            height = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        } else {
            float density = getResources().getDisplayMetrics().density;
            height = (int) (30 * density);
        }

        ViewGroup.LayoutParams params = toolbar.getLayoutParams();
        params.height = height;
        toolbar.setLayoutParams(params);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return false;
    }

    public void hideControls(View view) {
        showNavigationButtons();
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            if(event1 != null && event2 != null) {
                float diffY = event2.getY() - event1.getY();
                float diffX = event2.getX() - event1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            showNavigationButtons();
                            //TODO show buttons
                        } else {
                            return false;
                        }
                    }
                }
                return true;
            }
            return false;
        }
    }

    private void showNavigationButtons() {
        if(mControlsVisible) {
            for(int i = 100, j = 4; i <= 500; i = i + 100, j--) {
                final int jj = j;
                mControls.get(j).setVisibility(View.VISIBLE);
                final Animation fadeIn = new AlphaAnimation(1,0);
                fadeIn.setStartOffset(i);
                fadeIn.setDuration(i);
                mControls.get(j).setAnimation(fadeIn);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable(){
                    @Override
                    public void run()
                    {
                        mControls.get(jj).setVisibility(View.GONE);
                    }
                }, i * 2);
            }
        } else {
            for(int i = 100, j = 0; i <= 500; i = i + 100, j++) {
                mControls.get(j).setVisibility(View.VISIBLE);
                final Animation fadeIn = new AlphaAnimation(0,1);
                fadeIn.setStartOffset(i);
                fadeIn.setDuration(i);
                mControls.get(j).setAnimation(fadeIn);
            }
        }
        mControlsVisible = !mControlsVisible;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mPlaybackStartedReceiver != null) {
            unregisterReceiver(mPlaybackStartedReceiver);
        }
        if(mPlaybackUpdated != null) {
            unregisterReceiver(mPlaybackUpdated);
        }
        try {
            YouTubeSqlDb.YouTubeDbHelper.getInstance(this).finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

    }

    private void setIsLoopingIcon(boolean doUpdate) {
        boolean isLooping = SharedPrefs.getIsLooping(MainActivity.this);
        if(doUpdate) {
            SharedPrefs.setIsLooping(!isLooping, this);
            isLooping = !isLooping;
        }

        if(isLooping) {
            mLoopVideo.setColorFilter(Color.argb(0, 0, 0, 0));
        } else {
            mLoopVideo.setColorFilter(Color.argb(255, 255, 255, 255));
        }
        Log.i("is looping", String.valueOf(SharedPrefs.getIsLooping(MainActivity.this)));

    }

    private void setControlsVisible(boolean visible) {
        int visibility;
        if(visible) {
            visibility = View.VISIBLE;
        } else {
            visibility = View.GONE;
        }
        mPlay.setVisibility(visibility);
        mNextVideo.setVisibility(visibility);
        mPreviousVideo.setVisibility(visibility);
        mDurationSeekbar.setVisibility(visibility);
        mLoopVideo.setVisibility(visibility);
    }

    private void sendBroadcast(String action) {
        Intent new_intent = new Intent();

        switch(action) {
            case "play":
                new_intent.setAction(ACTION_PLAY);
                break;
            case "pause":
                new_intent.setAction(ACTION_PAUSE);
                break;
            case "next":
                new_intent.setAction(ACTION_NEXT);
                break;
            case "previous":
                new_intent.setAction(ACTION_PREVIOUS);
                break;
            case "seek":
                new_intent.setAction(ACTION_SEEK);
                new_intent.putExtra("seekTo", mProgressSet);
                break;
            case "stop":
                new_intent.setAction(ACTION_STOP);
                if(viewPager.getCurrentItem() != 0) {
                    clearTitleTextView();
                }
                break;
        }
        sendBroadcast(new_intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable(){
            @Override
            public void run()
            {
                Intent new_intent = new Intent();
                new_intent.setAction(ACTION_VIDEO_UPDATE);
                sendBroadcast(new_intent);
                Log.i("setaction", "vid update activity");
            }
        }, 2000);

    }

    private void clearTitleTextView() {
        mTitleTextView = (TextView) findViewById(R.id.title);
        mTitleTextView.setText("");
        mTitleTextView.setSelected(true);
    }

    private void setControlsEnabled(boolean value) {
        mPlay.setEnabled(value);
        mNextVideo.setEnabled(value);
        mPreviousVideo.setEnabled(value);
        mDurationSeekbar.setEnabled(value);
    }

    /**
     * sets max value of seekbar
     */
    private void setDuration(String duration) {
        try {
            String[] values = duration.split(":");
            int videoDuration;
            if(values.length > 0) {
                if(values.length == 3) {
                    videoDuration = Integer.parseInt(values[2]) + 60 * Integer.parseInt(values[1]) + 3600 * Integer.parseInt(values[0]);
                } else {
                    videoDuration = Integer.parseInt(values[1]) + 60 * Integer.parseInt(values[0]);
                }
                mDurationSeekbar.setMax(videoDuration);
                setControlsVisible(true);
                Resources res = getResources();
                Drawable pause = res.getDrawable(R.drawable.ic_pause);
                mPlay.setImageDrawable(pause);
                setControlsEnabled(true);
                mIsPlaying = true;
                mHasPlaybackStarted = true;
            }

        } catch (Exception e) {
            setControlsEnabled(false);
        }
    }

//    /**
//     * Attempts to set the account used with the API credentials. If an account
//     * name was previously saved it will use that one; otherwise an account
//     * picker dialog will be shown to the user. Note that the setting the
//     * account to use with the credentials object requires the app to have the
//     * GET_ACCOUNTS permission, which is requested here if it is not already
//     * present. The AfterPermissionGranted annotation indicates that this
//     * function will be rerun automatically whenever the GET_ACCOUNTS permission
//     * is granted.
//     */
//    @AfterPermissionGranted(PERMISSIONS)
//    private void requestPermissions() {
//        String[] perms = {Manifest.permission.GET_ACCOUNTS, Manifest.permission.READ_PHONE_STATE};
//        if (EasyPermissions.hasPermissions(this, perms)) {
//            // Already have permission, do the thing
//            if (EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS)) {
//                String accountName = getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
//                if (accountName != null) {
//                    getCredential().setSelectedAccountName(accountName);
//                } else {
//                    // Start a dialog from which the user can choose an account
//                    startActivityForResult(
//                            getCredential().newChooseAccountIntent(),
//                            REQUEST_ACCOUNT_PICKER);
//                }
//            } else {
//                // Request the GET_ACCOUNTS permission via a user dialog
//                EasyPermissions.requestPermissions(
//                        this,
//                        "This app needs to access your Google account (via Contacts).",
//                        REQUEST_PERMISSION_GET_ACCOUNTS,
//                        Manifest.permission.GET_ACCOUNTS);
//            }
//        } else {
//            // Do not have permissions, request them now
//            EasyPermissions.requestPermissions(this, getString(R.string.all_permissions_request),
//                    PERMISSIONS, perms);
//        }
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ACCOUNT_PICKER) {
            if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                if (accountName != null) {
                    SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(PREF_ACCOUNT_NAME, accountName);
                    editor.apply();
                    getCredential().setSelectedAccountName(accountName);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * Override super.onNewIntent() so that calls to getIntent() will return the
     * latest intent that was used to start this Activity rather than the first
     * intent.
     */
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    /**
     * Handle search intent and queries YouTube for videos
     *
     * @param intent
     */
    private void handleIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);

            if (searchFragment != null) {
                searchFragment.searchQuery(query);
            }
        }
    }


    /**
     * Setups viewPager for switching between pages according to the selected tab
     *
     * @param viewPager
     */
    private void setupViewPager(ViewPager viewPager) {

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        searchFragment = SearchFragment.newInstance();
        playListsFragment = PlayListsFragment.newInstance();
        recentlyPlayedFragment = RecentlyWatchedFragment.newInstance();
        downloadedFragment = DownloadedFragment.newInstance();

        adapter.addFragment(downloadedFragment, null);
        adapter.addFragment(playListsFragment, null);
        adapter.addFragment(recentlyPlayedFragment, null);
        adapter.addFragment(searchFragment, null);
        viewPager.setAdapter(adapter);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:");
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied: ");
    }

    @Override
    public void onVideoSelected(YouTubeVideo video) {
        if (!networkConf.isNetworkAvailable()) {
            networkConf.createNetErrorDialog();
            return;
        }
        setPlayIconAndDisableControls(true);
        Intent serviceIntent = new Intent(this, BackgroundAudioService.class);
        serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);
        serviceIntent.putExtra(Config.YOUTUBE_TYPE, ItemType.YOUTUBE_MEDIA_TYPE_VIDEO);
        serviceIntent.putExtra(Config.YOUTUBE_TYPE_VIDEO, video);
        startService(serviceIntent);
    }

    @Override
    public void onPlaylistSelected(List<YouTubeVideo> playlist, int position) {
        if (!networkConf.isNetworkAvailable()) {
            networkConf.createNetErrorDialog();
            return;
        }
        setPlayIconAndDisableControls(true);
        Intent serviceIntent = new Intent(this, BackgroundAudioService.class);
        serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);
        serviceIntent.putExtra(Config.YOUTUBE_TYPE, ItemType.YOUTUBE_MEDIA_TYPE_PLAYLIST);
        serviceIntent.putExtra(Config.YOUTUBE_TYPE_PLAYLIST, (ArrayList) playlist);
        serviceIntent.putExtra(Config.YOUTUBE_TYPE_PLAYLIST_VIDEO_POS, position);
        startService(serviceIntent);
    }

    private void setPlayIconAndDisableControls(boolean disable) {
        Resources res = getResources();
        Drawable play = res.getDrawable(R.drawable.ic_play);
        mPlay.setImageDrawable(play);
        if(disable) {
            setControlsEnabled(false);
        }
    }

    private void setPauseIcon() {
        Resources res = getResources();
        Drawable pause = res.getDrawable(R.drawable.ic_pause);
        mPlay.setImageDrawable(pause);
    }

    /**
     * Class which provides adapter for fragment pager
     */
    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }

    }

    /**
     * Options menu in action bar
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }

        //suggestions
        final CursorAdapter suggestionAdapter = new SimpleCursorAdapter(this,
                suggestions,
                null,
                new String[]{SearchManager.SUGGEST_COLUMN_TEXT_1},
                new int[]{android.R.id.text1},
                0);
        final List<String> suggestions = new ArrayList<>();

        searchView.setSuggestionsAdapter(suggestionAdapter);

        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                searchView.setQuery(suggestions.get(position), false);
                searchView.clearFocus();

                Intent suggestionIntent = new Intent(Intent.ACTION_SEARCH);
                suggestionIntent.putExtra(SearchManager.QUERY, suggestions.get(position));
                handleIntent(suggestionIntent);

                return true;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false; //if true, no new intent is started
            }

            @Override
            public boolean onQueryTextChange(final String query) {
                // check network connection. If not available, do not query.
                // this also disables onSuggestionClick triggering
                if (query.length() > 2) { //make suggestions after 3rd letter
                    if (networkConf.isNetworkAvailable()) {

                        getSupportLoaderManager().restartLoader(4, null, new LoaderManager.LoaderCallbacks<List<String>>() {
                            @Override
                            public Loader<List<String>> onCreateLoader(final int id, final Bundle args) {
                                return new SuggestionsLoader(getApplicationContext(), query);
                            }

                            @Override
                            public void onLoadFinished(Loader<List<String>> loader, List<String> data) {
                                if (data == null)
                                    return;
                                suggestions.clear();
                                suggestions.addAll(data);
                                String[] columns = {
                                        BaseColumns._ID,
                                        SearchManager.SUGGEST_COLUMN_TEXT_1
                                };
                                MatrixCursor cursor = new MatrixCursor(columns);

                                for (int i = 0; i < data.size(); i++) {
                                    String[] tmp = {Integer.toString(i), data.get(i)};
                                    cursor.addRow(tmp);
                                }
                                suggestionAdapter.swapCursor(cursor);
                            }

                            @Override
                            public void onLoaderReset(Loader<List<String>> loader) {
                                suggestions.clear();
                                suggestions.addAll(Collections.<String>emptyList());
                            }
                        }).forceLoad();
                        return true;
                    }
                }
                return false;
            }
        });

        return true;
    }

    /**
     * Handles selected item from action bar
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_search) {
            MenuItemCompat.expandActionView(item);
            return true;
        }

        return super.onOptionsItemSelected(item);

    }
}