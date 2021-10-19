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
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity {
    static final int PERMISSION_CODE = 1;
    static final String [] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    ArrayList<View> entries = new ArrayList<View>();

    private boolean bound = false;
    private MusicPlayerService musicPlayerService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
            ViewGroup child = (ViewGroup)layoutInflater.inflate(R.layout.file_entry, view_parent, false);

            entries.add(child);

            TextView file_name_view = (TextView) child.findViewById(R.id.file_name);
            file_name_view.setText(singleFile.getName());

            ImageView thumbNail = child.findViewById(R.id.file_thumbnail);

            OnEntryClickListener listener = new OnEntryClickListener();
            listener.file = singleFile;
            child.findViewById(R.id.file_entry_button).setOnClickListener(listener);

            if(singleFile.isDirectory()){
                thumbNail.setImageResource(R.drawable.folder_icon);
            }
            else{
                thumbNail.setImageResource(R.drawable.file_icon);
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
                        Intent intent = new Intent(MainActivity.this, TrackPlayer.class);
                        intent.putExtra("CURRENT_TRACK", file.getAbsolutePath());
                        musicPlayerService.playMusic(file);
                        MainActivity.this.startActivity(intent);
                    }
                }
            }
        }
    }
}
