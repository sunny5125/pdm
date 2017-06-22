package com.disarm.sanna.pdm.DisarmConnect;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;

/**
 * Created by hridoy on 21/8/16.
 */
public class SearchingDisarmDB implements Runnable {
    private android.os.Handler handler;
    private Context context;
    private int timerDBSearch = 3000;
    public int minDBLevel = 2;

    public String connectedSSID = DCService.wifi.getConnectionInfo().getSSID().toString().replace("\"","");
    public String lastConnectedSSID = connectedSSID;


    public SearchingDisarmDB(android.os.Handler handler, Context context)
    {
        this.handler = handler;
        this.context = context;

        this.handler.post(this);
    }
    @Override
    public void run()
    {
        connectedSSID = DCService.wifi.getConnectionInfo().getSSID().toString().replace("\"","");
        lastConnectedSSID.replace("\"","");
        Log.v("ConnectedSSID:",connectedSSID);
        if(lastConnectedSSID.startsWith("DH-") && !(lastConnectedSSID.equals(connectedSSID)))
        {
            Log.v("Disconnected DH:",lastConnectedSSID);
            Logger.addRecordToLog("Disconnected : " + lastConnectedSSID);
            lastConnectedSSID = "";
        }
        else if(lastConnectedSSID.contains("DB") && !(lastConnectedSSID.equals(connectedSSID))) {
            Log.v("Disconnected DB:",lastConnectedSSID);
            Logger.addRecordToLog("Disconnected : " + lastConnectedSSID);
            lastConnectedSSID = "";
        }
        else if(connectedSSID.startsWith("DH-") || connectedSSID.contains("DB"))
        {
            Log.v("Connected SSID:", connectedSSID + "LastConnectedSSID:" + lastConnectedSSID);
            lastConnectedSSID = connectedSSID;
        }
        else
        {
            Log.v("Connected SSID:", connectedSSID);
            lastConnectedSSID = "";
            connectedSSID = "";
        }
        Log.v(DCService.TAG4,"searching DB");
        List<ScanResult> allScanResults = DCService.wifi.getScanResults();
        if (allScanResults.toString().contains(DCService.dbAPName)) {
            // compare signal level
            int level = findDBSignalLevel(allScanResults);
            if (level < minDBLevel)
            {
                if(connectedSSID.contains("DB")) {
                    if (DCService.wifi.disconnect()) {
                        Logger.addRecordToLog("DB Disconnected as Level = " + level);
                        Log.v(DCService.TAG1, "DB Disconnected as Level = " + level);
                    }
                }
            }
            else {
                Log.v(DCService.TAG4, "Connecting DisarmDB");

              //  handler.removeCallbacksAndMessages(null); !-- Dont use it all other handler will be closed
                String ssid = DCService.dbAPName;
                WifiConfiguration wc = new WifiConfiguration();
                wc.SSID = "\"" + ssid + "\""; //IMPORTANT! This should be in Quotes!!
               // wc.preSharedKey = "\""+ DCService.dbPass +"\"";
                //wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

                int res = DCService.wifi.addNetwork(wc);
                boolean b = DCService.wifi.enableNetwork(res, true);
                Log.v(DCService.TAG4, "Connected to DB");
            }

        }
        else {
            Log.v(DCService.TAG4,"DisarmHotspotDB not found");
        }
        handler.postDelayed(this, timerDBSearch);
    }
    public int findDBSignalLevel(List<ScanResult> allScanResults)
    {
        for (ScanResult scanResult : allScanResults) {
            if(scanResult.SSID.toString().equals(DCService.dbAPName)) {
                Log.v("SSID:",scanResult.SSID.toString());
                int level =  WifiManager.calculateSignalLevel(scanResult.level, 5);
                return level;
            }
        }
        return 0;
    }

}
