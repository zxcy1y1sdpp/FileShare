package com.yumesoftworks.fileshare.peerToPeer;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.yumesoftworks.fileshare.TransferProgressActivity;
import com.yumesoftworks.fileshare.data.FileListEntry;
import com.yumesoftworks.fileshare.data.TextInfoSendObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class SenderSocketTransfer{
    private static final String TAG="SenderSocketTransfer";

    //actions
    private static final int ACTION_SEND_DETAIL=4001;
    private static final int ACTION_WAIT_BEFORE_SEND_FILE=4002;
    private static final int ACTION_SEND_FILE=4003;
    private static final int ACTION_FINISHED_FILE_TRANSFER=4004;
    private static final int ACTION_WAITING_FILE_SUCCESS=4005;
    private static final int ACTION_NEXT_ACTION=4006;
    private static final int ACTION_EXCEPTION=4007;

    //types of next actions
    public static final int NEXT_ACTION_CONTINUE=2001;
    public static final int NEXT_ACTION_CANCEL_SPACE =2002;

    //thread
    //private Handler socketHandler;
    private Thread socketThread;

    //socket
    private Socket mSocket;

    //data
    private String messageToSend;

    //socket info
    private String mIpAddress;
    private int mPort;
    private int mCurrentAction;
    private int mNextActionDetail;

    //data handling
    private FileListEntry mFileEntry;
    private int mCurrentFile;
    private int mTotalFiles;

    //interface
    private SenderSocketTransferInterface mSenderInterface;

    public SenderSocketTransfer(TransferFileCoordinatorHelper context, String recIpAddress, int recPort, FileListEntry recFile, int recCurrentFile, int recTotalFiles){
        mIpAddress=recIpAddress;
        mPort=recPort;
        mSenderInterface=(SenderSocketTransferInterface)context;
        mFileEntry =recFile;

        mCurrentFile=recCurrentFile;
        mTotalFiles=recTotalFiles;

        //socketHandler=new Handler(Looper.getMainLooper());
        socketThread=new Thread(new CommunicationThread());
        socketThread.start();
    }

    class CommunicationThread implements Runnable {

        @Override
        public void run() {
            Boolean doWeRepeat=true;//to retry if needed
            int totalSocketRetries=20;
            int currentSocketRetries=0;

            while (doWeRepeat && !socketThread.isInterrupted()) {
                // block the call until connection is created and return
                // Socket object
                try {
                    //wait for a connection
                    Log.d(TAG, "we try to create the socket: " + mIpAddress + " with port: " + mPort);

                    //vars
                    mSocket = new Socket(mIpAddress, mPort);
                    mSocket.setSoTimeout(3000);

                    Log.d(TAG, "Socket connected successfully Reading the user data");

                    //we reset the retries
                    currentSocketRetries = 0;

                    //we initialize the 1st action
                    mCurrentAction = ACTION_SEND_DETAIL;

                    //initialize streams
                    ObjectOutputStream messageOut = new ObjectOutputStream(mSocket.getOutputStream());
                    ObjectInputStream messageIn = new ObjectInputStream(mSocket.getInputStream());
                    InputStream fileInputStream;
                    OutputStream fileOutputStream = mSocket.getOutputStream();

                    do {
                        //we send the 1st file details
                        if (mCurrentAction == ACTION_SEND_DETAIL) {
                            //we send the details of the file
                            try {
                                //we send the file name
                                messageToSend = mFileEntry.getFileName();
                                String additionalInfo = String.valueOf(mCurrentFile) + "," +
                                        String.valueOf(mTotalFiles)+","+
                                        String.valueOf(new File(mFileEntry.getPath()).length());

                                TextInfoSendObject sendObject = new TextInfoSendObject(TransferProgressActivity.TYPE_FILE_DETAILS,
                                        messageToSend,
                                        additionalInfo);

                                messageOut.writeObject(sendObject);
                                messageOut.flush();
                                mCurrentAction = ACTION_WAIT_BEFORE_SEND_FILE;

                                //Log.d(TAG, "we send the file details "+additionalInfo);

                                //send to ui the current file to be sent
                                mSenderInterface.updateSendSendUI(sendObject);

                            } catch (Exception e) {
                                Log.d(TAG, "Error sending file details message: " + messageToSend + " " + e.getMessage());
                                mCurrentAction=ACTION_EXCEPTION;
                            }
                        }

                        //we send the file
                        if (mCurrentAction == ACTION_SEND_FILE) {
                            try {
                                //Log.d(TAG, "we start sending the file");
                                File file = new File(mFileEntry.getPath());

                                byte[] bytes = new byte[16 * 1024];
                                fileInputStream = new FileInputStream(file);
                                //Log.d(TAG, "File: getting the file input stream " + fileInputStream.toString());
                                //fileOutputStream = mSocket.getOutputStream();

                                //we send the file name
                                messageToSend = mFileEntry.getFileName();
                                String additionalInfo="";

                                //progress message
                                TextInfoSendObject objectUpdate=new TextInfoSendObject(TransferProgressActivity.TYPE_FILE_DETAILS,messageToSend,additionalInfo);

                                int count;
                                int byteCounter=0;
                                while ((count = fileInputStream.read(bytes)) > 0) {
                                    //Log.d(TAG, "File: reading the bytes " + count);
                                    fileOutputStream.write(bytes, 0, count);

                                    byteCounter+=bytes.length;

                                    //send progress update to UI
                                    additionalInfo= String.valueOf(mCurrentFile) + "," +
                                            String.valueOf(mTotalFiles)+","+
                                            String.valueOf(((FileInputStream) fileInputStream).getChannel().size())+","+
                                            String.valueOf(byteCounter);

                                    objectUpdate.setAdditionalInfo(additionalInfo);

                                    mSenderInterface.updateSendSendUI(objectUpdate);
                                    //Log.d(TAG, "File: wrote the bytes");
                                }

                                fileOutputStream.flush();
                                fileInputStream.close();
                                //Log.d(TAG, "File: flushed " + length);

                                fileOutputStream.close();

                                Log.d(TAG, "File sent");
                                mCurrentAction = ACTION_FINISHED_FILE_TRANSFER;
                            } catch (Exception e) {
                                Log.d(TAG, "There was en exception when sending file " + e.getMessage());
                                mCurrentAction=ACTION_EXCEPTION;
                            }
                        }

                        //we check if it is the last file
                        if (mCurrentAction == ACTION_FINISHED_FILE_TRANSFER) {
                            //we set the file as transferred in the database
                            mSenderInterface.updateSendSentFile(mFileEntry);

                            mNextActionDetail=NEXT_ACTION_CONTINUE;
                            mCurrentAction = ACTION_NEXT_ACTION;
                        }

                        //we check if it is waiting, we read the object
                        if (mCurrentAction == ACTION_WAITING_FILE_SUCCESS || mCurrentAction==ACTION_WAIT_BEFORE_SEND_FILE) {
                            //we read the object
                            try {
                                //messageIn = new ObjectInputStream(mSocket.getInputStream());
                                TextInfoSendObject message = (TextInfoSendObject) messageIn.readObject();

                                //we check if the message is the success of the file so we can continue with the next file
                                if (message.getMessageType() == TransferProgressActivity.TYPE_FILE_TRANSFER_SUCCESS) {
                                    //transfer is completed
                                    mCurrentAction = ACTION_NEXT_ACTION;
                                    Log.d(TAG, "the file has been transferred, we open a new socket");
                                }else if (message.getMessageType() == TransferProgressActivity.TYPE_FILE_DETAILS_SUCCESS) {
                                    //transfer is completed
                                    mCurrentAction = ACTION_SEND_FILE;
                                    Log.d(TAG, "the details have been received on the client, we send the bytes now");
                                }else if(message.getMessageType()==TransferProgressActivity.TYPE_FILE_TRANSFER_NO_SPACE){
                                    //we cannot complete transfer
                                    Log.d(TAG,"the transfer cannot be completed since the receiver ran our of space");
                                    //mCurrentAction
                                    mNextActionDetail=NEXT_ACTION_CANCEL_SPACE;
                                    mSenderInterface.finishedSendTransfer(mNextActionDetail);
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "Waiting client to communicate "+e.getMessage());
                            }
                        }
                    }while(mCurrentAction!=ACTION_NEXT_ACTION && mCurrentAction!=ACTION_EXCEPTION && !socketThread.isInterrupted());

                    //close the socket
                    if (!mSocket.isClosed()) {
                        try {
                            mSocket.close();
                            Log.e(TAG, "Socket closed");
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to close the socket");
                        }
                    }

                    if (mCurrentAction==ACTION_NEXT_ACTION) {
                        //we finish
                        doWeRepeat = false;
                        mSenderInterface.finishedSendTransfer(mNextActionDetail);
                    }else if(mCurrentAction==ACTION_EXCEPTION){
                        doWeRepeat=false;
                        mSenderInterface.socketErrorSend();
                        return;
                    }else{
                        doWeRepeat=false;
                        return;
                    }
                } catch (Exception e) {
                    Log.d(TAG, "the socket creation has failed, try again" + e.getMessage());
                    currentSocketRetries++;

                    if (currentSocketRetries==totalSocketRetries){
                        doWeRepeat=false;
                        mSenderInterface.socketErrorSend();
                        Log.d(TAG, "we ran out of tries for the socket");
                    }else{
                        //wait 1 second before trying again
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        }catch (InterruptedException exe){
                            Log.d(TAG,"couldn't interrupt "+exe.getMessage());
                        }
                    }
                }
            }
        }
    }

    public Boolean destroy(){
        Log.d(TAG, "Destroy sockets");

        try{
            mSocket.close();

            if (mSocket.isClosed()) {
                //socket closed
                mSenderInterface = null;
                socketThread.interrupt();

                Log.d(TAG, "Socket destroyed successfully");
                return true;
            }else{
                Log.d(TAG, "Cannot close socket after executing close()");
                return false;
            }
        }catch (Exception e){
            Log.d(TAG, "Cannot close socket " + e.getMessage());
            return false;
        }
    }

    //interface
    public interface SenderSocketTransferInterface {
        void updateSendSendUI(TextInfoSendObject textInfoSendObject);
        void updateSendSentFile(FileListEntry fileListEntry);
        void finishedSendTransfer(int typeFinishTransfer);
        void socketErrorSend();
    }
}