package com.yumesoftworks.fileshare;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.yumesoftworks.fileshare.data.FileListEntry;

import java.util.ArrayList;
import java.util.List;

public class TransferProgressActivity extends AppCompatActivity implements
        FileTransferProgress.OnFragmentInteractionListener{

    private static final String TAG="TransferProgressAct";

    //extras names
    //general
    public static final String LOCAL_BROADCAST_REC="LocalBroadCastFileShare";
    public static final String EXTRA_TYPE_TRANSFER="ExtraType";
    public static final String LOCAL_IP="LocalIp";
    public static final String REMOTE_IP="RemoteIp";
    public static final String LOCAL_PORT="LocalPort";
    public static final String REMOTE_PORT="RemotePort";

    //type of message sent on text object
    public static final int TYPE_END=1001;
    public static final int TYPE_FILE_DETAILS=1002;
    public static final int TYPE_FILE_DETAILS_SUCCESS=1003;
    public static final int TYPE_FILE_TRANSFER_SUCCESS=1004;

    //broadcast actions
    public static final String ACTION_FINISHED_TRANSFER="finishedTransfer";
    public static final String ACTION_UPDATE_UI="updateUI";
    public static final String ACTION_SOCKET_ERROR="connectionError";

    //name of bundle objects coming from service
    public static final String ACTION_UPDATE_UI_DATA="updateUIData";

    private LinearLayout mProgressBarHide;

    //this activity context
    private Context thisActivity;

    //constants for the actions
    public static final int FILES_SENDING=2001;
    public static final int FILES_RECEIVING=2002;
    public static final int RELAUNCH_APP=2003;
    public static final String ACTION_SERVICE ="ReceivingFiles";

    //fragment parts
    private FileTransferProgress fragmentFileTransferProgress;
    private FragmentManager fragmentManager;

    //analytics and admob
    private FirebaseAnalytics mFireAnalytics;

    //viewmodel
    private FileTransferViewModel fileTransferViewModel;

    //type
    private int typeOfService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_progress);

        //we check the intent with the information to start the service
        Intent intent=getIntent();

        //intent
        Intent serviceIntent=new Intent(this,ServiceFileShare.class);
        Bundle extras=intent.getExtras();

        //get the data to see how do we start the service
        typeOfService=extras.getInt(EXTRA_TYPE_TRANSFER);

        thisActivity=this;

        //analytics
        mFireAnalytics=FirebaseAnalytics.getInstance(this);

        //we get the instance of the indeterminate progress bar
        mProgressBarHide=findViewById(R.id.pb_atp_waitingForConnection);

        if (typeOfService==RELAUNCH_APP){
            //nothing happens since everything has been initialized

        }else {
            //first initialization
            //choose data in the intent
            if (typeOfService == FILES_SENDING) {
                //we start the services as sending stuff
                extras.putInt(ACTION_SERVICE, FILES_SENDING);
            } else if (typeOfService == FILES_RECEIVING) {
                //we start the service as receiving stuff
                extras.putInt(ACTION_SERVICE, FILES_RECEIVING);
            }

            //start the service
            serviceIntent.putExtras(extras);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            //initialize fragments
            initializeFragments();

            //get the broadcast receivers for responses from the service
            IntentFilter intentFilter=new IntentFilter(LOCAL_BROADCAST_REC);
            intentFilter.addAction(ACTION_FINISHED_TRANSFER);
            intentFilter.addAction(ACTION_SOCKET_ERROR);
            intentFilter.addAction(ACTION_UPDATE_UI);
            intentFilter.addAction(ACTION_UPDATE_UI_DATA);
            LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceived, intentFilter);
        }

        //toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.tp_toolbar);
        setSupportActionBar(myToolbar);
    }

    private void initializeFragments(){
        //manager
        fragmentManager=getSupportFragmentManager();

        //fragments
        fragmentFileTransferProgress=new FileTransferProgress();

        Bundle bundleFrag=new Bundle();
        bundleFrag.putInt(EXTRA_TYPE_TRANSFER,typeOfService);
        fragmentFileTransferProgress.setArguments(bundleFrag);

        //transaction
        fragmentManager.beginTransaction()
                .add(R.id.frag_atp_transfer_progress,fragmentFileTransferProgress)
                .commit();

        //we get the file model to populate the stuff
        fileTransferViewModel=ViewModelProviders.of(this).get(FileTransferViewModel.class);
        fileTransferViewModel.getFileListInfo().observe(this,fileTransferViewModelObserver);
    }

    //observer
    final Observer<List<FileListEntry>> fileTransferViewModelObserver=new Observer<List<FileListEntry>>() {
        @Override
        public void onChanged(@Nullable List<FileListEntry> fileListEntries) {
            //we create a list for the not transferred and one for the transferred

            List<FileListEntry> tempSent=new ArrayList<>();
            List<FileListEntry> tempNotSent=new ArrayList<>();

            for (int i=0;i<fileListEntries.size();i++){
                if (fileListEntries.get(i).getIsTransferred()==0){
                    tempNotSent.add(fileListEntries.get(i));
                }else{
                    tempSent.add(fileListEntries.get(i));
                }
            }

            if (typeOfService==FILES_SENDING) {
                fragmentFileTransferProgress.updateRV(tempNotSent);
            }else{
                fragmentFileTransferProgress.updateRV(fileListEntries);
            }
        }
    };

    //broadcast receiving
    private BroadcastReceiver mMessageReceived=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            Log.d(TAG,"Received message from service "+action);

            //we check what to do depending on what the service needs to do
            switch (action){
                case ACTION_UPDATE_UI:
                    mProgressBarHide.setVisibility(View.GONE);
                    //update ui
                    Bundle bundle=intent.getExtras();

                    //send the data to the fragment
                    fragmentFileTransferProgress.updateData(bundle);

                    break;
                case ACTION_FINISHED_TRANSFER:
                    //we show dialog that transfer is done
                    AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity);
                    builder.setMessage(R.string.service_finished_transfer)
                            .setCancelable(true)
                            .setNeutralButton(R.string.gen_button_ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                        }
                                    });
                    builder.show();
                    //change button to ok
                    fragmentFileTransferProgress.changeButton();
                    /*builder.setMessage(R.string.service_finished_transfer)
                            .setCancelable(true)
                            .setNeutralButton(R.string.gen_button_ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            //we go to the main activity
                                            Intent intent=new Intent(getApplicationContext(),WelcomeScreenActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                        }
                                    });
                    builder.show();*/
                    break;
                case ACTION_SOCKET_ERROR:
                    //we show dialog that there was an error and return to the main menu
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(thisActivity);
                    builder2.setMessage(R.string.service_socket_error)
                            .setCancelable(true)
                            .setNeutralButton(R.string.gen_button_ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                        }
                                    });

                    /*builder2.setMessage(R.string.service_socket_error)
                            .setCancelable(true)
                            .setNeutralButton(R.string.gen_button_ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            //we go to the main activity
                                            Intent intent=new Intent(getApplicationContext(),WelcomeScreenActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                        }
                                    });*/
                    builder2.show();
                    //change button to ok
                    fragmentFileTransferProgress.changeButton();
                    break;
            }
        }
    };

    @Override
    public void buttonOkCancel(String received){
        //we check if it ended or if we are cancelling it
        if(received==getResources().getString(R.string.gen_button_cancel)){
            Intent serviceIntent=new Intent(this,ServiceFileShare.class);
            stopService(serviceIntent);
        }

        Intent intent=new Intent(getApplicationContext(),WelcomeScreenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}