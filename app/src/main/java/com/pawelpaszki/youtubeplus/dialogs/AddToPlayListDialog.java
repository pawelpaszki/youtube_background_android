package com.pawelpaszki.youtubeplus.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import com.pawelpaszki.youtubeplus.YTApplication;
import com.pawelpaszki.youtubeplus.database.YouTubeSqlDb;
import com.pawelpaszki.youtubeplus.model.YouTubeVideo;
import com.pawelpaszki.youtubeplus.utils.SharedPrefs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by PawelPaszki on 11/07/2017.
 */

public class AddToPlayListDialog {

    public static void showPlaylistSelectionDialog(final Context context, final YouTubeVideo video) {
        if(SharedPrefs.getPlayListNames(context) != null && SharedPrefs.getPlayListNames(context).size() > 0) {
            String[] playLists = new String[SharedPrefs.getPlayListNames(context).size()];
            playLists = SharedPrefs.getPlayListNames(context).toArray(playLists);
            final String[] radioButtonOptions = playLists;
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            //alt_bld.setIcon(R.drawable.icon);
            builder.setTitle("Select playlist");
            builder.setSingleChoiceItems(radioButtonOptions, -1, new DialogInterface
                    .OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    Toast.makeText(context,
                            "playlist = "+radioButtonOptions[item], Toast.LENGTH_SHORT).show();
                    addVideoToPlaylist(context, video, radioButtonOptions[item]);
                    dialog.dismiss();

                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Please enter playlist's name");

            final EditText input = new EditText(context);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if(s.toString().length() > 15) {
                        input.setText(s.toString().substring(0,15));
                        input.setSelection(input.getText().length());
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            builder.setView(input);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            final AlertDialog dialog = builder.create();
            dialog.show();

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if(input.getText().toString().length() > 0) {
                        Set<String> playListNamesSet = new HashSet<>();
                        playListNamesSet.add(input.getText().toString());
                        SharedPrefs.savePlaylistNames(context, playListNamesSet);
                        addVideoToPlaylist(context, video, input.getText().toString());

                        dialog.dismiss();
                    }
                }
            });
        }
    }

    private static void addVideoToPlaylist(Context context, YouTubeVideo video, String playListName) {
        ArrayList<String> playlistItems = SharedPrefs.getPlaylistVideoIds(context, playListName);
        if(!playlistItems.contains(video.getId())) {
            playlistItems.add(video.getId());
            SharedPrefs.savePlaylistVideoIds(context, playlistItems, playListName);
            YouTubeSqlDb.getInstance().videos(YouTubeSqlDb.VIDEOS_TYPE.CUSTOM).create(video);
            int videoCounter = SharedPrefs.getVideoCounter(video.getId(), context) + 1;
            SharedPrefs.setVideoCounter(video.getId(), videoCounter,context);
        }

    }

}
