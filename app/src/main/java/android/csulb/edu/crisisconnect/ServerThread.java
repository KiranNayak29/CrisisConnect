package android.csulb.edu.crisisconnect;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by vaibhavjain on 5/7/2017
 * A thread that listens for incoming chat requests until interrupted (threads are interrupted when application is exiting).
 * Once a client request is accepted, the client socket is then passed to ConnectedClientThread so that server can start
 * listening to other requests
 */

public class ServerThread extends Thread {
    private static final String TAG = "ServerThread";
    private static final int PORT = 43000;
    private ServerSocket serverSocket = null;
    private Context ctx;
    private ArrayList<ConnectedClientThread> allClientThreadHandles = new ArrayList<>();

    public ServerThread(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void run() {
        Log.i(TAG, "Starting Server thread for exchanging text data");
        try {
            serverSocket = new ServerSocket(PORT);
            Log.i(TAG, "Started server on port " + PORT);
            Log.e(TAG, "Local address server thread: " + serverSocket);
            while (!this.isInterrupted()) {
                Socket client = serverSocket.accept();
                ConnectedClientThread clientThread = new ConnectedClientThread(ctx, client);
                allClientThreadHandles.add(clientThread);
                clientThread.start();
            }
            Log.d(TAG, "Finishing thread");
        } catch (Exception e) {
            Log.e(TAG, "Unable to start server thread", e);
        }
    }

    public void close() {
        Log.i(TAG, "Shutting down server");
        this.interrupt();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ioe) {
                Log.e(TAG, "Unable to close the server socket", ioe);
            }
        }

        //Make sure to close all the client threads when server is going down.
        if (allClientThreadHandles.size() > 0) {
            for (ConnectedClientThread clientThread : allClientThreadHandles) {
                clientThread.close();
            }
        }

    }
}
