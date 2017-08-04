package com.pawelpaszki.youtubeplus.interfaces;

import com.pawelpaszki.youtubeplus.model.YouTubeVideo;
import com.pawelpaszki.youtubeplus.utils.Config;

/**
 * Created by smedic on 9.2.17..
 */

public interface ItemEventsListener<Model> {
    void onAddClicked(YouTubeVideo video);

    void onRemoveClicked(YouTubeVideo video);

    void onItemClick(Model model); //handle click on a row (video or playlist)

    void onDownloadClicked(YouTubeVideo video, Config.MediaType type);
}
