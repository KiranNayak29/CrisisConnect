package android.csulb.edu.crisisconnect.Services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by vaibhavjain on 4/2/2017
 */

public class UpdateService extends Service {

    public static final String ACTION_UPDATE_BROADCAST = "updateViews";
    public static final String EXTRA_CLIENT_LIST = "clientList";
    private static final String TAG = "UpdateService";
    public static ServerSocket sSocket;
    boolean runForever = true;

    public UpdateService() {
        super();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    //TODO: Not a good idea to use a hard coded port. Will move to next available port next.
                    sSocket = new ServerSocket(50001);
                    //We will let the service to run indefinitely and will only interrupt/stop when we are exiting the app.
                    while (runForever) {
                        Socket client = sSocket.accept();
                        InputStream iStream = client.getInputStream();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] content = new byte[2048];
                        int bytesRead = -1;
                        while ((bytesRead = iStream.read(content)) != -1) {
                            baos.write(content, 0, bytesRead);
                        }
                        Intent updateIntent = new Intent(ACTION_UPDATE_BROADCAST);
                        updateIntent.putExtra(EXTRA_CLIENT_LIST, content);
                        LocalBroadcastManager.getInstance(UpdateService.this).sendBroadcast(updateIntent);
                        client.close();
                        //The following functionality lets others to use Thread.interrupt() to stop this service.
                        if (Thread.interrupted()) {
                            runForever = false;
                        }
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroying Service");
        super.onDestroy();
        try {
            sSocket.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
