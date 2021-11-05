package com.example.musicplayer;

import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;
import com.example.musicplayer.id_to_file_manager.MediaItemUtil;
import com.example.musicplayer.id_to_file_manager.IdCache;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MediaPlaybackService extends MediaBrowserServiceCompat {
    private static final String TAG = "MediaPlaybackService";

    private static final String ROOT_ID = "ROOT_ID";
    private static final String MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id";

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;

    MusicPlayer musicPlayer = new MusicPlayer();
    ArrayList<MediaSessionCompat.QueueItem> currentTracks = new ArrayList<>();
    int currentIndex = 0;

    Handler currentPositionHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        // Create a MediaSessionCompat
        mediaSession = new MediaSessionCompat(getApplicationContext(), TAG);

        // Enable callbacks from MediaButtons and TransportControls
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY_FROM_URI |
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                );
        mediaSession.setPlaybackState(stateBuilder.build());
        // MySessionCallback() has methods that handle callbacks from a media controller
        mediaSession.setCallback(new MySessionCallback());
        // Set the session's token so that client activities can communicate with it.
        setSessionToken(mediaSession.getSessionToken());
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        return new BrowserRoot(ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        if (parentId == MY_EMPTY_MEDIA_ROOT_ID) {
            result.sendResult(null);
            return;
        } else if (parentId == ROOT_ID) {
            DirectoryTrackManager.getInstance().openDirectory(Environment.getExternalStorageDirectory());
        } else {
            MediaBrowserCompat.MediaItem toOpen = IdCache.getInstance().GetItemFromId(parentId);
            DirectoryTrackManager.getInstance().openDirectory(
                    new File(toOpen.getDescription().getMediaUri().getPath())
            );
        }

        //first try to get result from cached
        ArrayList<MediaBrowserCompat.MediaItem> cachedMediaItems = IdCache.getInstance().GetItems(parentId);

        if (cachedMediaItems != null) {
            result.sendResult(cachedMediaItems);
            return;
        }

        ArrayList<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        //this is a bit weird but we will actually include requested parent's parent as a child
        //of course lot of children will have the multiple same parent but it will simplify things massively

        //generating media items
        File parentParent = DirectoryTrackManager.getInstance().getParentFile();
        File[] childrenFiles = DirectoryTrackManager.getInstance().getCurrentFiles();

        if (parentParent != null)
            mediaItems.add(MediaItemUtil.FromFile(parentParent));
        if(childrenFiles != null) {
            for (File file : childrenFiles) {
                if (file != null)
                    mediaItems.add(MediaItemUtil.FromFile(file));
            }
        }

        IdCache.getInstance().CacheItems(parentId, mediaItems);

        result.sendResult(mediaItems);
    }

    private boolean canPlayNextTrack() {
        return canGetTrackAt(currentIndex + 1);
    }

    private boolean canPlayPreviousTrack() {
        return canGetTrackAt(currentIndex - 1);
    }

    private void playNextTrack() {
        if (canPlayNextTrack()) {
            MediaSessionCompat.QueueItem toPlay = currentTracks.get((currentIndex + 1));
            playFromUri(toPlay.getDescription().getMediaUri());
        }
    }

    private void playPreviousTrack() {
        if (canPlayPreviousTrack()) {
            MediaSessionCompat.QueueItem toPlay = currentTracks.get((currentIndex - 1));
            playFromUri(toPlay.getDescription().getMediaUri());
        }
    }

    private void seekTo(long pos){
        musicPlayer.seekTo(pos);
    }

    private long getActions(){
        long actions = PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PLAY_FROM_URI;
        if(musicPlayer.getCurrentSong() != null){
            if(musicPlayer.isPlayerPrepared()){
                actions = actions | PlaybackStateCompat.ACTION_SEEK_TO | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP;
                if(musicPlayer.isPlaying()){
                    actions = actions | PlaybackStateCompat.ACTION_PAUSE;
                }
                else{
                    actions = actions | PlaybackStateCompat.ACTION_PLAY;
                }
            }
        }
        if(canPlayNextTrack())
            actions = actions | PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        if(canPlayPreviousTrack())
            actions = actions | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;

        return actions;
    }

    private void play() {
        Log.d(TAG, "received play");

        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(
                        PlaybackStateCompat.STATE_PLAYING,
                        Math.max(musicPlayer.getCurrentPosition(), 0),
                        0
                )
                .setActions(getActions()).build());

        musicPlayer.play();
        if(musicPlayer.isPlaying()){
            mediaSession.setActive(true);
            startService(new Intent(this, MediaPlaybackService.class));
            startNotification();
            startUpdatingPosition();
        }
    }

    private void pause() {
        Log.d(TAG, "received pause");
        musicPlayer.pauseMusic();
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(
                        PlaybackStateCompat.STATE_PAUSED,
                        Math.max(musicPlayer.getCurrentPosition(), 0),
                        0
                ).setActions(getActions()).build());
        stopUpdatingPosition();

        stopForeground(false);
    }

    private void stop() {
        Log.d(TAG, "received stop");
        musicPlayer.stopMusic();
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(
                        PlaybackStateCompat.STATE_STOPPED,
                        0,
                        0
                )
                .setActions(getActions()).build());
        stopUpdatingPosition();
        stopSelf();
        mediaSession.setActive(false);
        stopForeground(true);
    }

    private void playFromMediaId(String mediaId) {
        MediaBrowserCompat.MediaItem item = IdCache.getInstance().GetItemFromId(mediaId);
        playFromUri(item.getDescription().getMediaUri());
    }

    private void playFromUri(Uri uri) {
        MediaBrowserCompat.MediaItem toPlay = IdCache.getInstance().GetItem(uri);
        if (toPlay == null) {
            Log.e(TAG, "Could not find Item that matches URI");
            return;
        }

        musicPlayer.playMusic(new File(toPlay.getDescription().getMediaUri().getPath()));

        //update current track list
        if (trackIsIn(toPlay) < 0) {
            String parentId = IdCache.getInstance().GetParent(toPlay.getMediaId());
            ArrayList<MediaBrowserCompat.MediaItem> items = null;
            if (parentId != null) {
                items = IdCache.getInstance().GetItems(parentId);
            }
            if (items != null) {
                //update current track list
                currentTracks.clear();
                long index = 0;
                for (MediaBrowserCompat.MediaItem item : items) {
                    if (item.isPlayable()) {
                        MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(item.getDescription(), index);
                        currentTracks.add(queueItem);
                        index++;
                    }
                }
                mediaSession.setQueue((ArrayList<MediaSessionCompat.QueueItem>) currentTracks.clone());
                currentIndex = trackIsIn(toPlay);
            }
        }else{
            currentIndex = trackIsIn(toPlay);
        }

        mediaSession.setMetadata(MediaItemUtil.GetMetaData(toPlay));
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder().
                setState(PlaybackStateCompat.STATE_PLAYING, 0, 1).
                setActions(getActions()).build());

        mediaSession.setActive(true);
        startService(new Intent(this, MediaPlaybackService.class));
        startNotification();

        startUpdatingPosition();
    }

    //https://stackoverflow.com/questions/64185475/android-communication-between-mediaplayer-and-seekbar-in-mediasession-architecut
    private void startUpdatingPosition(){
        currentPositionHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long currentPosition = musicPlayer.getCurrentPosition();
                if(currentPosition >= 0) {
                    PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                            .setActions(getActions())
                            .setState(PlaybackStateCompat.STATE_PLAYING, currentPosition, 1)
                            .build();
                    mediaSession.setPlaybackState(playbackState);
                }
                startUpdatingPosition();
            }
        }, 500);
    }

    private void stopUpdatingPosition(){
        if (currentPositionHandler != null) {
            currentPositionHandler.removeCallbacksAndMessages(null);
        }
    }

    class MySessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            if(musicPlayer.isPlaying())
                pause();
            else
                play();
        }

        @Override
        public void onStop() {
            stop();
        }


        @Override
        public void onPause() {
            pause();
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            playFromUri(uri);
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            playFromMediaId(mediaId);
        }

        @Override
        public void onSkipToNext() {
            playNextTrack();
        }

        @Override
        public void onSkipToPrevious() {
            playPreviousTrack();
        }

        @Override
        public void onSeekTo(long pos) {
            seekTo(pos);
        }
    }

    private int trackIsIn(MediaBrowserCompat.MediaItem toCheck) {
        if (currentTracks.size() == 0) {
            return -1;
        }
        for (int i = 0; i < currentTracks.size(); i++) {
            MediaSessionCompat.QueueItem toCompare = currentTracks.get(i);
            if (toCompare.getDescription().getMediaId() == toCheck.getMediaId()) {
                return i;
            }
        }
        return -1;
    }

    public boolean canGetTrackAt(long index) {
        if (currentTracks == null ||
                currentTracks.size() == 0 ||
                index < 0 ||
                index >= currentTracks.size()
        ) {
            return false;
        }
        return true;
    }

    public final int NOTIFICATION_ID = 1234;

    public void startNotification(){
        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        MediaDescriptionCompat description = mediaMetadata.getDescription();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), App.CHANNEL_ID);

        builder
                // Add the metadata for the currently playing track
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setSubText(description.getDescription())
                .setLargeIcon(description.getIconBitmap())

                // Enable launching the player by clicking the notification
                //.setContentIntent(controller.getSessionActivity())

                // Stop the service when the notification is swiped away
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(getApplicationContext(), PlaybackStateCompat.ACTION_STOP))

                // Make the transport controls visible on the lockscreen
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                // Add an app icon and set its accent color
                // Be careful about the color
                .setSmallIcon(R.drawable.file_icon)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.design_default_color_primary_dark))

                // Add a pause button
                .addAction(new NotificationCompat.Action(
                        R.drawable.file_icon, "pause",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(getApplicationContext(),
                        PlaybackStateCompat.ACTION_PAUSE)))

                // Take advantage of MediaStyle features
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0)
                        // Add a cancel button
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(getApplicationContext(),
                                PlaybackStateCompat.ACTION_STOP)));

// Display the notification and place the service in the foreground
        getApplicationContext().startForegroundService(new Intent(MediaPlaybackService.this, MediaPlaybackService.class));
        if(Build.VERSION.SDK_INT >= 29)
            startForeground(NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else
            startForeground(NOTIFICATION_ID, builder.build());
    }
}
