package com.example.musicplayer;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    static final int PERMISSION_CODE = 1;
    static final String [] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    ArrayList<View> entries = new ArrayList<View>();

    private boolean bound = false;
    private MusicPlayerService musicPlayerService;

    private ViewGroup widget;
    private ImageView thumbNail;
    private ImageButton playButton;
    private ImageButton nextButton;
    private TextView musicName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        widget = findViewById(R.id.player_widget);
        thumbNail = findViewById(R.id.player_widget_thumbnail);
        playButton = findViewById(R.id.player_widget_play);
        nextButton = findViewById(R.id.player_widget_next);
        musicName = findViewById(R.id.player_widget_music_name);

        musicName.setSelected(true);

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

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bound){
                    File currentTrack = musicPlayerService.getCurrentTrack();
                    File nextTrack = DirectoryTrackManager.getInstance().getNextTrack(currentTrack);
                    if(nextTrack != null){
                        musicPlayerService.playMusic(nextTrack);
                    }
                }
            }
        });

        widget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bound){
                    File currentTrack = musicPlayerService.getCurrentTrack();
                    if(currentTrack != null){
                        startTrackPlayer(currentTrack);
                    }
                }
            }
        });

        if(!checkPermission()) {
            requestPermissions(REQUIRED_PERMISSIONS, PERMISSION_CODE);
        }
        else{
            if(DirectoryTrackManager.getInstance().getCurrentDirectory() == null){
                DirectoryTrackManager.getInstance().openDirectory(Environment.getExternalStorageDirectory());
            }
            displayFiles();
        }
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder _binder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MusicPlayerService.MusicPlayerBinder binder = (MusicPlayerService.MusicPlayerBinder) _binder;
            musicPlayerService = binder.getService();
            bound = true;

            updateWidget();
            musicPlayerService.addStateChangeListener(onStateChange);
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

    private boolean checkPermission(){
        int sum = 0;
        int result = 0;
        for(String permission : REQUIRED_PERMISSIONS){
            result = checkSelfPermission(permission);
            if(result == PackageManager.PERMISSION_GRANTED){
                sum+=1;
            }
        }
        return sum == REQUIRED_PERMISSIONS.length;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSION_CODE){
            for(int result : grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                    return;
                }
            }
        }
        if(DirectoryTrackManager.getInstance().getCurrentDirectory() == null){
            DirectoryTrackManager.getInstance().openDirectory(Environment.getExternalStorageDirectory());
        }
        displayFiles();
    }

    public void displayFiles(){
        File directory = DirectoryTrackManager.getInstance().getCurrentDirectory();
        if(directory == null || !directory.isDirectory()){
            return;
        }

        File [] files = DirectoryTrackManager.getInstance().getCurrentFiles();

        if(files == null){
            return;
        }

        ViewGroup view_parent = (ViewGroup) findViewById(R.id.file_container);

        //clear view
        for(View view : entries){
            view_parent.removeView(view);
        }

        LayoutInflater layoutInflater = getLayoutInflater();

        //add directory that goes to parent
        //TODO: design better way to go to parent directory
        File parentDirectory = directory.getParentFile();
        if(parentDirectory != null){
            ViewGroup parentDirectoryEntry = (ViewGroup)layoutInflater.inflate(R.layout.file_entry, view_parent, false);

            entries.add(parentDirectoryEntry);
            TextView parentDirectoryTextView = (TextView) parentDirectoryEntry.findViewById(R.id.file_name);
            parentDirectoryTextView.setText("..");
            ImageView parentDirectoryImageView = (ImageView) parentDirectoryEntry.findViewById(R.id.file_thumbnail);
            parentDirectoryImageView.setImageResource(R.drawable.folder_icon);

            OnEntryClickListener go_to_parent = new OnEntryClickListener();
            go_to_parent.file = parentDirectory;

            parentDirectoryEntry.findViewById(R.id.file_entry_button).setOnClickListener(go_to_parent);

            view_parent.addView(parentDirectoryEntry);
        }

        for(File singleFile : files){
            //generate view
            ViewGroup child = (ViewGroup)layoutInflater.inflate(R.layout.file_entry, view_parent, false);

            entries.add(child);

            //get view in child
            TextView file_name_view = (TextView) child.findViewById(R.id.file_name);
            file_name_view.setText(singleFile.getName());

            ImageView thumbNail = child.findViewById(R.id.file_thumbnail);

            //set listener
            OnEntryClickListener listener = new OnEntryClickListener();
            listener.file = singleFile;
            child.findViewById(R.id.file_entry_button).setOnClickListener(listener);

            if(singleFile.isDirectory()){
                thumbNail.setImageResource(R.drawable.folder_icon);
            }
            else{
                thumbNail.setImageResource(R.drawable.file_icon);
                if(musicPlayerService.supportsFormat(singleFile)){
                    MetaDataGetter.MetaDataRequester requester = new MetaDataGetter.MetaDataRequester(singleFile){
                        @Override
                        public void onGotMetaData(Bitmap art){
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
                else{
                    thumbNail.setImageResource(R.drawable.file_icon);
                }
            }

            view_parent.addView(child);
        }
    }

    private class OnEntryClickListener implements View.OnClickListener{
        public File file;
        @Override
        public void onClick(View v) {
            if(file.isDirectory()){
                DirectoryTrackManager.getInstance().openDirectory(file);
                displayFiles();
            }
            else {
                if(bound){
                    if(MusicPlayerService.supportsFormat(file)){
                        startTrackPlayer(file);
                    }
                }
            }
        }
    }

    private MusicPlayerService.StateChangeListener onStateChange = new MusicPlayerService.StateChangeListener(this){
        @Override
        public void onCurrentTrackChange(File track) {
            updateWidget();
        }
    };

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(bound){
            musicPlayerService.removeStateChangeListener(this);
        }
    }


    private void updateWidget(){
        if(bound){
            File currentTrack = musicPlayerService.getCurrentTrack();
            if(currentTrack == null){
                widget.setVisibility(View.GONE);
                return;
            }
            else{
                widget.setVisibility(View.VISIBLE);
                MetaDataGetter.MetaDataRequester requester = new MetaDataGetter.MetaDataRequester(currentTrack){
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

                musicName.setText(currentTrack.getName());
            }
        }
        else{
            widget.setVisibility(View.GONE);
        }
    }

    private void startTrackPlayer(File track){
        Intent intent = new Intent(MainActivity.this, TrackPlayer.class);
        intent.putExtra("CURRENT_TRACK", track.getAbsolutePath());
        musicPlayerService.playMusic(track);
        MainActivity.this.startActivity(intent);
    }
}
