package android.csulb.edu.crisisconnect;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.csulb.edu.crisisconnect.database.ChatHistoryContract.Messages;
import android.csulb.edu.crisisconnect.database.MessageHistoryDbHelper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private static final int REQ_CAMERA_CAPTURE = 0x09;
    Socket socket = null;
    EditText messageText;
    Button sendButton;
    ListView messageListView;
    ChatMessageAdapter messageAdapter;
    File currentImage = null;
    private String ipAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ipAddress = getIntent().getStringExtra(Constants.IP_ADDRESS);
        setTitle(ipAddress);
        setContentView(R.layout.activity_chat);

        messageText = (EditText) findViewById(R.id.chat_message_text);
        sendButton = (Button) findViewById(R.id.send_button);
        messageListView = (ListView) findViewById(R.id.chat_list_view);
        messageListView.setDivider(null);
        populateHistory();
        //We can be sure that there will always be a socket of current ipAddress present in SocketHolder.
        //That is because we cannot open ChatActivity unless there is a reliable connection established.
        socket = SocketHolder.getSocket(ipAddress);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String message = messageText.getText().toString();
                if (message.trim().length() > 0) {
                    AsyncTask<Object, Object, Object> sendWriteRequest = new AsyncTask<Object, Object, Object>() {

                        @Override
                        protected Object doInBackground(Object[] params) {
                            try {
                                OutputStream outputStream = socket.getOutputStream();
                                if (outputStream != null) {
                                    byte[] messageBytes = message.getBytes();
                                    ByteBuffer tempBuffer = ByteBuffer.allocate(messageBytes.length + 8);
                                    //Put 1 for indicating text message
                                    tempBuffer.putInt(Constants.MESSAGE_TYPE_TEXT);
                                    //Put the size of packets.
                                    tempBuffer.putInt(messageBytes.length);
                                    tempBuffer.put(messageBytes);
                                    Log.d(TAG, "Sending write request " + tempBuffer.toString());
                                    outputStream.write(tempBuffer.array());
                                } else {
                                    Log.e(TAG, "Unable to send data. Socket output stream is null");
                                }
                            } catch (IOException ioe) {
                                Log.e(TAG, "An exception occurred", ioe);
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Object o) {
                            messageText.setText("");
                            //Insert it into our db as well so that we can show it on our own listview that we have
                            //sent a message
                            insertIntoDb(socket, message, Constants.MESSAGE_TYPE_TEXT);
                        }
                    };
                    sendWriteRequest.execute();
                }
            }
        });
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                populateHistory();
            }
        }, new IntentFilter(Constants.BROADCAST_UPDATE_CHAT));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        ;
        inflater.inflate(R.menu.chat_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.send_image:
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                    File saveFile = null;
                    try {
                        saveFile = createImageFile();
                    } catch (IOException ioe) {
                        Log.e(TAG, "Unable to save the file", ioe);
                    }
                    if (saveFile != null) {
                        Uri photoUri = FileProvider.getUriForFile(this,
                                "saveimage.fileprovider",
                                saveFile);
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                        startActivityForResult(cameraIntent, REQ_CAMERA_CAPTURE);
                    }
                }
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_CAMERA_CAPTURE && resultCode == RESULT_OK) {
            AsyncTask<Object, Object, Object> sendWriteRequest = new AsyncTask<Object, Object, Object>() {

                @Override
                protected Object doInBackground(Object[] params) {
                    try {
                        FileInputStream fis = new FileInputStream(currentImage);
                        byte[] imageContents = new byte[(int) currentImage.length()];
                        fis.read(imageContents, 0, ((int) currentImage.length()));
                        fis.close();
                        //Log.d(TAG, "Contents of file: " + new String(imageContents, Charset.defaultCharset()));
                        OutputStream outputStream = socket.getOutputStream();
                        if (outputStream != null) {
                            ByteBuffer tempBuffer = ByteBuffer.allocate(imageContents.length + 8);
                            //Put 1 for indicating text message
                            tempBuffer.putInt(Constants.MESSAGE_TYPE_IMAGE);
                            //Put the size of packets.
                            tempBuffer.putInt(imageContents.length);
                            tempBuffer.put(imageContents);
                            Log.d(TAG, "Sending write request " + imageContents.length);
                            outputStream.write(tempBuffer.array());
                        } else {
                            Log.e(TAG, "Unable to send data. Socket output stream is null");
                        }
                    } catch (IOException ioe) {
                        Log.e(TAG, "An exception occurred", ioe);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Object o) {
                    //Insert it into our db as well so that we can show it on our own listview that we have
                    //sent a message
                    insertIntoDb(socket, currentImage.getAbsolutePath(), Constants.MESSAGE_TYPE_IMAGE);
                }
            };
            sendWriteRequest.execute();
        }
    }

    public File createImageFile() throws IOException {
        String imageFileName = String.valueOf(System.currentTimeMillis());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        currentImage = image;
        return image;
    }

    public void populateHistory() {
        SQLiteDatabase db = MessageHistoryDbHelper.getDBConnection();

        String[] projection = {
                Messages._ID,
                Messages.COLUMN_NAME_SENDER,
                Messages.COLUMN_NAME_RECEIVER,
                Messages.COLUMN_NAME_MESSAGE,
                Messages.COLUMN_NAME_MESSAGE_TYPE,
                Messages.COLUMN_NAME_LATITUDE,
                Messages.COLUMN_NAME_LONGITUDE
        };

        String selection = Messages.COLUMN_NAME_RECEIVER + " =? OR " + Messages.COLUMN_NAME_SENDER + " =?";
        String[] selectionArgs = {ipAddress, ipAddress};
        Cursor cursor = db.query(Messages.TABLE_NAME, projection, selection, selectionArgs, null, null, null);
        Log.d(TAG, "Messages to populate: " + cursor.getCount());
        messageAdapter = new ChatMessageAdapter(ChatActivity.this, cursor);
        messageListView.setAdapter(messageAdapter);
    }

    public void insertIntoDb(Socket socket, String message, int messageType) {
        SQLiteDatabase db = MessageHistoryDbHelper.getDBConnection();
        ContentValues values = new ContentValues();
        values.put(Messages.COLUMN_NAME_SENDER, socket.getInetAddress().getHostAddress());
        values.put(Messages.COLUMN_NAME_MESSAGE_TYPE, messageType);
        values.put(Messages.COLUMN_NAME_MESSAGE, message);
        db.insert(Messages.TABLE_NAME, null, values);
        //Reload messages
        populateHistory();
    }
}
