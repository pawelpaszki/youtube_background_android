package com.pawelpaszki.youtubeplus.utils;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import com.pawelpaszki.youtubeplus.YTApplication;
import com.pawelpaszki.youtubeplus.database.YouTubeSqlDb;
import com.pawelpaszki.youtubeplus.model.YouTubeVideo;

import java.io.File;

/**
 * Created by PawelPaszki on 10/07/2017.
 *
 * Used to initiate media download
 */

public class MediaDownloader {

    public static void downloadMedia(YouTubeVideo video, Context context) {
        String filename = video.getId();
        File[] files = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).listFiles();
        for (File file : files) {
            if (file.getAbsolutePath().contains(filename)) {
                Toast.makeText(YTApplication.getAppContext(), "Media has been downloaded already",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

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
}
