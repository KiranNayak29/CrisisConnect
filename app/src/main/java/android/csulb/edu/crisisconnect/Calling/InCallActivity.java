package android.csulb.edu.crisisconnect.Calling;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.csulb.edu.crisisconnect.R;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
import com.koushikdutta.async.http.body.JSONObjectBody;

import net.majorkernelpanic.streaming.SessionBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class InCallActivity extends AppCompatActivity implements MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnInfoListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener {
    private String otherIP,otherUserName,startTimestamp;
    private MediaPlayer mp;
    private ImageView circle2;
    private CountDownTimer cTimer;
    private int secsElapsed=0;
    private TextView minsTV,secsTV;
    private Animation a;
    private boolean outgoing;

    private static final String TAG="InCallActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_call);
        //save start time stamp to store in database later
        long now = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, KK:mm a");
        startTimestamp = sdf.format(new Date(now));

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        otherIP=getIntent().getStringExtra(Util.KEY_OTHER_IP);
        otherUserName=getIntent().getStringExtra(Util.KEY_OTHER_USERNAME);
        outgoing=getIntent().getBooleanExtra(Util.KEY_OUTGOING, true);
                ((TextView) findViewById(R.id.inCallTargetUsername)).setText(otherUserName);
        minsTV= (TextView) findViewById(R.id.inCallMins);
        secsTV= (TextView) findViewById(R.id.inCallSecs);

        ((Button)findViewById(R.id.endCallButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {   //send end call post to other ip
                String otherIPFullHTTP = Util.PROTOCOL_HTTP + otherIP + ":" + Util.HTTP_PORT;
                JSONObject requestJSON = new JSONObject();
                try {
                    requestJSON.put(Util.KEY_OPERATION, Util.OPERATION_TYPE_END_CALL);
                    // Not really required: in the service, we can get this ip anyway: requestJSON.put(Util.KEY_CALLER_IP,myIP);   //while sending caller's ip,send it raw(without http and port no) - its the receivers responsibility to handle a raw ip

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                AsyncHttpRequest req = new AsyncHttpPost(otherIPFullHTTP);

                AsyncHttpRequestBody body = new JSONObjectBody(requestJSON);
                req.setBody(body);
                AsyncHttpClient.getDefaultInstance().executeJSONObject(req, null);
                if(mp!=null)
                    mp.stop();
                if(cTimer!=null)
                    cTimer.cancel();
                if(a!=null)
                    a.cancel();
                finish();
            }
        });

        //we're here which means a call was accepted; connect to the other ip!
        mp=new MediaPlayer();
        mp.setOnBufferingUpdateListener(this);
        mp.setOnCompletionListener(this);
        mp.setOnErrorListener(this);
        mp.setOnInfoListener(this);
        mp.setOnPreparedListener(this);
        if(SessionBuilder.getInstance()==null) {
            Log.e(TAG,"Sessionbuilder is null!");
        }

        try {
            mp.setDataSource(Util.PROTOCOL_RTSP+otherIP+":"+Util.RTSP_PORT);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mp.prepareAsync();

        //show animation
        circle2= (ImageView) findViewById(R.id.incallOuterRing);
        //from http://stackoverflow.com/questions/3805622/drawable-rotating-around-its-center-android
        a = new RotateAnimation(0.0f, 360.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        a.setRepeatCount(-1);
        a.setDuration(1000);
        a.setInterpolator(new FastOutSlowInInterpolator());
        circle2.setAnimation(a);

        //timer time
                cTimer=new CountDownTimer(Long.MAX_VALUE,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                secsElapsed++;
                int mins=(secsElapsed/60);
                int secs=secsElapsed-(mins*60);
                minsTV.setText(String.valueOf(mins));
                secsTV.setText(String.valueOf(secs));
//                circle2.setProgress(progress);
            }

            @Override
            public void onFinish() {
            }
        }.start();
    }


    @Override
    public void onPrepared(MediaPlayer arg0) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onPrepared");
        mp.start();

        for(MediaPlayer.TrackInfo t : mp.getTrackInfo()) {
            Log.d(TAG, t.toString());
        }
    }

    @Override
    public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onError"+arg1+arg2);
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onInfo"+arg1+arg2);
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer arg0) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onCompletion");

    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onBufferingUpdate"+percent);
    }


    @Override
    protected void onStop() {
        //ask a confirmtaion if the call should be disconnected
        //if yes, send the end call post to the other ip

        if(mp!=null)
            mp.stop();
        if(cTimer!=null)
            cTimer.cancel();
        if(a!=null)
            a.cancel();

        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter=new IntentFilter(Util.INTENT_FILTER_SERVICE_ACTIVITY);
        LocalBroadcastManager.getInstance(this).registerReceiver(incomingCallBroadcastReceiver, filter);
    }

    @Override
    protected void onPause() {
     LocalBroadcastManager.getInstance(this).unregisterReceiver(incomingCallBroadcastReceiver);
        super.onPause();
    }



    private BroadcastReceiver incomingCallBroadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            final String otherIP=intent.getStringExtra(Util.KEY_OTHER_IP);
            switch(intent.getIntExtra(Util.KEY_INTENT_FILTER_REASON, Util.INTENT_FILTER_REASON_NO_REASON)) {
                //no other type of broadcast will be sent to us whle we're in a call
                case Util.OPERATION_TYPE_END_CALL:
                    Toast.makeText(InCallActivity.this, "The other party ended the call", Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                default:
                    Log.d(TAG, "Invalid int extra received via broadcast!");
                    break;
            }
        }
    };

}
