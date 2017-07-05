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

import android.Manifest;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.MatrixCursor;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.transition.Visibility;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import com.pawelpaszki.youtubeplus.database.YouTubeSqlDb;
import com.pawelpaszki.youtubeplus.fragments.DownloadedFragment;
import com.pawelpaszki.youtubeplus.fragments.FavoritesFragment;
import com.pawelpaszki.youtubeplus.fragments.PlaylistsFragment;
import com.pawelpaszki.youtubeplus.fragments.RecentlyWatchedFragment;
import com.pawelpaszki.youtubeplus.fragments.SearchFragment;
import com.pawelpaszki.youtubeplus.interfaces.OnFavoritesSelected;
import com.pawelpaszki.youtubeplus.interfaces.OnItemSelected;
import com.pawelpaszki.youtubeplus.model.ItemType;
import com.pawelpaszki.youtubeplus.model.YouTubeVideo;
import com.pawelpaszki.youtubeplus.utils.Config;
import com.pawelpaszki.youtubeplus.utils.NetworkConf;
import com.pawelpaszki.youtubeplus.utils.SharedPrefs;
import com.pawelpaszki.youtubeplus.youtube.SuggestionsLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static com.pawelpaszki.youtubeplus.R.layout.suggestions;
import static com.pawelpaszki.youtubeplus.youtube.YouTubeSingleton.getCredential;

/**
 * Activity that manages fragments and action bar
 */
public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks,
        OnItemSelected, OnFavoritesSelected {

    private static final String TAG = "SMEDIC MAIN ACTIVITY";
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    private static final int PERMISSIONS = 1;
    public static final String PREF_ACCOUNT_NAME = "accountName";

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private SeekBar mDurationSeekbar;
    private ImageView mPreviousVideo;
    private ImageView mPlay;
    private ImageView mNextVideo;
    private ImageView mLoopVideo;
    private boolean mIsPlaying;
    private boolean mHasPlaybackStarted;
    private int mProgressSet;
    private int mPausedAt;


    private SearchFragment searchFragment;
    private RecentlyWatchedFragment recentlyPlayedFragment;
    private FavoritesFragment favoritesFragment;
    private DownloadedFragment downloadedFragment;

    private static final String ACTION_PLAYBACK_STARTED = "playbackStarted";
    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_SEEK = "action_seek";
    public static final String ACTION_SEEKBAR_UPDATE = "action_update";

    private int[] tabIcons = {
            R.drawable.ic_downloaded,
            R.drawable.ic_star,
            R.drawable.ic_recently_wached,
            R.drawable.ic_search,
            R.drawable.ic_action_playlist
    };

    private BroadcastReceiver mPlaybackStartedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.i("extras", intent.getStringExtra("duration"));
            setDuration(intent.getStringExtra("duration"));
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        YouTubeSqlDb.getInstance().init(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(3);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

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
                    setPlayIconAndDisableControls(true);
                }
            }
        });
        mDurationSeekbar.setEnabled(false);

        mPreviousVideo = (ImageView) findViewById(R.id.previous);
        mPreviousVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mHasPlaybackStarted) {
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
                    sendBroadcast("next");
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
        setControlsVisible(false);
        setupTabIcons();

        requestPermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mPlaybackStartedReceiver != null) {
            unregisterReceiver(mPlaybackStartedReceiver);
        }
    }

    private void setIsLoopingIcon(boolean doUpdate) {
        boolean isLooping = SharedPrefs.getIsLooping(MainActivity.this);
        if(doUpdate) {
            SharedPrefs.setIsLooping(!isLooping, this);
            isLooping = !isLooping;
        }
        Resources res = getResources();

        Drawable icon;
        if(isLooping) {
            icon = res.getDrawable(R.drawable.ic_loop_selected);
        } else {
            icon = res.getDrawable(R.drawable.ic_loop);
        }
        mLoopVideo.setImageDrawable(icon);
        //Log.i("is looping", String.valueOf(SharedPrefs.getIsLooping(MainActivity.this)));

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
        }
        sendBroadcast(new_intent);
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
                Drawable pause = res.getDrawable(android.R.drawable.ic_media_pause);
                mPlay.setImageDrawable(pause);
                setControlsEnabled(true);
                mIsPlaying = true;
                mHasPlaybackStarted = true;
            }

        } catch (Exception e) {
            setControlsEnabled(false);
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(PERMISSIONS)
    private void requestPermissions() {
        String[] perms = {Manifest.permission.GET_ACCOUNTS, Manifest.permission.READ_PHONE_STATE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            // Already have permission, do the thing
            if (EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS)) {
                String accountName = getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
                if (accountName != null) {
                    getCredential().setSelectedAccountName(accountName);
                } else {
                    // Start a dialog from which the user can choose an account
                    startActivityForResult(
                            getCredential().newChooseAccountIntent(),
                            REQUEST_ACCOUNT_PICKER);
                }
            } else {
                // Request the GET_ACCOUNTS permission via a user dialog
                EasyPermissions.requestPermissions(
                        this,
                        "This app needs to access your Google account (via Contacts).",
                        REQUEST_PERMISSION_GET_ACCOUNTS,
                        Manifest.permission.GET_ACCOUNTS);
            }
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.all_permissions_request),
                    PERMISSIONS, perms);
        }
    }

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

            viewPager.setCurrentItem(2, true); //switch to search fragment

            if (searchFragment != null) {
                searchFragment.searchQuery(query);
            }
        }
    }

    /**
     * Setups icons for 3 tabs
     */
    private void setupTabIcons() {
        tabLayout.getTabAt(0).setIcon(tabIcons[0]);
        tabLayout.getTabAt(1).setIcon(tabIcons[1]);
        tabLayout.getTabAt(2).setIcon(tabIcons[2]);
        tabLayout.getTabAt(3).setIcon(tabIcons[3]);
        tabLayout.getTabAt(4).setIcon(tabIcons[4]);
    }

    /**
     * Setups viewPager for switching between pages according to the selected tab
     *
     * @param viewPager
     */
    private void setupViewPager(ViewPager viewPager) {

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        searchFragment = SearchFragment.newInstance();
        recentlyPlayedFragment = RecentlyWatchedFragment.newInstance();
        favoritesFragment = FavoritesFragment.newInstance();
        downloadedFragment = DownloadedFragment.newInstance();
        PlaylistsFragment playlistsFragment = PlaylistsFragment.newInstance();

        adapter.addFragment(downloadedFragment, null);
        adapter.addFragment(favoritesFragment, null);
        adapter.addFragment(recentlyPlayedFragment, null);
        adapter.addFragment(searchFragment, null);
        adapter.addFragment(playlistsFragment, null);
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
        Drawable play = res.getDrawable(android.R.drawable.ic_media_play);
        mPlay.setImageDrawable(play);
        if(disable) {
            setControlsEnabled(false);
        }
    }

    private void setPauseIcon() {
        Resources res = getResources();
        Drawable pause = res.getDrawable(android.R.drawable.ic_media_pause);
        mPlay.setImageDrawable(pause);
    }



    @Override
    public void onFavoritesSelected(YouTubeVideo video, boolean isChecked) {
        if (isChecked) {
            favoritesFragment.addToFavoritesList(video);
        } else {
            favoritesFragment.removeFromFavorites(video);
        }
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {

            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle(getString(R.string.myName));
            alertDialog.setIcon(R.mipmap.ic_launcher);

            alertDialog.setMessage(getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME + "\n\n" +
                    getString(R.string.email) + "\n\n");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();

            return true;
        } else if (id == R.id.action_clear_list) {
            YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).deleteAll();
            recentlyPlayedFragment.clearRecentlyPlayedList();
            return true;
        } else if (id == R.id.action_search) {
            MenuItemCompat.expandActionView(item);
            return true;
        }

        return super.onOptionsItemSelected(item);

    }
}