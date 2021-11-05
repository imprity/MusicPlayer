package com.example.musicplayer;

import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;

public class TrackPlayer extends AppCompatActivity {
    private static final String TAG = "TrackPlayer";
    ImageView thumbNail;
    ImageButton prevButton;
    ImageButton nextButton;
    ImageButton playButton;
    TextView musicText;
    SeekBar seekBar;

    private MediaBrowserCompat mediaBrowser;
    long currentDuration = 100;

    private final MediaBrowserCompat.ConnectionCallback connectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    // Get the token for the MediaSession
                    MediaSessionCompat.Token token = mediaBrowser.getSessionToken();
                    // Create a MediaControllerCompat
                    MediaControllerCompat mediaController = new MediaControllerCompat(TrackPlayer.this, token);
                    //Save the controller
                    MediaControllerCompat.setMediaController(TrackPlayer.this, mediaController);

                    buildTransportControls();
                }

                @Override
                public void onConnectionSuspended() {
                    Log.d(TAG, "media browser connection suspended");
                }

                @Override
                public void onConnectionFailed() {
                    Log.d(TAG, "media browser connection failed");
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_player);

        seekBar = findViewById(R.id.seekBar);
        prevButton = findViewById(R.id.previous_button);
        nextButton = findViewById(R.id.next_button);
        playButton = findViewById(R.id.play_button);
        musicText = findViewById(R.id.music_name);
        thumbNail = findViewById(R.id.music_thumbnail);

        musicText.setSelected(true);

        mediaBrowser = new MediaBrowserCompat(
                this,
                new ComponentName(this, MediaPlaybackService.class),
                connectionCallbacks,
                null
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        mediaBrowser.connect();
    }

    @Override
    protected void onResume(){
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (MediaControllerCompat.getMediaController(TrackPlayer.this) != null) {
            MediaControllerCompat.getMediaController(TrackPlayer.this).unregisterCallback(controllerCallback);
        }
        mediaBrowser.disconnect();
    }

    void buildTransportControls() {
        //MediaControllerCompat controller =
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pbState = MediaControllerCompat.getMediaController(TrackPlayer.this).getPlaybackState().getState();
                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                    MediaControllerCompat.getMediaController(TrackPlayer.this).getTransportControls().pause();
                } else {
                    MediaControllerCompat.getMediaController(TrackPlayer.this).getTransportControls().play();
                }
            }
        });

        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControllerCompat.getMediaController(TrackPlayer.this).getTransportControls().skipToPrevious();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControllerCompat.getMediaController(TrackPlayer.this).getTransportControls().skipToNext();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                MediaControllerCompat controller =MediaControllerCompat.getMediaController(TrackPlayer.this);
                PlaybackStateCompat pbState = controller.getPlaybackState();
                if((pbState.getActions() & PlaybackStateCompat.ACTION_SEEK_TO) > 0){
                    int progress = seekBar.getProgress();
                    double normalized = (double) progress / (double) seekBar.getMax();
                    long newPos = (long)Math.floor(normalized * (double) currentDuration);
                    controller.getTransportControls().seekTo(newPos);
                }
            }
        });

        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(TrackPlayer.this);
        mediaController.registerCallback(controllerCallback);

        MediaMetadataCompat metadata = mediaController.getMetadata();
        updateUI(metadata);
    }


    MediaControllerCompat.Callback controllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    Log.d(TAG, "Meta data changed :\n"+metadata.toString());
                    updateUI(metadata);
                    Log.d(TAG, "current duration: " + Long.toString(currentDuration));
                }

                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    long currentPosition = state.getPosition();
                    double normalized = (double)currentPosition / (double)currentDuration;
                    int maxValue = seekBar.getMax();
                    seekBar.setProgress((int)Math.floor((double) maxValue * normalized));
                }
            };

    private void updateUI(MediaMetadataCompat metadata){
        Bitmap albumArt = metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART);
        if(albumArt != null){
            thumbNail.setImageBitmap(albumArt);
        }
        currentDuration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        musicText.setText(metadata.getDescription().getTitle());
    }
}