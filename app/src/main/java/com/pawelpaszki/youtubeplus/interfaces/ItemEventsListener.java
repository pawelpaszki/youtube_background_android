package com.pawelpaszki.youtubeplus.interfaces;

import com.pawelpaszki.youtubeplus.model.YouTubeVideo;

/**
 * Created by smedic on 9.2.17..
 */

public interface ItemEventsListener<Model> {
    void onAdditionalClicked(YouTubeVideo video);

    void onFavoriteClicked(YouTubeVideo video, boolean isChecked);

    void onItemClick(Model model); //handle click on a row (video or playlist)
}
