package android.csulb.edu.crisisconnect.Calling;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.csulb.edu.crisisconnect.ClientAdapter;
import android.csulb.edu.crisisconnect.MainActivity;
import android.csulb.edu.crisisconnect.R;
import android.csulb.edu.crisisconnect.WifiHotspotApis.ClientScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
//import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
import com.koushikdutta.async.http.body.JSONObjectBody;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.rtsp.RtspServer;
import android.csulb.edu.crisisconnect.WifiHotspotApis.WifiApManager;
import android.csulb.edu.crisisconnect.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class LandingActivity extends AppCompatActivity {
    private static String myIP;
    private Session mSession;
    private ListView listView;
    private ClientAdapter clientadapter;
    private static final String TAG="LandingActivity";
    private ProgressDialog pDialog;
    private String otherUsernameWhenWeCall;
    private int callState;
    private static final int CALL_STATE_IN_CALL=1;
    private static final int CALL_STATE_AVAILABLE=2;
    //drawer
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mDrawerList;
    WifiApManager wifiApManager;
    MainActivity mains;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);
        setupSlidingDrawer();
        callState=CALL_STATE_AVAILABLE;
        ListView listView= (ListView) findViewById(R.id.landingListView);

        wifiApManager = new WifiApManager(this);
         ClientAdapter clientadapter = new ClientAdapter(LandingActivity.this,wifiApManager.results);
        listView.setAdapter(clientadapter);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        String myUsername=prefs.getString(Util.KEY_PREFS_EMAIL,"user@gmail.com");
        ((TextView)findViewById(R.id.drawerEmail)).setText(myUsername);
        //adapter=new HomeAdapter(this,R.layout.main_listview_row,DatabaseHelper.getInstance(this).getAllContacts());
        //listView.setAdapter(adapter);
        listView.setDivider(null);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //show alertdialog with either history or call
                ClientScanResult a = wifiApManager.results.get(position);
                //otherUsernameWhenWeCall=((Contact) adapter.getItem(position)).getUsername();
                otherUsernameWhenWeCall = a.getHWAddr();
                //final String rawIP = ((Contact) adapter.getItem(position)).getIp();
                final String rawIP = a.getIpAddr();
                new AlertDialog.Builder(LandingActivity.this)
                        .setTitle(otherUsernameWhenWeCall)
//                        .setMessage("Do you want to accept a call from " + otherUsername)
                        .setPositiveButton("Call", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //Call selected
                                pDialog = new ProgressDialog(LandingActivity.this);
                                pDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "End", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        callState = CALL_STATE_AVAILABLE;
                                        pDialog.dismiss();
                                    }
                                });
                                pDialog.setIndeterminate(true);
                                pDialog.setMessage("Calling " + otherUsernameWhenWeCall);

                                pDialog.setCancelable(false);
                                pDialog.show();

                                performCall(rawIP);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                /*Intent i=new Intent(LandingActivity.this,HistoryActivity.class);
                                i.putExtra(Util.KEY_OTHER_USERNAME,otherUsernameWhenWeCall);
                                startActivity(i);*/
                                Toast.makeText(LandingActivity.this, "Canceled", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();


            }
        });
        //add new contact:


        //save my ip
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        myIP= Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        if(myIP!=null)
            ((TextView)findViewById(R.id.landingTextView)).setText("Your IP address: " + myIP);

        //button listener
        ((Button)findViewById(R.id.callButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //request the other partner for a call
                //create json request body
                Toast.makeText(LandingActivity.this, "Disabled", Toast.LENGTH_SHORT).show();

            }
        });

        //start HTTP server
        startService(new Intent(LandingActivity.this, HTTPServerService.class));

//        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
//        editor.putString(RtspServer.KEY_PORT, String.valueOf(Util.RTSP_PORT));
//        editor.commit();

        //configure SessionBuilder
        AudioQuality quality = new AudioQuality(8000,44100);
        mSession = SessionBuilder.getInstance()
                .setPreviewOrientation(90)
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_AAC)
                .setVideoEncoder(SessionBuilder.VIDEO_NONE)

                .setAudioQuality(AudioQuality.DEFAULT_AUDIO_QUALITY)
                .build();//Set audio quality? 8000,32 is default (8000 sampling,32000 bitrate bits/sec)
        //03-28 14:57:35.768: I/ACodec(5598): sampleRate=8000 channels=1 bits=16 (bitrate 32000?)
//        mSession.getAudioTrack().setStreamingMethod(MediaStream.MODE_MEDIARECORDER_API);

        //start RTSP server
        this.startService(new Intent(this, RtspServer.class));

    }
    //TODO: use SetContacts on the adapter in either onresume or in onstart..
    private void performCall(String rawIP) {
        callState=CALL_STATE_IN_CALL;
        //build a proper ip address
        String calleeIPFullHTTP = Util.PROTOCOL_HTTP + rawIP + ":" + Util.HTTP_PORT;
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);

        String myUsername=prefs.getString(Util.KEY_PREFS_USERNAME,rawIP);
        JSONObject requestJSON = new JSONObject();
        try {
            requestJSON.put(Util.KEY_OPERATION, Util.OPERATION_TYPE_REQUEST_CALL);
            requestJSON.put(Util.KEY_OTHER_USERNAME,myUsername);
            // Not really required: in the service, we can get this ip anyway: requestJSON.put(Util.KEY_CALLER_IP,myIP);   //while sending caller's ip,send it raw(without http and port no) - its the receivers responsibility to handle a raw ip

        } catch (JSONException e) {
            e.printStackTrace();
        }


        AsyncHttpRequest req = new AsyncHttpPost(calleeIPFullHTTP);

        AsyncHttpRequestBody body = new JSONObjectBody(requestJSON);
        req.setBody(body);
        AsyncHttpClient.getDefaultInstance().executeJSONObject(req, null);
        //show ui showing that a call is being attempted
        Toast.makeText(LandingActivity.this, "Calling the other party", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter=new IntentFilter(Util.INTENT_FILTER_SERVICE_ACTIVITY);
        LocalBroadcastManager.getInstance(this).registerReceiver(incomingCallBroadcastReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(incomingCallBroadcastReceiver);

    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        Log.d(TAG, "Destroying session and stopping RTSP server");
//        if(mSession!=null)
//            mSession.stop();
        stopService(new Intent(this, RtspServer.class));
        super.onDestroy();
    }

    private BroadcastReceiver incomingCallBroadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            final String otherIP=intent.getStringExtra(Util.KEY_OTHER_IP);


            switch(intent.getIntExtra(Util.KEY_INTENT_FILTER_REASON, Util.INTENT_FILTER_REASON_NO_REASON)) {
                case Util.INTENT_FILTER_REASON_NEW_INCOMING_CALL:
                    //show alert showing if user wants to accept or not
                    final String otherUsername=intent.getStringExtra(Util.KEY_INTENT_FILTER_OTHER_USERNAME);
                    new AlertDialog.Builder(LandingActivity.this)
                            .setTitle("Incoming call")
                            .setMessage("Do you want to accept a call from " + otherUsername)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //call accepted. Post to the caller, start mediaplayer listening and go to in call ui
                                    JSONObject acceptJSON=new JSONObject();
                                    try {
                                        acceptJSON.put(Util.KEY_OPERATION,Util.OPERATION_TYPE_ACCEPT_CALL);

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    String callerIPFullHTTP=Util.PROTOCOL_HTTP+otherIP+":"+Util.HTTP_PORT;
                                    AsyncHttpRequest req = new AsyncHttpPost(callerIPFullHTTP);
                                    AsyncHttpRequestBody body = new JSONObjectBody(acceptJSON);
                                    req.setBody(body);
                                    AsyncHttpClient.getDefaultInstance().executeJSONObject(req, null);

                                    Intent i=new Intent(LandingActivity.this,InCallActivity.class);
                                    i.putExtra(Util.KEY_OTHER_IP, otherIP);
                                    i.putExtra(Util.KEY_OTHER_USERNAME, otherUsername);
                                    i.putExtra(Util.KEY_OUTGOING, false);
                                    startActivity(i);
                                    callState=CALL_STATE_IN_CALL;

//                                    Toast.makeText(LandingActivity.this, "You accepted the call", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //call accepted. Post to the caller, start mediaplayer listening and go to in call ui
                                    JSONObject rejectJSON=new JSONObject();
                                    try {
                                        rejectJSON.put(Util.KEY_OPERATION, Util.OPERATION_TYPE_REJECT_CALL);

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    String callerIPFullHTTP=Util.PROTOCOL_HTTP+otherIP+":"+Util.HTTP_PORT;
                                    AsyncHttpRequest req = new AsyncHttpPost(callerIPFullHTTP);
                                    AsyncHttpRequestBody body = new JSONObjectBody(rejectJSON);
                                    req.setBody(body);
                                    AsyncHttpClient.getDefaultInstance().executeJSONObject(req, null);

//                                    Toast.makeText(LandingActivity.this, "You rejected the call", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .show();
                    break;

                case Util.INTENT_FILTER_REASON_CALL_ACCEPTED:   //other party accepted our call
                    callState=CALL_STATE_IN_CALL;
                    if(pDialog!=null)
                        pDialog.dismiss();
                    Toast.makeText(LandingActivity.this,"Your call has been accepted", Toast.LENGTH_SHORT).show();
                    Intent i=new Intent(LandingActivity.this,InCallActivity.class);
                    i.putExtra(Util.KEY_OTHER_IP, otherIP);
                    i.putExtra(Util.KEY_OTHER_USERNAME, otherUsernameWhenWeCall);
                    i.putExtra(Util.KEY_OUTGOING, true);
                    startActivity(i);
                    break;

                case Util.INTENT_FILTER_REASON_CALL_REJECTED:   //other party rejected our call
                    callState=CALL_STATE_AVAILABLE;
                    if(pDialog!=null)
                        pDialog.dismiss();
                    Toast.makeText(LandingActivity.this,otherUsernameWhenWeCall+" rejected your call", Toast.LENGTH_SHORT).show();
                    break;

                //Util.OPERATION_TYPE_END_CALL: This case will never be handled here (since we ll always be in the IN Call UI when we receive this
                default:
                    Log.d(TAG,"Invalid int extra received via broadcast!");
                    break;
            }
        }
    };
    private void setupSlidingDrawer() {

        mDrawerLayout= (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList= (ListView) findViewById(R.id.left_drawer_listview);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout
                , R.string.drawer_open_string,R.string.drawer_close_string) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);

                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.addDrawerListener(mDrawerToggle);
       /* ArrayList<DrawerItem> drawerItems=new ArrayList<DrawerItem>();
        drawerItems.add(new DrawerItem("Your Profile",R.drawable.ic_profile));
        drawerItems.add(new DrawerItem("Chats",R.drawable.ic_chats));
        drawerItems.add(new DrawerItem("Contacts",R.drawable.ic_contacts));
        drawerItems.add(new DrawerItem("Notification Settings",R.drawable.ic_settings));
        DrawerAdapter d=new DrawerAdapter(this,R.layout.left_drawer_listview_row,drawerItems);
        mDrawerList.setAdapter(d);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
*/

    }
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if(mDrawerLayout.isDrawerOpen(GravityCompat.START))
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                else
                    mDrawerLayout.openDrawer(GravityCompat.START);  // OPEN DRAWER

                return true;



        }
        return super.onOptionsItemSelected(item);
    }
}
