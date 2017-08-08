# youtube_background_android

This application is a modification of following:

https://github.com/smedic/Android-YouTube-Background-Player

Youtube API Key is required to be pasted into utils/Config.java file in order for this application to work. The key can be obtained here:

https://console.developers.google.com/

In order to use this code, you are required to clone this repo and open it in Android Studio.

The main goal of this application is to be able to listen to youtube media in the background and watch videos in foreground, once downloaded

## Please note that Google Play prevents from playing YouTube videos in the background, therefore applications like this are not permitted to be pushed to Play Store

Swiping from left edge of the screen shows/hides buttons, which open one of four sections of the application:
_____________________________________________________________________________________________________________

* Downloaded videos - displays list of downloaded media, which can be individually all collectively removed, list items can also be rearranged (when player is stopped)
* Playlists - displays view with dropdown of all available playlists (if any). Playlists can be added, removed, individually displayed and their items can be rearranged
* Recently opened media
* Search view

Media can be played from either of these sections. When played from "Downloaded" section, video will also appear (and re-appear when returning into the app)

Long pressing the name of the media will show additional options:
_________________________________________________________________

* add media to playlist (if there is no playlist, new playlist will be created, otherwise list of playlists will be displayed)
* remove media from the list (except from search view)
* download media (except from download view)

<img src="https://github.com/pawelpaszki/youtube_background_android/blob/master/raw/animation.gif" alt="YouTube+" width="360" height="640">

####TODO

* improve media sync played from Downloaded tab
* (optionally) provide audio equalizer
* (optionally) provide video view for streamed media

Please see License tab for more details