package com.disarm.sanna.pdm.DisarmConnect;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by hridoy on 21/8/16.
 */
public class SearchingDisarmDB implements Runnable {
    private android.os.Handler handler;
    private Context context;
    private int timerDBSearch = 5000;

    public SearchingDisarmDB(android.os.Handler handler, Context context)
    {
        this.handler = handler;
        this.context = context;

        this.handler.post(this);
    }
    @Override
    public void run()
    {
        String connectedDH = MyService.wifi.getConnectionInfo().getSSID().toString().replace("\"","");
        List<ScanResult> allScanResult = MyService.wifi.getScanResults();

        Log.v(MyService.TAG4,"searching DB");
        if (allScanResult.toString().contains(MyService.dbAPName)) {
            Log.v(MyService.TAG4, "Connecting DisarmDB");



            handler.removeCallbacksAndMessages(null);
            String ssid = MyService.dbAPName;
            WifiConfiguration wc = new WifiConfiguration();
            wc.SSID = "\"" + ssid + "\""; //IMPORTANT! This should be in Quotes!!
            wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            int res = MyService.wifi.addNetwork(wc);
            boolean b = MyService.wifi.enableNetwork(res, true);
            Log.v(MyService.TAG4, "Connected to DB");

            // Get Strength of connected DB Level
            int numOfLevels = 5;
            WifiInfo wifiInfo = MyService.wifi.getConnectionInfo();
            int connectedDBLevel = MyService.wifi.calculateSignalLevel(wifiInfo.getRssi(),numOfLevels);

            // Check DB strength and disconnect connection if required
            Log.v("Checking for other DH:",".");
            String otherDH="";
            Map allDHDBAvailable = new HashMap<String, Integer>();

            for (ScanResult scanResult : allScanResult) {
                otherDH = scanResult.SSID.toString();
                if(otherDH.contains("DH-") || otherDH.contains("DB")) {
                    allDHDBAvailable.put(scanResult.SSID,scanResult.level);
                }
            }
            Log.v("All DHDBAvailable:", String.valueOf(Arrays.asList(allDHDBAvailable.toString())));
            String bestFoundSSID="";
            int maxValueInMap = 0;
            try {
                maxValueInMap = (int) Collections.max(allDHDBAvailable.values());  // This will return max value in the Hashmap
                Iterator it = allDHDBAvailable.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Integer> pair = (Map.Entry) it.next();
                    if (pair.getValue() == maxValueInMap) {
                        Log.v("Best Found SSID:", pair.getKey());     // Print the key with max value
                        bestFoundSSID = pair.getKey().toString();
                    }
                }
            }
            catch (Exception e)
            {}
            if((bestFoundSSID.toString() != connectedDH) && (connectedDBLevel > 1))
            {
                Log.v("Found:",bestFoundSSID.toString() + " ," + connectedDBLevel);

                String pass = "password123";
                wc.SSID = "\"" + bestFoundSSID + "\""; //IMPORTANT! This should be in Quotes!!
                wc.preSharedKey = "\""+ pass +"\"";
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                res = MyService.wifi.addNetwork(wc);
                b = MyService.wifi.enableNetwork(res, true);
                Log.v(MyService.TAG2, "Connected");
            }
            else
            {
                Log.v("Connected DB Level:",String.valueOf(connectedDBLevel));
            }
        }
        else
        {
            Map allDHAvailable = new HashMap<String, Integer>();
            String otherDH="";
            if(connectedDH.contains("DH-")) {
               Log.v("Connected Wifi:", connectedDH);
                for (ScanResult scanResult : allScanResult) {
                    otherDH = scanResult.SSID.toString();
                    if(otherDH.contains("DH-")) {
                        allDHAvailable.put(scanResult.SSID,scanResult.level);
                    }
                }
            }
            try {
                String bestFoundSSID="";
                int maxValueInMap = (int) Collections.max(allDHAvailable.values());  // This will return max value in the Hashmap
                Iterator it = allDHAvailable.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Integer> pair = (Map.Entry) it.next();
                    if (pair.getValue() == maxValueInMap) {
                        Log.v("Best Found SSID:", pair.getKey());     // Print the key with max value
                        bestFoundSSID = pair.getKey().toString();
                        Logger.addRecordToLog("Best Found SSID"+ ',' + pair.getKey());

                    }
                }
                Log.v("Better DH: ",bestFoundSSID.toString());
                if(!(connectedDH.toString().equals(bestFoundSSID.toString())))
                {
                    String pass = "password123";
                    WifiConfiguration wc = new WifiConfiguration();
                    wc.SSID = "\"" + bestFoundSSID + "\""; //IMPORTANT! This should be in Quotes!!
                    wc.preSharedKey = "\""+ pass +"\"";
                    wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    //wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    int res = MyService.wifi.addNetwork(wc);
                    boolean b = MyService.wifi.enableNetwork(res, true);
                    Log.v("Changed DH connection:",connectedDH + " to " + bestFoundSSID);
                }
            }
            catch (Exception e)
            {}
            Log.v("All DH Available:", String.valueOf(Arrays.asList(allDHAvailable.toString())));
        }
        handler.postDelayed(this, timerDBSearch);
    }

}
