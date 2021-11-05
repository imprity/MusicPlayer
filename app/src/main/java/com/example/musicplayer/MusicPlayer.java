package com.example.musicplayer;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.IdentityHashMap;

public class MusicPlayer {
    private static final String TAG = "MusicPlayer";
    private MediaPlayer player = null;
    private boolean player_prepared = false;
    private File currentSong = null;

    public final static String[] supportedFormats = {
            ".mp3",
            ".wav"
    };

    public MusicPlayer(){
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

    public void stopMusic(){
        if(player_prepared && player != null){
            player.stop();
            player_prepared = false;
        }
    }

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

    public boolean isPlayerPrepared(){
        return player_prepared;
    }

    public void play(){
        if(currentSong != null){
            if(player_prepared && player != null){
                player.start();
            }
            else{
                playMusic(currentSong);
            }
        }
    }

    public File getCurrentSong(){
        return currentSong;
    }

    public long getCurrentPosition(){
        if(player_prepared && player != null){
            return player.getCurrentPosition();
        }
        return -1;
    }

    public void seekTo(long millis){
        if(player_prepared && player != null){
            try{
                player.seekTo(millis, MediaPlayer.SEEK_PREVIOUS_SYNC);
            }
            catch (Exception e) {
                Log.d("failed to seek : ", e.toString());
            }
        }
    }

    public long getDuration(){
        if(player_prepared && player != null){
            return player.getDuration();
        }
        else{
            return -1;
        }
    }

    public void playMusic(File song){
        player.reset();
        player_prepared = false;

        try {
            player.setDataSource(song.getAbsolutePath());
            player.prepare();
            player_prepared = true;
            currentSong = song;
        }
        catch (Exception e){
            Log.d("failed to play file : ",e.toString());
            player.reset();
            player_prepared = false;
            currentSong = null;
            return;
        }
        player.start();
    }

    public void onDestroy() {
        if(player != null){
            player.release();
        }
        player = null;
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
