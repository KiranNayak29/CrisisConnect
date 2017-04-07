package android.csulb.edu.crisisconnect.broadcastreceiver;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.csulb.edu.crisisconnect.SearchNetworks;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.List;

/**
 * Created by vaibhavjain on 4/4/2017
 */

public class WifiScanReceiver extends BroadcastReceiver {
    WifiManager mManager = null;
    public static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 0;

    public WifiScanReceiver(WifiManager wifiManager){
        mManager = wifiManager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
        }
        else{
            ((SearchNetworks)context).processWifiClients();
        }
    }
}
