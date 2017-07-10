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
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.pawelpaszki.youtubeplus.MainActivity;
import com.pawelpaszki.youtubeplus.R;
import com.pawelpaszki.youtubeplus.adapters.NoThumbnailAdapter;
import com.pawelpaszki.youtubeplus.adapters.VideosAdapter;
import com.pawelpaszki.youtubeplus.database.YouTubeSqlDb;
import com.pawelpaszki.youtubeplus.interfaces.ItemEventsListener;
import com.pawelpaszki.youtubeplus.interfaces.OnItemSelected;
import com.pawelpaszki.youtubeplus.model.YouTubeVideo;
import com.pawelpaszki.youtubeplus.utils.MediaDownloader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Stevan Medic on 21.3.16..
 */
public class PlayListsFragment extends BaseFragment implements ItemEventsListener<YouTubeVideo> {

    private List<YouTubeVideo> customVideos;

    private RecyclerView playListsView;
    private NoThumbnailAdapter noThumbnailAdapter;
    private Context context;

    public PlayListsFragment() {
        // Required empty public constructor
    }

    public static PlayListsFragment newInstance() {
        return new PlayListsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        customVideos = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_list, container, false);
        playListsView = (RecyclerView) v.findViewById(R.id.fragment_list_items);
        playListsView.setLayoutManager(new LinearLayoutManager(context));
        LinearLayout videosContainer = (LinearLayout) v.findViewById(R.id.videos_container);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) videosContainer.getLayoutParams();
        float density = context.getResources().getDisplayMetrics().density;
        params.topMargin = (int) (30 * density);
        videosContainer.setLayoutParams(params);

        noThumbnailAdapter = new NoThumbnailAdapter(context, customVideos,"playListsFragment");
        noThumbnailAdapter.setOnItemEventsListener(this);
        playListsView.setAdapter(noThumbnailAdapter);

        //disable swipe to refresh for this tab
        v.findViewById(R.id.swipe_to_refresh).setEnabled(false);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        customVideos.clear();
        customVideos.addAll(YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.CUSTOM).readAll());
        noThumbnailAdapter.notifyDataSetChanged();
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
        this.context = null;
    }

    @Override
    public void onAddClicked(YouTubeVideo video) {
        Log.i("add clicked","playlists");
        // TODO
    }

    @Override
    public void onRemoveClicked(YouTubeVideo video) {
        Log.i("remove clicked","playlists");
        //todo
    }

    @Override
    public void onItemClick(YouTubeVideo video) {
        //TODO playlists
//        YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).create(video);
//        itemSelected.onPlaylistSelected(favoriteVideos, favoriteVideos.indexOf(video));
    }

    @Override
    public void onDownloadClicked(YouTubeVideo video) {
        MediaDownloader.downloadMedia(video, context);
    }
}
