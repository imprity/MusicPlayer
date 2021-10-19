package com.example.musicplayer;


import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.security.Provider;
import java.security.spec.ECField;
import java.util.ArrayList;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

public class MusicPlayerService extends Service {
    private MediaPlayer player = null;
    private boolean player_prepared = false;

    private File currentTrack;

    public final static String[] supportedFormats = {
            ".mp3",
            ".wav"
    };

    private final MusicPlayerBinder binder = new MusicPlayerBinder();

    public void pauseMusic(){
        if(player_prepared && player != null && player.isPlaying()){
            player.pause();
        }
    }

    public boolean isPlaying(){
        if(player_prepared && player != null){
            return player.isPlaying();
        }
        return false;
    }

    public void resumeMusic(){
        if(player_prepared && player != null && !player.isPlaying()){
            player.start();
        }
    }

    public long getCurrentPosition(){
        if(player_prepared && player != null){
            return player.getCurrentPosition();
        }
        return -1;
    }

    public void seekTo(long millis){
        if(player_prepared){
            try{
                player.seekTo(millis, MediaPlayer.SEEK_PREVIOUS_SYNC);
            }
            catch (Exception e) {
                Log.d("failed to seek : ", e.toString());
            }
        }
    }

    public long getDuration(){
        if(player_prepared){
            return player.getDuration();
        }
        else{
            return -1;
        }
    }

    public Exception playMusic(File song){

        if(currentTrack != null && song.getAbsolutePath() == currentTrack.getAbsolutePath()){
            return null;
        }
        player.reset();
        player_prepared = false;
        Log.d("received path : ",song.getName());

        try {
            player.setDataSource(song.getAbsolutePath());
            player.prepare();
            player_prepared = true;
        }
        catch (Exception e){
            Log.d("failed to play file : ",e.toString());
            player.reset();
            player_prepared = false;
            return e;
        }
        currentTrack = new File(song.getAbsolutePath());
        player.start();
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        player = new MediaPlayer();
        player.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );

        player.setOnErrorListener(new MediaPlayer.OnErrorListener(){
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e("MediaPlayer failed, resetting", "shit");
                player_prepared = false;
                mp.reset();
                return true;
            }
        });
    }

    public File getCurrentTrack(){
        if(currentTrack != null){
            return new File(currentTrack.getAbsolutePath());
        }
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(player != null){
            player.release();
        }
        player = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class MusicPlayerBinder extends Binder{
        MusicPlayerService getService(){
            return MusicPlayerService.this;
        }
    }

    public static boolean supportsFormat(File file){
        for(String format : supportedFormats){
            if(file.getName().endsWith(format)){
                return true;
            }
        }
        return false;
    }
}
