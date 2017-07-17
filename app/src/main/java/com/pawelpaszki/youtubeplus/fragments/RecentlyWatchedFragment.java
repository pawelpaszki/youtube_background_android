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
package com.pawelpaszki.youtubeplus.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pawelpaszki.youtubeplus.MainActivity;
import com.pawelpaszki.youtubeplus.R;
import com.pawelpaszki.youtubeplus.YTApplication;
import com.pawelpaszki.youtubeplus.adapters.VideosAdapter;
import com.pawelpaszki.youtubeplus.database.YouTubeSqlDb;
import com.pawelpaszki.youtubeplus.interfaces.ItemEventsListener;
import com.pawelpaszki.youtubeplus.interfaces.OnItemSelected;
import com.pawelpaszki.youtubeplus.model.YouTubeVideo;
import com.pawelpaszki.youtubeplus.utils.Config;
import com.pawelpaszki.youtubeplus.utils.MediaDownloader;
import com.pawelpaszki.youtubeplus.utils.MediaStorageHandler;

import java.io.File;
import java.util.ArrayList;

import static com.pawelpaszki.youtubeplus.dialogs.AddToPlayListDialog.showPlaylistSelectionDialog;

/**
 * Class that handles list of the recently watched YouTube
 * Created by smedic on 7.3.16..
 */
public class RecentlyWatchedFragment extends BaseFragment implements
        ItemEventsListener<YouTubeVideo> {

    private ArrayList<YouTubeVideo> recentlyPlayedVideos;

    private RecyclerView recentlyPlayedListView;
    private VideosAdapter videoListAdapter;
    private OnItemSelected itemSelected;
    private Context context;

    public RecentlyWatchedFragment() {
        // Required empty public constructor
    }

    public static RecentlyWatchedFragment newInstance() {
        return new RecentlyWatchedFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recentlyPlayedVideos = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_list, container, false);
        LinearLayout spinner = (LinearLayout) v.findViewById(R.id.playlist_management);
        spinner.setVisibility(View.GONE);
        TypedValue tv = new TypedValue();
        int height;
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            height = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        } else {
            float density = getResources().getDisplayMetrics().density;
            height = (int) (50 * density);
        }
        LinearLayout videosContainer = (LinearLayout) v.findViewById(R.id.videos_container);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) videosContainer.getLayoutParams();
        float density = context.getResources().getDisplayMetrics().density;
        params.topMargin = height + (int) (6 * density);
        videosContainer.setLayoutParams(params);
        recentlyPlayedListView = (RecyclerView) v.findViewById(R.id.fragment_list_items);
        recentlyPlayedListView.setLayoutManager(new LinearLayoutManager(context));
        videoListAdapter = new VideosAdapter(context, recentlyPlayedVideos, "recentlyWatched");
        videoListAdapter.setOnItemEventsListener(this);
        recentlyPlayedListView.setAdapter(videoListAdapter);

        Button clearRecentButton = (Button) v.findViewById(R.id.clear_recent);
        clearRecentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(recentlyPlayedVideos.size() > 0) {
                    recentlyPlayedVideos.clear();
                    YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).deleteAll();
                    videoListAdapter.notifyDataSetChanged();
                }
            }
        });

        //disable swipe to refresh for this tab
        v.findViewById(R.id.swipe_to_refresh).setEnabled(false);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        recentlyPlayedVideos.clear();
        recentlyPlayedVideos.addAll(YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).readAll(null, context));
        videoListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof MainActivity) {
            itemSelected = (MainActivity) context;
            this.context = context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.itemSelected = null;
        this.context = null;
    }

    public void setTitle(String title) {
        String aTitle = title + " " + getString(R.string.recently_watched_tab);
        ((MainActivity)getActivity()).getmTitleTextView().setText(aTitle);
        ((MainActivity)getActivity()).getmTitleTextView().setSelected(true);
    }

    @Override
    public void onRemoveClicked(YouTubeVideo video) {
        recentlyPlayedVideos.remove(video);
        YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).delete(video.getId(), YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED.toString(), context);
        videoListAdapter.notifyDataSetChanged();
        Log.i("clicked", "true");
    }

    @Override
    public void onAddClicked(YouTubeVideo video) {
        Log.i("add clicked","recent fragment");
        showPlaylistSelectionDialog(context, video);
    }

    @Override
    public void onItemClick(YouTubeVideo video) {
        itemSelected.onPlaylistSelected(recentlyPlayedVideos, recentlyPlayedVideos.indexOf(video));
    }

    @Override
    public void onDownloadClicked(YouTubeVideo video) {
        MediaDownloader.downloadMedia(video, context);
    }
}
