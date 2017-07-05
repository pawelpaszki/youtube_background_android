package com.pawelpaszki.youtubeplus.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pawelpaszki.youtubeplus.BackgroundAudioService;
import com.pawelpaszki.youtubeplus.MainActivity;
import com.pawelpaszki.youtubeplus.R;
import com.pawelpaszki.youtubeplus.adapters.VideosAdapter;
import com.pawelpaszki.youtubeplus.database.YouTubeSqlDb;
import com.pawelpaszki.youtubeplus.interfaces.ItemEventsListener;
import com.pawelpaszki.youtubeplus.interfaces.OnFavoritesSelected;
import com.pawelpaszki.youtubeplus.interfaces.OnItemSelected;
import com.pawelpaszki.youtubeplus.model.ItemType;
import com.pawelpaszki.youtubeplus.model.YouTubeVideo;
import com.pawelpaszki.youtubeplus.utils.Config;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by PawelPaszki on 04/07/2017.
 */

public class DownloadedFragment extends BaseFragment implements ItemEventsListener<YouTubeVideo> {

    private static final String ACTION_LOCAL_PLAYBACK_STARTED = "LocalPlaybackStarted";
    private ArrayList<YouTubeVideo> downloadedVideos;

    private RecyclerView downloadedListView;
    private VideosAdapter videoListAdapter;
    private OnItemSelected itemSelected;
    private OnFavoritesSelected onFavoritesSelected;
    private Context context;

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
        downloadedListView = (RecyclerView) v.findViewById(R.id.fragment_list_items);
        downloadedListView.setLayoutManager(new LinearLayoutManager(context));
        videoListAdapter = new VideosAdapter(context, downloadedVideos, "downloadedFragment");
        videoListAdapter.setOnItemEventsListener(this);
        downloadedListView.setAdapter(videoListAdapter);

        //disable swipe to refresh for this tab
        v.findViewById(R.id.swipe_to_refresh).setEnabled(false);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        downloadedVideos.clear();
        downloadedVideos.addAll(YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.DOWNLOADED).readAll());
        videoListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            this.context = context;
            itemSelected = (MainActivity) context;
            onFavoritesSelected = (MainActivity) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.context = null;
        itemSelected = null;
        onFavoritesSelected = null;
    }

    @Override
    public void onAdditionalClicked(YouTubeVideo video) {
        downloadedVideos.remove(video);

        String filename = video.getId();
        File[] files = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).listFiles();
        for(int i = 0; i < files.length; i++) {
            if(files[i].getAbsolutePath().toString().contains(filename)) {
                String fileToRemove = files[i].getAbsolutePath();
                File file = new File(fileToRemove);
                boolean ignored = file.delete();
                YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.DOWNLOADED).delete(video.getId());
                videoListAdapter.notifyDataSetChanged();
                break;
            }
        }
    }

    @Override
    public void onFavoriteClicked(YouTubeVideo video, boolean isChecked) {
        onFavoritesSelected.onFavoritesSelected(video, isChecked); // pass event to MainActivity
    }

    @Override
    public void onItemClick(YouTubeVideo video) {
        Log.i("item clicked", "downloaded");
        Intent serviceIntent = new Intent(getActivity(), BackgroundAudioService.class);
        serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);
        serviceIntent.putExtra(Config.YOUTUBE_TYPE, ItemType.MEDIA_LOCAL);
        serviceIntent.putExtra(Config.YOUTUBE_TYPE_VIDEO, video);
        serviceIntent.putExtra(Config.LOCAL_MEDIA_FILEAME, video.getId());
        serviceIntent.putExtra(Config.YOUTUBE_TYPE_PLAYLIST, (ArrayList) downloadedVideos);
        getActivity().startService(serviceIntent);
    }
}
