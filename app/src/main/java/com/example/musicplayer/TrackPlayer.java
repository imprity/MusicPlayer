package com.example.musicplayer;

import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;

public class TrackPlayer extends AppCompatActivity {
    ImageView thumbNail;
    ImageButton prevButton;
    ImageButton nextButton;
    ImageButton playButton;
    TextView musicText;
    SeekBar seekBar;

    private boolean bound = false;
    private MusicPlayerService musicPlayerService;

    private boolean touchingSeekBar = false;

    //subtracting two cause I'm scared lol
    private final static int SEEK_BAR_MAX_VALUE = Integer.MAX_VALUE -2;

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

        Intent intent = getIntent();
        File currentTrack = new File(intent.getStringExtra("CURRENT_TRACK"));

        updateUI(currentTrack);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bound){
                    if(musicPlayerService.isPlaying()){
                       musicPlayerService.pauseMusic();
                    }
                    else{
                        musicPlayerService.resumeMusic();
                    }
                }
            }
        });

        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bound){
                    File previousTrack = DirectoryTrackManager.getInstance().getPreviousTrack(musicPlayerService.getCurrentTrack());
                    if(previousTrack!= null){
                        musicPlayerService.playMusic(previousTrack);
                    }
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bound){
                    File nextTrack = DirectoryTrackManager.getInstance().getNextTrack(musicPlayerService.getCurrentTrack());
                    if(nextTrack!= null){
                        musicPlayerService.playMusic(nextTrack);
                    }
                }
            }
        });

        seekBar.setMin(0);
        //arbitrary number
        seekBar.setMax(SEEK_BAR_MAX_VALUE);

        Handler seekBarHandler = new Handler();
        seekBarHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(bound){
                    long pos = musicPlayerService.getCurrentPosition();
                    long duration = musicPlayerService.getDuration();
                    if(pos != -1 && duration != -1 && !touchingSeekBar){
                        double newPos = (Double.valueOf(pos) / Double.valueOf(duration)) * Double.valueOf(SEEK_BAR_MAX_VALUE);
                        seekBar.setProgress((int)newPos);
                    }
                }
                seekBarHandler.postDelayed(this, 10);
            }
        }, 10);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    if(bound){
                        long duration = musicPlayerService.getDuration();
                        if(duration != -1){
                            double newPos = (Double.valueOf(progress)/Double.valueOf(SEEK_BAR_MAX_VALUE)) * Double.valueOf(duration);
                            musicPlayerService.seekTo((long)newPos);
                        }
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                touchingSeekBar = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                touchingSeekBar = false;
            }
        });
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder _binder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MusicPlayerService.MusicPlayerBinder binder = (MusicPlayerService.MusicPlayerBinder) _binder;
            musicPlayerService = binder.getService();
            bound = true;

            musicPlayerService.addStateChangeListener(listener);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };

    @Override
    protected void onStart(){
        super.onStart();
        Intent intent = new Intent(getApplicationContext(), MusicPlayerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(bound){
            musicPlayerService.removeStateChangeListener(this);
        }
    }

    class SeekBarThread extends Thread{
        public Handler handler;

        public void run() {
            Looper.prepare();

            handler = new Handler(Looper.myLooper()) {
                public void handleMessage(Message msg) {
                    // process incoming messages here
                }
            };

            Looper.loop();
        }
    }

    private void updateUI(File track){
        musicText.setText(track.getName());

        MetaDataGetter.MetaDataRequester requester = new MetaDataGetter.MetaDataRequester(track){
            @Override
            public void onGotMetaData(Bitmap art) {
                if(art != null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            thumbNail.setImageBitmap(art);
                        }
                    });
                }
            }
        };
        MetaDataGetter.getInstance().requestMetaData(requester);
    }

    private MusicPlayerService.StateChangeListener listener = new MusicPlayerService.StateChangeListener(this){
        @Override
        public void onCurrentTrackChange(File track) {
            updateUI(track);
        }
    };
}