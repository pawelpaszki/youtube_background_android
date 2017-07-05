package com.pawelpaszki.youtubeplus.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Toast;

import com.pawelpaszki.youtubeplus.YTApplication;
import com.pawelpaszki.youtubeplus.model.YouTubeVideo;

import java.util.ArrayList;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

/**
 * Created by PawelPaszki on 04/07/2017.
 */

public class MediaStorageHandler {

    public static void downloadVideo(final YouTubeVideo video, final Context context) {
        String youtubeLink = Config.YOUTUBE_BASE_URL + video.getId();
        new YouTubeExtractor(context) {

            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {

                if (ytFiles == null) {
                    return;
                }
                ArrayList <Integer> iTags = new ArrayList<>();

                for (int i = 0, itag; i < ytFiles.size(); i++) {
                    itag = ytFiles.keyAt(i);
                        iTags.add(itag);
                }
                YtFile ytFile;
                int index = 0;
                /*
                itag value      Video resolution
                160	            144p
                133	            240p
                134	            360p
                135	            480p
                136	            720p
                137	            1080p
                */
                if(iTags.contains(22)) {
                    ytFile = ytFiles.get(22);
                    index = 22;
                } else if(iTags.contains(18)) {
                    ytFile = ytFiles.get(18);
                    index = 18;
                } else if(iTags.contains(36)) {
                    ytFile = ytFiles.get(36);
                    index = 36;
                } else if(iTags.contains(17)) {
                    ytFile = ytFiles.get(17);
                    index = 17;
                } else {
                    ytFile = ytFiles.get(ytFiles.size() - 1);
                    index = ytFiles.keyAt(ytFiles.size() - 1);
                }

                Log.i("video tag", String.valueOf(index));
                String filename = video.getId() + "." + ytFile.getFormat().getExt();
                downloadFromUrl(ytFile.getUrl(), video.getTitle(), filename, context);
            }
        }.extract(youtubeLink, true, false);
    }

    private static void downloadFromUrl(String youtubeDlUrl, String downloadTitle, String fileName, Context context) {
        Uri uri = Uri.parse(youtubeDlUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(downloadTitle);

        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
    }
}
