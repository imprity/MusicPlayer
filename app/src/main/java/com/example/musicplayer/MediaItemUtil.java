package com.example.musicplayer;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;

import androidx.annotation.NonNull;

import java.io.File;

public class MediaItemUtil {
    static MediaBrowserCompat.MediaItem createMediaItemFromFile(@NonNull File file){
        MediaDescriptionCompat desc =
                new MediaDescriptionCompat.Builder()
                        .setMediaId(file.getAbsolutePath())
                        .setTitle(file.getName())
                        .build();

        int flag = 0;

        if(file.isDirectory()){
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE;
        }
        else if(MusicPlayer.supportsFormat(file)){
            flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE;
        }

        return new MediaBrowserCompat.MediaItem(desc, flag);
    }

    static File getFileFromMediaItem(MediaBrowserCompat.MediaItem item){
        String path = item.getMediaId();
        File file = new File(path);
        return file;
    }
}
