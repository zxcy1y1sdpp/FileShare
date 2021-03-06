package com.yumesoftworks.fileshare;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.annotation.NonNull;

import com.yumesoftworks.fileshare.data.AppDatabase;
import com.yumesoftworks.fileshare.data.FileListEntry;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FileTransferViewModel extends AndroidViewModel {
    private static LiveData<List<FileListEntry>> data;
    private String TAG = "FileTransferViewModel";
    private AppDatabase database;

    public FileTransferViewModel(@NonNull Application application) {
        super(application);

        //we load the database
        database=AppDatabase.getInstance(this.getApplication());
        data=database.fileListDao().loadFileList();
    }

    public LiveData<List<FileListEntry>> getFileListInfo(){
        return data;
    }

    //update the data, once it has been copied
    /*public void updateFile(final FileListEntry fileListEntry){
        Executor myExecutor = Executors.newSingleThreadExecutor();
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                database.fileListDao().updateFile(fileListEntry);
            }
        });


        //new updateDatabaseAsyncTask(database).execute(fileListEntry);
    }*/

    /*private static class updateDatabaseAsyncTask extends AsyncTask<FileListEntry,Void,Void> {
        private AppDatabase database;

        updateDatabaseAsyncTask(AppDatabase recDatabase){
            database=recDatabase;
        }

        @Override
        protected Void doInBackground(final FileListEntry... params) {
            //we save the new file
            database.fileListDao().updateFile(params[0]);
            return null;
        }
    }*/
}
