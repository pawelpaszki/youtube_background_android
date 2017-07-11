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
import com.pawelpaszki.youtubeplus.model.YouTubeVideo;
import com.pawelpaszki.youtubeplus.utils.SharedPrefs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by PawelPaszki on 11/07/2017.
 */

public class AddToPlayListDialog {

    public static void showPlaylistSelectionDialog(final Context context, YouTubeVideo video) {
        if(SharedPrefs.getPlayListNames(context).size() > 0) {
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
                            "Group Name = "+radioButtonOptions[item], Toast.LENGTH_SHORT).show();
                    dialog.dismiss();

                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Please enter playlist's name");

            final EditText input = new EditText(context);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

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
            //Overriding the handler immediately after show is probably a better approach than OnShowListener as described below
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if(input.getText().toString().length() > 0) {
                        Set<String> playListNamesSet = new HashSet<>();
                        playListNamesSet.add(input.getText().toString());
                        SharedPrefs.savePlaylistNames(context, playListNamesSet);
                        for(String item: SharedPrefs.getPlayListNames(context)) {
                            Log.i("item", item);
                        }
                        SharedPrefs.savePlaylistNames(context, playListNamesSet);
                        Toast.makeText(YTApplication.getAppContext(), "playlist created",
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }
            });
        }
    }

}
