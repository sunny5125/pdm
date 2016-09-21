package com.disarm.sanna.pdm.DisarmConnect;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by hridoy on 21/8/16.
 */
public class SearchingBestDisarmDH implements Runnable {
    private android.os.Handler handler;
    private Context context;
    private int timerDHSearch = 7000;

    public SearchingBestDisarmDH(android.os.Handler handler, Context context)
    {
        this.handler = handler;
        this.context = context;

        this.handler.post(this);
    }
    @Override
    public void run()
    {
        Log.v(MyService.TAG4,"searching best DH");
        String connectedDH = MyService.wifi.getConnectionInfo().getSSID();
        Map allOtherDHAvailable = new HashMap<String, Integer>();
        String otherDH="";
        if(connectedDH.contains("DH-")) {
            List<ScanResult> allScanResults = MyService.wifi.getScanResults();
            for (ScanResult scanResult : allScanResults) {
                otherDH = scanResult.SSID.toString();
                if(otherDH.contains("DH-") && (otherDH != connectedDH)) {
                    Log.v("Found other DH:",otherDH.toString());
                    allOtherDHAvailable.put(scanResult.SSID,scanResult.level);
                }
            }
            /*if ((otherDH = allScanResults.toString().contains(MyService.mobileAPName)) && ) {
                Log.v(MyService.TAG4, "Connecting DisarmDB");

                // compare signal level
                int level = compareSignalLevel(allScanResults);
                Log.v("Level:", String.valueOf(level));

                //handler.removeCallbacks(WifiConnect.class);
                handler.removeCallbacksAndMessages(null);
                String ssid = MyService.dbAPName;
                WifiConfiguration wc = new WifiConfiguration();
                wc.SSID = "\"" + ssid + "\""; //IMPORTANT! This should be in Quotes!!
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                int res = MyService.wifi.addNetwork(wc);
                boolean b = MyService.wifi.enableNetwork(res, true);
                Log.v(MyService.TAG4, "Connected to DB");
            } else {
                Log.v(MyService.TAG4, "DisarmHotspotDB not found");
            }*/
            handler.postDelayed(this, timerDHSearch);
        }
    }
    public int compareSignalLevel(List<ScanResult> allScanResults)
    {
        for (ScanResult scanResult : allScanResults) {
            if(scanResult.SSID.toString().equals(MyService.dbAPName)) {
                Log.v("SSID:",scanResult.SSID.toString());
                int level =  WifiManager.calculateSignalLevel(scanResult.level, 5);
                return level;

            }
        }
        return 0;
    }

}
