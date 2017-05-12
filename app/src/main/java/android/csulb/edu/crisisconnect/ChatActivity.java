package android.csulb.edu.crisisconnect;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.csulb.edu.crisisconnect.database.ChatHistoryContract.Messages;
import android.csulb.edu.crisisconnect.database.MessageHistoryDbHelper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

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
    ImageView fullscreenImage;
    ChatMessageAdapter messageAdapter;
    File currentImage = null;
    private String ipAddress;
    LocationManager locManager = null;
    LocationListener locationListener = null;
    Cursor historyCursor = null;
    double latitude;
    double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ipAddress = getIntent().getStringExtra(Constants.IP_ADDRESS);
        setTitle(ipAddress);
        setContentView(R.layout.activity_chat);

        messageText = (EditText) findViewById(R.id.chat_message_text);
        sendButton = (Button) findViewById(R.id.send_button);
        fullscreenImage = (ImageView) findViewById(R.id.fullscreen_image);
        messageListView = (ListView) findViewById(R.id.chat_list_view);
        messageListView.setDivider(null);
        registerForContextMenu(messageListView);
        populateHistory();

        messageListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor dataCursor = (Cursor) parent.getItemAtPosition(position);
                dataCursor.moveToPosition(position);
                int datatype = dataCursor.getInt(dataCursor.getColumnIndex(Messages.COLUMN_NAME_MESSAGE_TYPE));
                if(datatype == 2) {
                    String pathToImage = dataCursor.getString(dataCursor.getColumnIndex(Messages.COLUMN_NAME_MESSAGE));
                    Glide.with(ChatActivity.this).load(new File(pathToImage)).into(fullscreenImage);
                    fullscreenImage.setBackgroundColor(Color.BLACK);
                    sendButton.setVisibility(View.INVISIBLE);
                    fullscreenImage.setVisibility(View.VISIBLE);
                }
            }
        });

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
                            insertIntoDb(socket, message, Constants.MESSAGE_TYPE_TEXT, null, null);
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
                locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (!locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    buildAlertMessageNoGps();
                }
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

            final AsyncTask<Object, Object, Object> sendWriteRequest = new AsyncTask<Object, Object, Object>() {

                @Override
                protected Object doInBackground(Object[] params) {
                    try {
                        FileInputStream fis = new FileInputStream(currentImage);
                        byte[] imageContents = new byte[(int) currentImage.length()];
                        fis.read(imageContents, 0, ((int) currentImage.length()));
                        fis.close();
                        OutputStream outputStream = socket.getOutputStream();
                        if (outputStream != null) {
                            ByteBuffer tempBuffer = ByteBuffer.allocate(imageContents.length + 24);
                            //Put 1 for indicating text message
                            tempBuffer.putInt(Constants.MESSAGE_TYPE_IMAGE);
                            //Put the size of packets.
                            tempBuffer.putInt(imageContents.length);
                            tempBuffer.putDouble(latitude);
                            tempBuffer.putDouble(longitude);
                            tempBuffer.put(imageContents);
                            Log.d(TAG, "Sending write request for " + imageContents.length + " bytes");
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
                    insertIntoDb(socket,
                            currentImage.getAbsolutePath(),
                            Constants.MESSAGE_TYPE_IMAGE,
                            String.valueOf(latitude),
                            String.valueOf(longitude));
                }
            };

            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    Log.d(TAG, "Latitude and Longitude before inserting: " + latitude + " " + longitude);
                    sendWriteRequest.execute();
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {}
            };

            if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                if(locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null);
                } else if(locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
                }
            } else{
                Toast.makeText(ChatActivity.this, "Insufficient permissions", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Insufficient permissions");
            }
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
        historyCursor = db.query(Messages.TABLE_NAME, projection, selection, selectionArgs, null, null, null);
        Log.d(TAG, "Messages to populate: " + historyCursor.getCount());
        messageAdapter = new ChatMessageAdapter(ChatActivity.this, historyCursor);
        messageListView.setAdapter(messageAdapter);
    }

    public void insertIntoDb(Socket socket, String message, int messageType, String latitude, String longitude) {
        SQLiteDatabase db = MessageHistoryDbHelper.getDBConnection();
        ContentValues values = new ContentValues();
        values.put(Messages.COLUMN_NAME_SENDER, socket.getInetAddress().getHostAddress());
        values.put(Messages.COLUMN_NAME_MESSAGE_TYPE, messageType);
        values.put(Messages.COLUMN_NAME_MESSAGE, message);
        values.put(Messages.COLUMN_NAME_LATITUDE, latitude);
        values.put(Messages.COLUMN_NAME_LONGITUDE, longitude);
        db.insert(Messages.TABLE_NAME, null, values);
        //Reload messages
        populateHistory();
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if(v.getId() == R.id.chat_list_view) {
            MenuItem item1 = menu.add(Menu.NONE, 1, Menu.NONE, "Get Location");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Cursor cursor = historyCursor;
        cursor.moveToPosition(info.position);
        String lat = cursor.getString(cursor.getColumnIndex(Messages.COLUMN_NAME_LATITUDE));
        String longi = cursor.getString(cursor.getColumnIndex(Messages.COLUMN_NAME_LONGITUDE));;
        Log.d(TAG, "Latitude and Longitude while quering: " + lat + " " + longi);
        if(lat != null && longi != null) {
            Intent mapIntent = new Intent(Intent.ACTION_VIEW);
            mapIntent.setData(Uri.parse("geo:0,0?q=" + lat + "," + longi));
            startActivity(mapIntent);
        } else{
            Log.d(TAG, "Latitude and Longitude not available");
            Toast.makeText(ChatActivity.this, "Latitude and Longitude not available", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (fullscreenImage.getVisibility() == View.VISIBLE) {
            fullscreenImage.setImageDrawable(null);
            sendButton.setVisibility(View.VISIBLE);
            fullscreenImage.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }
}
