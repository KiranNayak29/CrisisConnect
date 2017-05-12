package android.csulb.edu.crisisconnect;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.csulb.edu.crisisconnect.database.ChatHistoryContract.Messages;
import android.csulb.edu.crisisconnect.database.MessageHistoryDbHelper;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by vaibhavjain on 5/8/2017
 * This thread is started when the server thread accepts an incoming chat request from a ClientThread.
 */
class ConnectedClientThread extends Thread {
    private static final String TAG = "ConnectedClientThread";
    InputStream inputStream = null;
    Context ctx = null;
    private Socket clientSocket = null;

    ConnectedClientThread(Context ctx, Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.ctx = ctx;
    }

    @Override
    public void run() {
        Log.i(TAG, "Client connected. Starting a new connected client thread.");
        try {
            inputStream = clientSocket.getInputStream();
        } catch (IOException ioe) {
            Log.e(TAG, "Unable to get input stream", ioe);
        }
        byte[] bufferData = new byte[16384];
        int datatype = 0;
        int numOfPackets = 0;
        double latitude = 0;
        double longitude = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while (!this.isInterrupted()) {
            try {
                int numOfBytes = inputStream.read(bufferData);
                if (numOfBytes <= 0) {
                    continue;
                }
                //bos.write(bufferData);
                //Log.d(TAG, "Size: " + bos.size());
                //Log.d(TAG, "Receiving: " + new String(bufferData, Charset.defaultCharset()) + "\n" + "size: " + numOfBytes);
                byte[] trimmedBuffer = Arrays.copyOf(bufferData, numOfBytes);
                bufferData = new byte[16384];
                ByteBuffer tempByteBuffer = ByteBuffer.wrap(trimmedBuffer);
                if (datatype == 0) {
                    datatype = tempByteBuffer.getInt();
                    Log.d(TAG, "Datatype: " + datatype);
                }
                if (numOfPackets == 0) {
                    numOfPackets = tempByteBuffer.getInt();
                    Log.d(TAG, "Number of packets to receive: " + numOfPackets);
                }
                if (datatype == Constants.MESSAGE_TYPE_IMAGE && latitude == 0 && longitude == 0) {
                    latitude = tempByteBuffer.getDouble();
                    longitude = tempByteBuffer.getDouble();
                }
                byte[] dst = new byte[tempByteBuffer.remaining()];
                tempByteBuffer.get(dst);
                bos.write(dst);
                if (bos.size() == numOfPackets) {
                    //Textual data
                    if (datatype == 1) {
                        SQLiteDatabase db = MessageHistoryDbHelper.getDBConnection();
                        ContentValues values = new ContentValues();
                        values.put(Messages.COLUMN_NAME_RECEIVER, clientSocket.getInetAddress().getHostAddress());
                        values.put(Messages.COLUMN_NAME_MESSAGE_TYPE, Constants.MESSAGE_TYPE_TEXT);
                        values.put(Messages.COLUMN_NAME_MESSAGE, bos.toString());
                        db.insert(Messages.TABLE_NAME, null, values);
                        Intent updateChat = new Intent(Constants.BROADCAST_UPDATE_CHAT);
                        LocalBroadcastManager.getInstance(ctx).sendBroadcast(updateChat);
                        datatype = 0;
                        numOfPackets = 0;
                        bos = new ByteArrayOutputStream();
                    } else if (datatype == 2) {
                        File imageFile = createImageFile();
                        BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(imageFile));
                        outStream.write(bos.toByteArray());
                        outStream.flush();
                        outStream.close();

                        SQLiteDatabase db = MessageHistoryDbHelper.getDBConnection();
                        ContentValues values = new ContentValues();
                        values.put(Messages.COLUMN_NAME_RECEIVER, clientSocket.getInetAddress().getHostAddress());
                        values.put(Messages.COLUMN_NAME_MESSAGE_TYPE, Constants.MESSAGE_TYPE_IMAGE);
                        values.put(Messages.COLUMN_NAME_MESSAGE, imageFile.getAbsolutePath());
                        values.put(Messages.COLUMN_NAME_LATITUDE, latitude);
                        values.put(Messages.COLUMN_NAME_LONGITUDE, longitude);
                        db.insert(Messages.TABLE_NAME, null, values);
                        Intent updateChat = new Intent(Constants.BROADCAST_UPDATE_CHAT);
                        LocalBroadcastManager.getInstance(ctx).sendBroadcast(updateChat);
                        datatype = 0;
                        numOfPackets = 0;
                        bos = new ByteArrayOutputStream();
                    }
                }
            } catch (IOException ioe) {
                Log.e(TAG, "Exception occurred", ioe);
            }
        }
    }

    public void close() {
        this.interrupt();
        try {
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Unable to close connected client thread", ioe);
        }
    }

    public File createImageFile() throws IOException {
        String imageFileName = String.valueOf(System.currentTimeMillis());
        File storageDir = ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        return image;
    }
}
