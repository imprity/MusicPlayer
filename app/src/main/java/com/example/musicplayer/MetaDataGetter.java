package com.example.musicplayer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;

public class MetaDataGetter {
    private static final String TAG = "MetaDataGetter";

    private MetaDataGetter (){}

    private static MetaDataGetter instance = new MetaDataGetter();

    public static MetaDataGetter getInstance(){
        return instance;
    }

    private class MetadataThread extends Thread{
        private MetaDataRequester requester;

        private MetadataThread(MetaDataRequester requester){
            this.requester = requester;
        }

        @Override
        public void run(){
            //Meta datas to get
            Bitmap thumbnail = null;


            if(requester.file != null && MusicPlayer.supportsFormat(requester.file)){
                File file = requester.file;

                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                byte []  rawThumbnail = null;
                try {
                    mediaMetadataRetriever.setDataSource(file.getAbsolutePath());

                    rawThumbnail = mediaMetadataRetriever.getEmbeddedPicture();
                }
                catch (Exception e){
                    Log.e(TAG, "Error generating bitmap for file", e);
                }
                if(rawThumbnail != null){
                    thumbnail = BitmapFactory.decodeByteArray(rawThumbnail, 0, rawThumbnail.length, new BitmapFactory.Options());
                }
            }

            requester.onGotMetaData(thumbnail);
        }
    }

    public static class MetaDataRequester{
        private File file;
        public MetaDataRequester(File file){
            this.file = file;
        }

        public void onGotMetaData(Bitmap thumbnail){};
    }

    public void requestMetaData(MetaDataRequester requester){
        MetadataThread thread = new MetadataThread(requester);
        thread.start();
    }
}
