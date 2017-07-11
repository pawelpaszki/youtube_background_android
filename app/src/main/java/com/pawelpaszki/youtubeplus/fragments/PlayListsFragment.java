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
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.pawelpaszki.youtubeplus.MainActivity;
import com.pawelpaszki.youtubeplus.R;
import com.pawelpaszki.youtubeplus.YTApplication;
import com.pawelpaszki.youtubeplus.adapters.NoThumbnailAdapter;
import com.pawelpaszki.youtubeplus.database.YouTubeSqlDb;
import com.pawelpaszki.youtubeplus.interfaces.ItemEventsListener;
import com.pawelpaszki.youtubeplus.model.YouTubeVideo;
import com.pawelpaszki.youtubeplus.utils.MediaDownloader;
import com.pawelpaszki.youtubeplus.utils.SharedPrefs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.pawelpaszki.youtubeplus.dialogs.AddToPlayListDialog.showPlaylistSelectionDialog;

/**
 * Created by Stevan Medic on 21.3.16..
 */
public class PlayListsFragment extends BaseFragment implements ItemEventsListener<YouTubeVideo> {

    private List<YouTubeVideo> customVideos;

    private RecyclerView playListsView;
    private NoThumbnailAdapter noThumbnailAdapter;
    private Context context;
    private AlertDialog.Builder mAlertBuilder;
    private Spinner mSpinner;
    private ArrayAdapter<String> mSpinnerArrayAdapter;

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

        final LinearLayout playListManagement = (LinearLayout) v.findViewById(R.id.playlist_management);

        noThumbnailAdapter = new NoThumbnailAdapter(context, customVideos,"playListsFragment");
        noThumbnailAdapter.setOnItemEventsListener(this);
        playListsView.setAdapter(noThumbnailAdapter);

        ImageView deletePlayList = (ImageView) playListManagement.findViewById(R.id.remove_playlist_button);
        ImageView addPlayList = (ImageView) playListManagement.findViewById(R.id.add_playlist_button);



        mSpinner = (Spinner) v.findViewById(R.id.playlist_spinner);
        if(SharedPrefs.getPlayListNames(context) != null) {
            String[] data = new String[SharedPrefs.getPlayListNames(context).size()];
            data = SharedPrefs.getPlayListNames(context).toArray(data);
            mSpinnerArrayAdapter = new ArrayAdapter<>(
                    context, R.layout.spinner_item, data);
            mSpinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            mSpinner.setAdapter(mSpinnerArrayAdapter);
        }
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.i("position selected", String.valueOf(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        deletePlayList.setOnClickListener(new AdapterView.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> playListNames = new ArrayList<String> (SharedPrefs.getPlayListNames(context));
                if(playListNames.size() > 0) {
                    playListNames.remove(mSpinner.getSelectedItem().toString());
                    Set<String> playListNamesSet = new HashSet<>();
                    for(String item : playListNames) {
                        playListNamesSet.add(item);
                    }
                    SharedPrefs.savePlaylistNames(context, playListNamesSet);
                    String[] data = new String[SharedPrefs.getPlayListNames(context).size()];
                    data = SharedPrefs.getPlayListNames(context).toArray(data);
                    mSpinnerArrayAdapter = new ArrayAdapter<>(
                            context, R.layout.spinner_item, data);
                    mSpinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                    mSpinner.setAdapter(mSpinnerArrayAdapter);
                    loadPlaylist();
                    ArrayList<String> videos = new ArrayList<>();
                    for(YouTubeVideo video: customVideos) {
                        videos.add(video.getId());
                    }
                    YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.CUSTOM).deleteCustomPlayList(videos, context);

                    //TODO remove all videos from the database
                }
            }
        });
        addPlayList.setOnClickListener(new AdapterView.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAlertBuilder = new AlertDialog.Builder(context);
                mAlertBuilder.setTitle("Please enter playlist's name");
                final ArrayList<String> playListNames = SharedPrefs.getPlayListNames(context);


                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                input.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        if(playListNames != null) {
                            if(playListNames.contains(s.toString())) {
                                mAlertBuilder.setTitle("Playlist name must be unique");
                            }
                        } else {
                            mAlertBuilder.setTitle("Please enter playlist's name");
                        }
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });
                mAlertBuilder.setView(input);

                mAlertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if((playListNames != null && !playListNames.contains(input.getText().toString())) || playListNames == null) {
                            playListNames.add(input.getText().toString());
                            Set<String> playListNamesSet = new HashSet<>();
                            for(String item : playListNames) {
                                playListNamesSet.add(item);
                            }
                            SharedPrefs.savePlaylistNames(context, playListNamesSet);
                            for(String item: SharedPrefs.getPlayListNames(context)) {
                                Log.i("item", item);
                            }
                            SharedPrefs.savePlaylistNames(context, playListNamesSet);
                            Toast.makeText(YTApplication.getAppContext(), "playlist created",
                                    Toast.LENGTH_SHORT).show();
                            String[] data = new String[SharedPrefs.getPlayListNames(context).size()];
                            data = SharedPrefs.getPlayListNames(context).toArray(data);
                            mSpinnerArrayAdapter = new ArrayAdapter<>(
                                    context, R.layout.spinner_item, data);
                            mSpinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                            mSpinner.setAdapter(mSpinnerArrayAdapter);
                            loadPlaylist();
                        } else {
                            Toast.makeText(YTApplication.getAppContext(), "playlist with given name exists already",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                mAlertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                mAlertBuilder.show();
            }
        });

        //disable swipe to refresh for this tab
        v.findViewById(R.id.swipe_to_refresh).setEnabled(false);
        setClickable(deletePlayList);
        setClickable(addPlayList);
        return v;
    }

    private void setClickable(ImageView view) {
        view.bringToFront();
        view.setClickable(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("on resume", "playlists");
        customVideos.clear();
        if(SharedPrefs.getPlayListNames(context) != null && SharedPrefs.getPlayListNames(context).size() > 0) {
            String[] data = new String[SharedPrefs.getPlayListNames(context).size()];
            data = SharedPrefs.getPlayListNames(context).toArray(data);
            mSpinnerArrayAdapter = new ArrayAdapter<>(
                    context, R.layout.spinner_item, data);
            mSpinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            mSpinner.setAdapter(mSpinnerArrayAdapter);
            customVideos.addAll(YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.CUSTOM).readAll(data[0], context));
        }
        noThumbnailAdapter.notifyDataSetChanged();
    }

    private void loadPlaylist() {
        customVideos.clear();
        if(mSpinner.getSelectedItem() != null) {
            if(SharedPrefs.getPlayListNames(context) != null && SharedPrefs.getPlayListNames(context).size() > 0) {
                String[] data = new String[SharedPrefs.getPlayListNames(context).size()];
                data = SharedPrefs.getPlayListNames(context).toArray(data);
                customVideos.addAll(YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.CUSTOM).readAll(mSpinner.getSelectedItem().toString(), context));
            }
            noThumbnailAdapter.notifyDataSetChanged();
        }

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
        showPlaylistSelectionDialog(context, video);
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
