package com.yumesoftworks.fileshare.peerToPeer;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.yumesoftworks.fileshare.SenderPickDestinationActivity;
import com.yumesoftworks.fileshare.TransferProgressActivity;
import com.yumesoftworks.fileshare.data.TextInfoSendObject;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceiverSocketTransfer {
    private static final String TAG="ServiceClientSocket";

    //actions
    private static final int ACTION_SEND_MESSAGE=1001;
    private static final int ACTION_RECEIVE_FILE=1002;
    private static final int ACTION_RECEIVE_DETAILS=1003;

    //local server socket
    private ServerSocket mServerSocket;
    private Socket mSocket;

    //thread
    private Handler socketHandler;
    private Thread socketThread;

    //current action
    private int mCurrentAction;

    //file numbers
    private int mCurrentFile;
    private int mTotalFiles;

    //interface
    private ClientSocketTransferInterface mReceiverInterface;

    public ReceiverSocketTransfer(Context context, ServerSocket serverSocketPort){
        mServerSocket = serverSocketPort;
        mReceiverInterface=(ClientSocketTransferInterface) context;

        socketHandler=new Handler();
        socketThread=new Thread(new CommunicationThread());
        socketThread.start();
    }

    class CommunicationThread implements Runnable{
        @Override
        public void run() {
            while(true){
                // Socket object
                try {
                    //wait for a connection
                    Log.d(TAG, "Waiting for the socket to be connected " + mServerSocket.getLocalPort());

                    mSocket = mServerSocket.accept();
                    mCurrentAction=ACTION_RECEIVE_DETAILS;

                    //loop for sending and receiving
                    Boolean keepLoop=true;
                    while (keepLoop) {

                        if (mCurrentAction==ACTION_SEND_MESSAGE) {
                            //we read the messages sent by sender
                            try {
                                TextInfoSendObject textInfoSendObject=new TextInfoSendObject(TransferProgressActivity.TYPE_FILE_TRANSFER_SUCCESS,"","");
                                ObjectOutputStream messageOut = new ObjectOutputStream(mSocket.getOutputStream());
                                messageOut.writeObject(textInfoSendObject);
                                //reset action to receive details
                                mCurrentAction=ACTION_RECEIVE_DETAILS;
                            } catch (Exception e) {
                                Log.d(TAG, "There is no output stream " + e.getMessage());
                            }
                        }

                        //receiving
                        if (mCurrentAction==ACTION_RECEIVE_DETAILS){
                            //we read the object
                            try {
                                ObjectInputStream messageIn = new ObjectInputStream(mSocket.getInputStream());
                                TextInfoSendObject message = (TextInfoSendObject) messageIn.readObject();

                                //update the ui
                                mReceiverInterface.updateSendUI(message);
                                /*if (message == SenderPickDestinationActivity.MESSAGE_OPEN_ACTIVITY) {
                                    //we will open the new activity and wait for the connection via interface
                                    //mReceiverInterface.openNexActivity();
                                }*/
                                //change the action to get ready to receive file
                                mCurrentAction=ACTION_RECEIVE_FILE;

                            } catch (Exception e) {
                                Log.d(TAG, "There is no input stream " + e.getMessage());
                            }
                        }
                        if (mCurrentAction==ACTION_RECEIVE_FILE){
                            //we receive the bytes and then save it

                            //we store the file
                            mCurrentAction=ACTION_SEND_MESSAGE;
                        }

                        //we check if it is the last file and we finish
                        if (mCurrentFile==mTotalFiles){
                            //we call the finish
                            mReceiverInterface.finishedReceiveClient();
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "the socket accept has failed, try again");
                    mReceiverInterface.socketFailedClient();
                }
            }
        }
    }

    //inerface
    public interface ClientSocketTransferInterface{
        void finishedReceiveClient();
        void socketFailedClient();
        void updateSendUI(TextInfoSendObject textInfoSendObject);
    }

    //kill the socket
    public void destroySocket(){
        //cancel socket
        Log.d(TAG,"Trying to close socket");
        try {
            mSocket.close();
        }catch (Exception e){
            Log.d(TAG,"Cannot close socket "+e.getMessage());
        }

        //destroy thread
        socketThread.interrupt();
    }
}

