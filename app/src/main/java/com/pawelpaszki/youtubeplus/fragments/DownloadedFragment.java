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
import android.widget.LinearLayout;
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

/**
 * Created by PawelPaszki on 04/07/2017.
 */

public class DownloadedFragment extends BaseFragment implements ItemEventsListener<YouTubeVideo> {

    private ArrayList<YouTubeVideo> downloadedVideos;

    private RecyclerView downloadedListView;
    private NoThumbnailAdapter videoListAdapter;
    private OnItemSelected itemSelected;
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
        LinearLayout spinner = (LinearLayout) v.findViewById(R.id.playlist_management);
        spinner.setVisibility(View.GONE);
        downloadedListView = (RecyclerView) v.findViewById(R.id.fragment_list_items);
        downloadedListView.setLayoutManager(new LinearLayoutManager(context));
        videoListAdapter = new NoThumbnailAdapter(context, downloadedVideos, "downloadedFragment");
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
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
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
        YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.DOWNLOADED).delete(video.getId());
        videoListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAddClicked(YouTubeVideo video) {
        Log.i("add clicked","true");
        //TODO
    }

    @Override
    public void onItemClick(YouTubeVideo video) {
        Log.i("item clicked", "downloaded");
        if(fileExists(video)) {
            Intent serviceIntent = new Intent(getActivity(), BackgroundAudioService.class);
            serviceIntent.setAction(BackgroundAudioService.ACTION_PLAY);
            serviceIntent.putExtra(Config.YOUTUBE_TYPE, ItemType.MEDIA_LOCAL);
            serviceIntent.putExtra(Config.YOUTUBE_TYPE_VIDEO, video);
            serviceIntent.putExtra(Config.LOCAL_MEDIA_FILEAME, video.getId());
            serviceIntent.putExtra(Config.YOUTUBE_TYPE_PLAYLIST, (ArrayList) downloadedVideos);
            getActivity().startService(serviceIntent);
        } else if (SharedPrefs.getDownloadInProgress(context, video.getId())) {
            Toast.makeText(YTApplication.getAppContext(), "Media is still being downloaded",
                    Toast.LENGTH_SHORT).show();
        } else {
            downloadedVideos.remove(video);
            YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.DOWNLOADED).delete(video.getId());
            videoListAdapter.notifyDataSetChanged();
            Toast.makeText(YTApplication.getAppContext(), "Unable to find media",
                    Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onDownloadClicked(YouTubeVideo video) {
        //do nothing
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
}

