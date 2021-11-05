package com.example.musicplayer.id_to_file_manager;

import android.net.Uri;
import android.support.v4.media.MediaBrowserCompat;

import java.util.ArrayList;
import java.util.HashMap;

public class IdCache {
    private static IdCache instance = new IdCache();
    public static IdCache getInstance(){
        return instance;
    }

    private IdCache(){}

    HashMap<String , ArrayList<MediaBrowserCompat.MediaItem>> parentChildrenMap = new HashMap<>();
    HashMap<String, String> childParentMap = new HashMap<>();
    HashMap<String, MediaBrowserCompat.MediaItem> idItemMap = new HashMap<>();
    HashMap<Uri, MediaBrowserCompat.MediaItem> uriItemMap = new HashMap<>();

    public void CacheItems(String parentId, ArrayList<MediaBrowserCompat.MediaItem> items){
        parentChildrenMap.put(parentId, (ArrayList<MediaBrowserCompat.MediaItem>)items.clone());
        for(MediaBrowserCompat.MediaItem mediaItem : items){
            idItemMap.put(mediaItem.getMediaId(), mediaItem);
            uriItemMap.put(mediaItem.getDescription().getMediaUri(), mediaItem);
            childParentMap.put(mediaItem.getMediaId(), parentId);
        }
    }

    public MediaBrowserCompat.MediaItem GetItemFromId(String id){
        if(idItemMap.containsKey(id)){
            return idItemMap.get(id);
        }
        return null;
    }

    public ArrayList<MediaBrowserCompat.MediaItem> GetItems(String parentId){
        if(parentChildrenMap.containsKey(parentId)){
            return (ArrayList<MediaBrowserCompat.MediaItem>) parentChildrenMap.get(parentId).clone();
        }
        return null;
    }

    public String GetParent(String childId){
        if(childParentMap.containsKey(childId)){
            return childParentMap.get(childId);
        }
        return null;
    }

    public MediaBrowserCompat.MediaItem GetItem(Uri mediaUri){
        if(uriItemMap.containsKey(mediaUri)){
            return uriItemMap.get(mediaUri);
        }
        return null;
    }
}
