package com.example.musicplayer;

import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class DirectoryTrackManager{
    private static final DirectoryTrackManager instance = new DirectoryTrackManager();

    public static DirectoryTrackManager getInstance() {
        return instance;
    }

    private DirectoryTrackManager() { }


    private File currentDirectory = null;
    private File[] currentFiles = null;
    private ArrayList<File> currentTracks = new ArrayList<File>();

    private static class FileSorter implements Comparator<File> {
        @Override
        public int compare(File f1, File f2) {
            int result = 0;

            if(f1.isDirectory()){
                result -= 999999;
            }
            if(f2.isDirectory()){
                result += 999999;
            }

            result += f1.getName().compareTo(f2.getName());
            return result;
        }
    }

    public File[] getCurrentFiles(){
        if(currentFiles == null){
            return null;
        }
        return currentFiles.clone();
    }

    public File getCurrentDirectory(){
        if(currentDirectory == null){
            return null;
        }
        return new File(currentDirectory.getAbsolutePath());
    }

    public File[] getCurrentTracks(){
        return (File[]) currentTracks.toArray();
    }

    public boolean openDirectory(File directory){
        try {
            if (directory != null && directory.exists() && directory.isDirectory()) {
                File[] files = directory.listFiles();

                if(files != null){
                    currentDirectory = directory;
                    Arrays.sort(files, new FileSorter());
                    currentFiles = files;

                    currentTracks.clear();

                    for(File file : currentFiles){
                        if(MusicPlayerService.supportsFormat(file)){
                            currentTracks.add(file);
                        }
                    }
                    return true;
                }
            }
        }
        catch (Exception e){
            return false;
        }
        return false;
    }

    File getPreviousTrack(File file){
        if(currentTracks.isEmpty()){
            return null;
        }
        for(int i=0; i<currentTracks.size(); i++){
            if(currentTracks.get(i).getAbsolutePath() == file.getAbsolutePath()){
                if(i >= 1){
                    return currentTracks.get(i-1);
                }
            }
        }
        return null;
    }

    File getNextTrack(File file){
        if(currentTracks.isEmpty()){
            return null;
        }
        for(int i=0; i<currentTracks.size(); i++){
            if(currentTracks.get(i).getAbsolutePath() == file.getAbsolutePath()){
                if(i < currentTracks.size()-1){
                    return currentTracks.get(i+1);
                }
            }
        }
        return null;
    }

    public boolean openParent(){
        if (currentDirectory != null && currentDirectory.exists() && currentDirectory.isDirectory()){
            File parent = currentDirectory.getParentFile();
            return openDirectory(parent);
        }
        return false;
    }
}
