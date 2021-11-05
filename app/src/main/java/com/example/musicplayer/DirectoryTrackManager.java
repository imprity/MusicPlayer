package com.example.musicplayer;

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
    private File previousDirectory = null;
    private File parentFile = null;
    private File[] currentFiles = null;

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

    public boolean openDirectory(File directory){
        try {
            if (directory != null && directory.exists() && directory.isDirectory()) {
                File[] files = directory.listFiles();

                if(files != null){
                    previousDirectory = currentDirectory;
                    currentDirectory = directory;

                    Arrays.sort(files, new FileSorter());
                    currentFiles = files;

                    parentFile = currentDirectory.getParentFile();
                    return true;
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public boolean openParent(){
        if (parentFile != null && parentFile.isDirectory()){
            return openDirectory(parentFile);
        }
        return false;
    }

    File getParentFile(){
        if(parentFile == null){
            return null;
        }
        return new File(parentFile.getAbsolutePath());
    }
}
