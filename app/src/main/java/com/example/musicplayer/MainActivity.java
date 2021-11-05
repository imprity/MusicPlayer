package com.example.musicplayer;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.musicplayer.id_to_file_manager.IdCache;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private MediaBrowserCompat mediaBrowser;

    static final int PERMISSION_CODE = 1;
    static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    ArrayList<View> entries = new ArrayList<View>();

    private ViewGroup widget;
    private ImageView thumbNail;
    private ImageButton playButton;
    private ImageButton nextButton;
    private TextView musicName;


    private MediaBrowserCompat.SubscriptionCallback subscriptionCallback = new MediaBrowserCompat.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
            displayMediaItems(new ArrayList<>(children));
        }
        @Override
        public void onError(@NonNull String parentId) {
            Log.d(TAG, "ERROR : " + parentId);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!checkPermission()) {
            requestPermissions(REQUIRED_PERMISSIONS, PERMISSION_CODE);
        } else {
            initMediaBrowser();
        }
        widget = findViewById(R.id.player_widget);
        widget.setVisibility(View.GONE);
    }

    private void initMediaBrowser() {
        mediaBrowser = new MediaBrowserCompat(
                this,
                new ComponentName(this, MediaPlaybackService.class),
                connectionCallbacks,
                null
        );
        mediaBrowser.connect();
    }

    @Override
    protected void onResume(){
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (MediaControllerCompat.getMediaController(MainActivity.this) != null) {
            MediaControllerCompat.getMediaController(MainActivity.this).unregisterCallback(controllerCallback);
        }
        mediaBrowser.disconnect();
    }

    private boolean checkPermission() {
        int sum = 0;
        int result = 0;
        for (String permission : REQUIRED_PERMISSIONS) {
            result = checkSelfPermission(permission);
            if (result == PackageManager.PERMISSION_GRANTED) {
                sum += 1;
            }
        }
        return sum == REQUIRED_PERMISSIONS.length;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                    return;
                }
            }
        }
        initMediaBrowser();
    }

    public void displayMediaItems(ArrayList<MediaBrowserCompat.MediaItem> mediaItems){
        ViewGroup view_parent = (ViewGroup) findViewById(R.id.file_container);

        //clear view
        for(View view : entries){
            view_parent.removeView(view);
        }

        LayoutInflater layoutInflater = getLayoutInflater();

        for(MediaBrowserCompat.MediaItem item : mediaItems){
            MediaDescriptionCompat desc = item.getDescription();
            //generate view
            ViewGroup child = (ViewGroup)layoutInflater.inflate(R.layout.file_entry, view_parent, false);

            entries.add(child);

            //get view in child
            TextView file_name_view = (TextView) child.findViewById(R.id.file_name);
            file_name_view.setText(desc.getTitle());

            ImageView thumbNail = child.findViewById(R.id.file_thumbnail);

            //set listener
            OnEntryClickListener listener = new OnEntryClickListener();
            listener.item = item;
            child.findViewById(R.id.file_entry_button).setOnClickListener(listener);

            if(item.isBrowsable()){
                thumbNail.setImageResource(R.drawable.folder_icon);
            }
            else{
                thumbNail.setImageResource(R.drawable.file_icon);
                Bitmap bitmap = item.getDescription().getIconBitmap();
                if(item.isPlayable() && bitmap != null){
                    thumbNail.setImageBitmap(bitmap);
                }
            }

            view_parent.addView(child);
        }
    }

    private class OnEntryClickListener implements View.OnClickListener{
        public MediaBrowserCompat.MediaItem item;
        @Override
        public void onClick(View v) {
            if(item.isBrowsable()){
                ArrayList<MediaBrowserCompat.MediaItem> items = IdCache.getInstance().GetItems(item.getMediaId());
                if(items == null){
                    mediaBrowser.subscribe(item.getMediaId(), subscriptionCallback);
                }
                else{
                    displayMediaItems(items);
                }
            }
            else {
                if(item.isPlayable()){
                    MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().playFromUri(item.getDescription().getMediaUri(), null);
                    startActivity(new Intent(getApplicationContext(), TrackPlayer.class));
                }
            }
        }
    }


    private final MediaBrowserCompat.ConnectionCallback connectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    // Get the token for the MediaSession
                    MediaSessionCompat.Token token = mediaBrowser.getSessionToken();
                    // Create a MediaControllerCompat
                    MediaControllerCompat mediaController = new MediaControllerCompat(MainActivity.this, token);
                    //Save the controller
                    MediaControllerCompat.setMediaController(MainActivity.this, mediaController);

                    String rootId = mediaBrowser.getRoot();
                    mediaBrowser.subscribe(rootId, subscriptionCallback);
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

    void buildTransportControls() {
    }

    MediaControllerCompat.Callback controllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    Log.d(TAG, "Meta data changed :\n"+metadata.toString());
                }

                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    Log.d(TAG, "playback state changed :\n"+state.toString());
                }

            };
}
