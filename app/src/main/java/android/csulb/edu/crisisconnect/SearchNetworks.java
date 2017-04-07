package android.csulb.edu.crisisconnect;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.csulb.edu.crisisconnect.broadcastreceiver.WifiScanReceiver;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import pl.bclogic.pulsator4droid.library.PulsatorLayout;

public class SearchNetworks extends Activity {
    PulsatorLayout pulseLayout = null;
    WifiManager mManager = null;
    WifiScanReceiver wifiReceiver = null;
    IntentFilter wifiScanFilter = null;
    List<ScanResult> wifiResult = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_networks);

        pulseLayout = (PulsatorLayout) findViewById(R.id.pulsator_layout);
        pulseLayout.start();

        mManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        wifiReceiver = new WifiScanReceiver(mManager);
        wifiScanFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        if(!mManager.isWifiEnabled()){
            mManager.setWifiEnabled(true);
        }
        mManager.startScan();
    }

    @Override
    protected void onResume(){
        super.onResume();
        registerReceiver(wifiReceiver, wifiScanFilter);
    }

    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(wifiReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch(requestCode){
            case WifiScanReceiver.PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    processWifiClients();
                }
        }
    }

    public void processWifiClients(){
        Log.e("CLIENTS", "IN");
        wifiResult = mManager.getScanResults();
        Log.e("CLIENTS", String.valueOf(wifiResult.size()));
        List<String> clients = new ArrayList<String>();
        for(ScanResult wifiClient : wifiResult){
            clients.add(wifiClient.SSID);
        }
        ListView lv = (ListView) findViewById(R.id.list_view);
        NetworkListAdapter adapter = new NetworkListAdapter(this, R.layout.network_listview_row, wifiResult);
        if(!adapter.isEmpty()) {
            lv.setAdapter(adapter);
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    connectWifi(parent, view, position, id);
                }
            });
        }
        else{
            Toast.makeText(this, "No WiFi connections found!", Toast.LENGTH_LONG).show();
        }
    }

    public void connectWifi(AdapterView<?> parent, View view, int position, long id){
        ScanResult client = (ScanResult) parent.getItemAtPosition(position);
        if(client == null || client.SSID.trim().equals("")){
            Toast.makeText(this, "Unable to connect!", Toast.LENGTH_LONG).show();
            return;
        }
        final WifiConfiguration configuration = new WifiConfiguration();
        //configuration.SSID = "\"" + client.SSID + "\"";
        configuration.SSID = String.format("\"%s\"", client.SSID);
        configuration.priority = 99999;
        String security = client.capabilities;

        if(security.contains("WPA2") || security.contains("WPA")){
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final EditText eText = new EditText(this);
            eText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            builder.setTitle(client.SSID)
                    .setMessage("Password:")
                    .setView(eText)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(eText.getText().toString().trim().equals("")){
                                Toast.makeText(SearchNetworks.this, "Empty password not allowed!", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                //configuration.preSharedKey = "\"" + eText.getText().toString() + "\"";
                                configuration.preSharedKey = String.format("\"%s\"", eText.getText().toString());
                                dialog.cancel();
                                configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                                configuration.allowedProtocols.set(WifiConfiguration.Protocol.WPA); // For WPA
                                configuration.allowedProtocols.set(WifiConfiguration.Protocol.RSN); // For WPA2
                                configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                                configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
                                configuration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                                configuration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                                configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                                configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                                connectWifi(configuration);
                            }
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            builder.show();
        }
        //For Open networks that are not WEP
        else if(!security.contains("WEP")){
            configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            connectWifi(configuration);
        }
    }

    public void connectWifi(WifiConfiguration configuration){
        int networkId = mManager.addNetwork(configuration);
        mManager.disconnect();
        mManager.enableNetwork(networkId, true);
        boolean connectStatus = mManager.reconnect();
        if(connectStatus){
            Toast.makeText(this, "Connecting to the network now...", Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(this, "Connection unsuccessful!", Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
