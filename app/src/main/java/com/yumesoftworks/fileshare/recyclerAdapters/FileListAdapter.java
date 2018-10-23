package com.yumesoftworks.fileshare.recyclerAdapters;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.yumesoftworks.fileshare.R;
import com.yumesoftworks.fileshare.data.FileListEntry;

import java.io.File;
import java.util.List;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileListViewHolder> {
    private static final String TAG="FileListAdapter";
    final private FileClickListener mFileClickListener;

    private List<FileListEntry> mFileList;
    private Context mContext;

    public FileListAdapter (Context context, FileClickListener listener){
        mContext=context;
        mFileClickListener=listener;
    }

    @NonNull
    @Override
    public FileListViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        // Inflate the task_layout to a view
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.item_tv_file_browser, viewGroup, false);

        return new FileListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileListViewHolder fileListViewHolder, int i) {
        FileListEntry fileListEntry=mFileList.get(i);

        //set values in view
        fileListViewHolder.tv_fileName.setText(fileListEntry.getFileName());

        Log.d(TAG,"mime type is "+fileListEntry.getMimeType());
        Log.d(TAG,"the path is "+fileListEntry.getPath());

        //we check if it is a directory
        if (fileListEntry.getDirectory()){
            //it is a directory
            fileListViewHolder.cv_selected.setVisibility(View.INVISIBLE);
            int imageUri = mContext.getResources().getIdentifier("icon_folder_128","drawable",mContext.getPackageName());
            Picasso.get().load(imageUri).into(fileListViewHolder.iv_icon);
        }else{
            //it is a file
            fileListViewHolder.cv_selected.setVisibility(View.VISIBLE);
            if (fileListEntry.getMimeType()!=null) {
                if (fileListEntry.getMimeType().startsWith("image")) {
                    Uri uri=Uri.fromFile(new File(fileListEntry.getPath()));
                    int tempUri = mContext.getResources().getIdentifier("icon_image_128","drawable",mContext.getPackageName());
                    Picasso.get()
                            .load(uri)
                            .placeholder(tempUri)
                            .into(fileListViewHolder.iv_icon);
                } else if (fileListEntry.getMimeType().startsWith("video")){
                    int tempUri = mContext.getResources().getIdentifier("icon_video_128","drawable",mContext.getPackageName());
                    Picasso.get().load(tempUri).into(fileListViewHolder.iv_icon);
                }else if (fileListEntry.getMimeType().startsWith("audio")){
                    int tempUri = mContext.getResources().getIdentifier("icon_music_128","drawable",mContext.getPackageName());
                    Picasso.get().load(tempUri).into(fileListViewHolder.iv_icon);
                }else {
                    int tempUri = mContext.getResources().getIdentifier("icon_file_128","drawable",mContext.getPackageName());
                    Picasso.get().load(tempUri).into(fileListViewHolder.iv_icon);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        if (mFileList == null) {
            return 0;
        }
        return mFileList.size();
    }

    //public method to return data in adapter
    public List<FileListEntry> getAvatarList(){
        return mFileList;
    }

    //public method to update adapter
    public void setFileList(List<FileListEntry> FileListData){
        Log.d(TAG,"settling new file list on the adapter");
        mFileList=FileListData;
        notifyDataSetChanged();
    }

    //public method to get adapter item
    public FileListEntry getFileItem(int itemId){
        return mFileList.get(itemId);
    }

    //ViewHolder
    class FileListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView iv_icon;
        TextView tv_fileName;
        CheckBox cv_selected;

        public FileListViewHolder(View itemView){
            super(itemView);

            iv_icon=itemView.findViewById(R.id.iv_item_file);
            tv_fileName=itemView.findViewById(R.id.tv_item_file_name);
            cv_selected=itemView.findViewById(R.id.cb_item_file);

            cv_selected.setOnClickListener(this);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            //we change the value of selected items
            if (mFileList.get(getAdapterPosition()).getIsSelected()==0){
                //we activate the checkbox
                mFileList.get(getAdapterPosition()).setIsSelected(1);
                cv_selected.setChecked(true);
            }else{
                //we deactivate the checkbox and values
                mFileList.get(getAdapterPosition()).setIsSelected(0);
                cv_selected.setChecked(false);
            }

            mFileClickListener.onItemClickListener(getAdapterPosition());
        }
    }

    //interface
    public interface FileClickListener{
        void onItemClickListener(int itemId);
    }
}