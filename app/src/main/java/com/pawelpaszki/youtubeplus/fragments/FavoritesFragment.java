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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.pawelpaszki.youtubeplus.utils.MediaStorageHandler;
import com.pawelpaszki.youtubeplus.utils.NetworkConf;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Stevan Medic on 21.3.16..
 */
public class FavoritesFragment extends BaseFragment implements ItemEventsListener<YouTubeVideo> {

    private List<YouTubeVideo> favoriteVideos;

    private RecyclerView favoritesListView;
    private VideosAdapter videoListAdapter;
    private OnItemSelected itemSelected;
    private Context context;

    public FavoritesFragment() {
        // Required empty public constructor
    }

    public static FavoritesFragment newInstance() {
        return new FavoritesFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        favoriteVideos = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_list, container, false);
        favoritesListView = (RecyclerView) v.findViewById(R.id.fragment_list_items);
        favoritesListView.setLayoutManager(new LinearLayoutManager(context));

        videoListAdapter = new VideosAdapter(context, favoriteVideos,"favouritesFragment");
        videoListAdapter.setOnItemEventsListener(this);
        favoritesListView.setAdapter(videoListAdapter);

        //disable swipe to refresh for this tab
        v.findViewById(R.id.swipe_to_refresh).setEnabled(false);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        favoriteVideos.clear();
        favoriteVideos.addAll(YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE).readAll());
        videoListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            this.itemSelected = (MainActivity) context;
            this.context = context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.itemSelected = null;
        this.context = null;
    }

    /**
     * Clears recently played list items
     */
    public void clearFavoritesList() {
        favoriteVideos.clear();
        videoListAdapter.notifyDataSetChanged();
    }

    public void addToFavoritesList(YouTubeVideo video) {
        YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE).create(video);
    }

    public void removeFromFavorites(YouTubeVideo video) {
        YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.FAVORITE).delete(video.getId());
        favoriteVideos.remove(video);
        videoListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAdditionalClicked(YouTubeVideo video) {
        boolean notExists = YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.DOWNLOADED).create(video);
        //TODO check if exists already on sd card
        if(notExists) {
            try {
                MediaStorageHandler.downloadVideo(video, context);
            } catch (Exception e) {
                Toast.makeText(YTApplication.getAppContext(), "Video has not been downloaded",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onFavoriteClicked(YouTubeVideo video, boolean isChecked) {
        if (isChecked) {
            addToFavoritesList(video);
        } else {
            removeFromFavorites(video);
        }
    }

    @Override
    public void onItemClick(YouTubeVideo video) {
        YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.RECENTLY_WATCHED).create(video);
        itemSelected.onPlaylistSelected(favoriteVideos, favoriteVideos.indexOf(video));
    }
}
