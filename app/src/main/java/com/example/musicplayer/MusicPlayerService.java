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
import java.util.Arrays;
import java.util.IdentityHashMap;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

public class MusicPlayerService extends Service {
    private static final String TAG = "MusicPlayerService";
    private MediaPlayer player = null;
    private boolean player_prepared = false;

    private int currentTrackIndex = 0;
    private File[] currentTracks = null;

    private IdentityHashMap<Object, StateChangeListener> listenerMap = new IdentityHashMap<Object, StateChangeListener>();

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
        if(player_prepared && player != null){
            return player.getDuration();
        }
        else{
            return -1;
        }
    }

    public int getCurrentTrackIndex(){
        return currentTrackIndex;
    }

    public void playMusic(File[] newTracks, int newIndex){
        if(newTracks == null || newTracks.length <=0 || newIndex < 0 || newIndex >= newTracks.length){
            return;
        }

        //check if prev song is the same song
        if(Arrays.deepEquals(newTracks, currentTracks) && currentTrackIndex == newIndex){
            return;
        }

        //try to initialize player
        File song = newTracks[newIndex];

        player.reset();
        player_prepared = false;

        try {
            player.setDataSource(song.getAbsolutePath());
            player.prepare();
            player_prepared = true;
        }
        catch (Exception e){
            Log.d("failed to play file : ",e.toString());
            player.reset();
            player_prepared = false;
            return;
        }
        player.start();

        currentTracks = newTracks;
        currentTrackIndex = newIndex;

        for(StateChangeListener listener : listenerMap.values()){
            listener.onCurrentTrackChange(getCurrentTrack());
        }
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
        if(currentTracks == null || currentTracks.length <=0 || currentTrackIndex <0 || currentTrackIndex >= currentTracks.length){
            return null;
        }
        return  currentTracks[currentTrackIndex];
    }

    public File getNextTrack(){
        if(canGetTrackAt(currentTrackIndex+1)){
            return currentTracks[currentTrackIndex+1];
        }
        return null;
    }

    public File getPreviousTrack(){
        if(canGetTrackAt(currentTrackIndex - 1)){
            return currentTracks[currentTrackIndex-1];
        }
        return null;
    }

    public File[] getCurrentTracks(){
        if(currentTracks != null){
            return currentTracks.clone();
        }
        return null;
    }

    public void playNextTrack(){
        if(canGetTrackAt(currentTrackIndex+1)){
            playMusic(currentTracks, currentTrackIndex+1);
        }
    }

    public void playPreviousTrack(){
        if(canGetTrackAt(currentTrackIndex-1)){
            playMusic(currentTracks, currentTrackIndex-1);
        }
    }

    public boolean canGetTrackAt(int index){
        if(currentTracks == null ||
            currentTracks.length == 0 ||
            index < 0 ||
            index >= currentTracks.length
        ){
            return false;
        }
        return  true;
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

    public static class StateChangeListener{
        private Object listener;
        public StateChangeListener(Object listener){
            this.listener = listener;
        }

        public Object getListeningObject(){
            return listener;
        }

        public void onCurrentTrackChange(File track){}
    }

    public void addStateChangeListener(StateChangeListener listener){
        if(listenerMap.containsKey(listener.listener)){
            Log.d(TAG, "Each object can have only one listener");
        }
        listenerMap.put(listener.getListeningObject(), listener);
    }

    public void removeStateChangeListener(Object object){
        listenerMap.remove(object);
    }
}
