package android.csulb.edu.crisisconnect;

import java.util.ArrayList;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.csulb.edu.crisisconnect.Calling.LandingActivity;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.BoolRes;
import android.support.v4.app.ActivityCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import android.csulb.edu.crisisconnect.WifiHotspotApis.*;
import android.csulb.edu.crisisconnect.WifiHotspotApis.ClientScanResult;
import android.csulb.edu.crisisconnect.WifiHotspotApis.FinishScanListener;
import android.csulb.edu.crisisconnect.WifiHotspotApis.WifiApManager;
import android.csulb.edu.crisisconnect.WifiHotspotApis.WifiApManager;
import android.csulb.edu.crisisconnect.Calling.LandingActivity;

public class MainActivity extends Activity {
    TextView textView1;
    WifiApManager wifiApManager;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.INTERNET, Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.CHANGE_NETWORK_STATE,Manifest.permission.WRITE_SETTINGS,Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,Manifest.permission.CAMERA,Manifest.permission.READ_PHONE_STATE};

        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        textView1 = (TextView) findViewById(R.id.textView1);
        wifiApManager = new WifiApManager(this);
//
        Boolean retVal = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            retVal = Settings.System.canWrite(this);}

        if(retVal){
            Toast.makeText(this, "Write allowed :-)", Toast.LENGTH_LONG).show();
        }else{
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + this.getPackageName()));
        //    startActivity(intent);
        }





scan();



    }

    public void scan() {

        wifiApManager.getClientList(false, new FinishScanListener() {

            @Override
            public void onFinishScan(final ArrayList<ClientScanResult> clients) {

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

            }
        });
    }

    public void refresh()
    {


    }


    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "Get Clients");
        menu.add(0, 1, 0, "Open AP");
        menu.add(0, 2, 0, "Close AP");
        menu.add(0, 3, 0, "Search Networks");
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                scan();
                refresh();
                break;
            case 1:
                wifiApManager.setWifiApEnabled(null, true);
                break;
            case 2:
                wifiApManager.setWifiApEnabled(null, false);
                break;
            case 3:
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                break;
        }

        return super.onMenuItemSelected(featureId, item);
    }

public void calls(View view)
{
    Toast.makeText(this, "Now Calling", Toast.LENGTH_SHORT).show();
    Intent landing = new Intent(this, LandingActivity.class);
    startActivity(landing);

}


}

