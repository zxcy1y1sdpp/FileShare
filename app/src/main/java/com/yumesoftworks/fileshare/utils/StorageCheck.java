package com.yumesoftworks.fileshare.utils;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StorageCheck {
    public List<File> getStorageList(){
        //List of storage
        File fullStorage=new File("/storage");

        //Internal path
        /*File internalStorage=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "");
        //go up 1 level to get the root
        internalStorage=new File(internalStorage.getParent());*/

        File internalStorage=new File(Environment.getExternalStorageDirectory().getAbsolutePath());

        //list inside fullStorage
        File[] listStorage=fullStorage.listFiles();
        List<File> finalListStorage=new ArrayList<>();
        finalListStorage.add(internalStorage);

        //final list
        for (File fileList: listStorage) {
            if (fileList.getPath().contains("self")||fileList.getPath().contains("emulated")){

            }else{
                finalListStorage.add(fileList);
            }
        }
         return finalListStorage;
    }
}
