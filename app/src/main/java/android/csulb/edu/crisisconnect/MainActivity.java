package android.csulb.edu.crisisconnect;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.csulb.edu.crisisconnect.Calling.LandingActivity;
import android.csulb.edu.crisisconnect.Services.UpdateService;
import android.csulb.edu.crisisconnect.WifiHotspotApis.ClientScanResult;
import android.csulb.edu.crisisconnect.WifiHotspotApis.FinishScanListener;
import android.csulb.edu.crisisconnect.WifiHotspotApis.WIFI_AP_STATE;
import android.csulb.edu.crisisconnect.WifiHotspotApis.WifiApManager;
import android.csulb.edu.crisisconnect.database.MessageHistoryDbHelper;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

import static android.os.Build.VERSION_CODES.M;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    TextView textView1;
    WifiApManager wifiApManager;
    private MessageHistoryDbHelper dbHelper = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.INTERNET, Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.CHANGE_NETWORK_STATE,Manifest.permission.WRITE_SETTINGS,Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        textView1 = (TextView) findViewById(R.id.textView1);
        wifiApManager = new WifiApManager(this);

        Boolean retVal = false;
        if (Build.VERSION.SDK_INT >= M) {
            retVal = Settings.System.canWrite(this);}

        if (!retVal) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + this.getPackageName()));
            startActivity(intent);
        }
        scan();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        byte[] clientz = intent.getByteArrayExtra(UpdateService.EXTRA_CLIENT_LIST);
                        ArrayList<ClientScanResult> list=null;
                        ObjectInputStream ois = null;
                        try {
                            ois = new ObjectInputStream(new ByteArrayInputStream(clientz));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            try {

                                 list = (ArrayList<ClientScanResult>) ois.readObject();
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } finally {
                            try {
                                ois.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
                        int ip = wifiInfo.getIpAddress();
                        String localIP = Formatter.formatIpAddress(ip);

                        for (ClientScanResult client : wifiApManager.results) {
                            Iterator<ClientScanResult> iter = list.iterator();
                            while(iter.hasNext()){
                                ClientScanResult item = iter.next();
                                if( item.getIpAddr().equals(client.getIpAddr()) )
                                {
                                    iter.remove();
                                }
                            }
                        }

                        for (ClientScanResult client : list) {

                           if(!client.getIpAddr().equals(localIP)){
                               wifiApManager.results.add(client);
                            Toast.makeText(getApplicationContext(), client.getIpAddr(), Toast.LENGTH_SHORT).show();}
                        }

                    }
                }, new IntentFilter(UpdateService.ACTION_UPDATE_BROADCAST)
        );


    }

    public void scan() {

        wifiApManager.getClientList(false, new FinishScanListener() {

            @Override
            public void onFinishScan(final ArrayList<ClientScanResult> clients) {
                Log.d(TAG, "Finished Scanning...Populating list...");
                textView1.setText("WifiApState: " + wifiApManager.getWifiApState() + "\n\n");
                textView1.append("Clients: \n");
                for (ClientScanResult clientScanResult : clients) {
                    textView1.append("####################\n");
                    textView1.append("IpAddr: " + clientScanResult.getIpAddr() + "\n");
                    textView1.append("Device: " + clientScanResult.getDevice() + "\n");
                    textView1.append("HWAddr: " + clientScanResult.getHWAddr() + "\n");
                    textView1.append("isReachable: " + clientScanResult.isReachable() + "\n");
                }

                ClientAdapter clientadapter = new ClientAdapter(MainActivity.this,wifiApManager.results);
                ListView listView = (ListView) findViewById(R.id.clntlst);
                listView.setAdapter(clientadapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        ClientScanResult a = wifiApManager.results.get(i);
                        Toast.makeText(MainActivity.this, a.getIpAddr()+a.getHWAddr(), Toast.LENGTH_LONG).show();

                    }
                });

                if (wifiApManager.getWifiApState() == WIFI_AP_STATE.WIFI_AP_STATE_ENABLED) {
                    updateAllClients(clients);
                }

            }
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "Get Clients");
        menu.add(0, 1, 0, "Open AP");
        menu.add(0, 2, 0, "Close AP");
        menu.add(0, 3, 0, "Search Networks");
        return super.onCreateOptionsMenu(menu);
    }


    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                scan();
                break;
            case 1:
                wifiApManager.setWifiApEnabled(null, true);
                break;
            case 2:
                wifiApManager.setWifiApEnabled(null, false);
                break;
            case 3:
                LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

                if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    buildAlertMessageNoGps();
                } else {
                    startActivity(new Intent(MainActivity.this, SearchNetworks.class));
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void calls(View view) {
        Intent landing = new Intent(this, LandingActivity.class);
        startActivity(landing);
    }

    public void updateAllClients(final ArrayList<ClientScanResult> clients) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = null;
                    for (ClientScanResult client : clients) {
                        socket = new Socket();
                        socket.bind(null);
                        socket.connect(new InetSocketAddress(client.getIpAddr(), 50001), 500);
                        OutputStream oStream = socket.getOutputStream();
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(bos);
                        oos.writeObject(clients);

                        byte[] bytes = bos.toByteArray();
                        oStream.write(bytes);
                        socket.close();
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "Starting service");
        super.onResume();
        startService(new Intent(this, UpdateService.class));
        AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                //Initialize the database connection. We do not use it now and just prepare it for later use
                if (dbHelper == null) {
                    dbHelper = new MessageHistoryDbHelper(MainActivity.this);
                }
                return null;
            }
        };
        task.execute();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "Stopping service");
        super.onPause();
        stopService(new Intent(this, UpdateService.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }
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
}

