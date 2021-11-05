package com.example.musicplayer.id_to_file_manager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import com.example.musicplayer.MetaDataGetter;
import com.example.musicplayer.MusicPlayer;

import java.io.File;

public class MediaItemUtil {
    private static final String TAG = "FileToMediaItem";
    public static MediaBrowserCompat.MediaItem FromFile(File file){
        Bitmap thumbnail = null;

        if(MusicPlayer.supportsFormat(file)) {

            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            byte[] rawThumbnail = null;
            try {
                mediaMetadataRetriever.setDataSource(file.getAbsolutePath());

                rawThumbnail = mediaMetadataRetriever.getEmbeddedPicture();
            } catch (Exception e) {
                Log.e(TAG, "Error generating bitmap for file", e);
            }
            if (rawThumbnail != null) {
                thumbnail = BitmapFactory.decodeByteArray(rawThumbnail, 0, rawThumbnail.length, new BitmapFactory.Options());
            }
        }

        MediaDescriptionCompat desc = new MediaDescriptionCompat.Builder()
                .setMediaId(file.getAbsolutePath())
                .setMediaUri(Uri.fromFile(file))
                .setTitle(file.getName())
                .setIconBitmap(thumbnail)
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

    private static String CreateIdFromFile(File file){
        return file.getAbsolutePath();
    }
    /*
    List of meta datas I can put
    Maybe include more as app gets more sophisticated
    Constants

    String	METADATA_KEY_ALBUM
    The album title for the media.

    String	METADATA_KEY_ALBUM_ART !!!BITMAP USE EXISTING BITMAP!!!
    The artwork for the album of the media's original source as a Bitmap.

    String	METADATA_KEY_ALBUM_ARTIST
    The artist for the album of the media's original source.

    String	METADATA_KEY_ART !!!BITMAP!!!
    The artwork for the media as a Bitmap.

    String	METADATA_KEY_ARTIST
    The artist of the media.

    String	METADATA_KEY_AUTHOR
    The author of the media.

    String	METADATA_KEY_COMPOSER
    The composer of the media.

    String	METADATA_KEY_DATE
    The date the media was created or published.

    String	METADATA_KEY_DISC_NUMBER
    The disc number for the media's original source.

    String	METADATA_KEY_DISPLAY_ICON !!!BITMAP USE EXSITING THUMBNAIL!!!
    An icon or thumbnail that is suitable for display to the user.

    String	METADATA_KEY_DISPLAY_TITLE
    A title that is suitable for display to the user.

    String	METADATA_KEY_DURATION
    The duration of the media in ms.

    String	METADATA_KEY_GENRE
    The genre of the media.

    String	METADATA_KEY_MEDIA_ID
    A String key for identifying the content.

    String	METADATA_KEY_MEDIA_URI
    A Uri formatted String representing the content.

    String	METADATA_KEY_NUM_TRACKS
    The number of tracks in the media's original source.

    String	METADATA_KEY_RATING
    The overall rating for the media.

    String	METADATA_KEY_TITLE
    The title of the media.

    String	METADATA_KEY_TRACK_NUMBER
    The track number for the media.

    String	METADATA_KEY_USER_RATING
    The user's rating for the media.

    String	METADATA_KEY_WRITER
    The writer of the media.

    String	METADATA_KEY_YEAR
    The year the media was created or published as a long.
     */

    public static MediaMetadataCompat GetMetaData(MediaBrowserCompat.MediaItem item){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(item.getDescription().getMediaUri().getPath());
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM))
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, item.getDescription().getIconBitmap())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST))
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, item.getDescription().getIconBitmap())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST))
                .putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR))
                .putString(MediaMetadataCompat.METADATA_KEY_COMPOSER, retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER))
                .putString(MediaMetadataCompat.METADATA_KEY_DATE, retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE))
                //.putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)))
                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, item.getDescription().getIconBitmap())
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, item.getDescription().getTitle().toString())
                //.putString(MediaMetadataCompat.METADATA_KEY_DURATION, retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE))
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, item.getMediaId())
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, item.getDescription().getMediaUri().toString())
                //.putString(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS))
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, item.getDescription().getTitle().toString())
                //.putString(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER))
                .putString(MediaMetadataCompat.METADATA_KEY_WRITER, retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER));
                //.putString(MediaMetadataCompat.METADATA_KEY_YEAR, retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR))

        String discNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER);
        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        String numTracks = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS);
        String trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
        String year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);

        if(discNumber != null){
            builder.putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, Long.parseLong(discNumber));
        }
        if(duration != null){
            builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, Long.parseLong(duration));
        }
        if(numTracks != null){
            builder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, Long.parseLong(numTracks));
        }
        if(trackNumber != null){
            builder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, Long.parseLong(trackNumber));
        }
        if(year != null){
            builder.putLong(MediaMetadataCompat.METADATA_KEY_YEAR, Long.parseLong(year));
        }

        MediaMetadataCompat metadata = builder.build();

        return metadata;
    }
}
