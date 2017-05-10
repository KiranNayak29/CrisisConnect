package android.csulb.edu.crisisconnect;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;

/**
 * Created by vaibhavjain on 5/8/2017
 */

public class SocketHolder {
    private static final String TAG = "SocketHolder";
    private static HashMap<String, Socket> socketMap = new HashMap<>();

    public static Socket getSocket(String ipAddress) {
        return (socketMap.containsKey(ipAddress) ? socketMap.get(ipAddress) : null);
    }

    public static void saveSocket(Socket socket) {
        Log.d(TAG, "Saved in Holder: " + socket.getInetAddress().getHostAddress());
        socketMap.put(socket.getInetAddress().getHostAddress(), socket);
    }

    public static void createASocketConnection(final String ipAddress, final FinishSocketCreateListener finishSocketCreateListener) {
        final Socket socket = new Socket();
        AsyncTask<Object, Integer, Object> createTask = new AsyncTask<Object, Integer, Object>() {

            @Override
            protected Object doInBackground(Object[] params) {
                try {
                    socket.bind(null);
                    Log.d(TAG, "Connecting to " + ipAddress);
                    socket.connect(new InetSocketAddress(ipAddress, 43000));
                    Log.d(TAG, "Connected");
                    saveSocket(socket);
                } catch (IOException ioe) {
                    Log.e(TAG, "Unable to create a new socket connection", ioe);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable mainRunnable = new Runnable() {

                    @Override
                    public void run() {
                        finishSocketCreateListener.onFinishSocketCreate();
                    }

                };
                mainHandler.post(mainRunnable);
            }
        };
        createTask.execute();
    }
}
