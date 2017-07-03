package com.pawelpaszki.youtubeplus.interfaces;

import com.pawelpaszki.youtubeplus.model.YouTubeVideo;

/**
 * Created by smedic on 5.3.17..
 */

public interface OnFavoritesSelected {
    void onFavoritesSelected(YouTubeVideo video, boolean isChecked);
}
