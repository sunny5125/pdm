package com.disarm.sanna.pdm.DisarmConnect;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.disarm.sanna.pdm.MainActivity;
import com.disarm.sanna.pdm.R;

import java.util.Arrays;
import java.util.List;


/**
 * Created by hridoy on 19/8/16.
 */
public class Toggler extends Activity{
    // Randomly value less than 0.50 will make HotspotActive else WifiActive
    private static double toggleBetweenHotspotWifi = 0.90;

    public static  int addIncreasewifi = 5000,wifiIncrease= 5000,hpIncrease=5000,addIncreasehp = 5000;

    // max increase of Wifi and HP Value
    private static int maxWifiIncrease = 35000;
    private static int maxHPIncrease = 35000;

    // Set hotspot creation minimum battery level
    private static double minimumBatteryLevel = 10;
    public static List<Integer> allFrequency;

    public static int convertFrequencyToChannel(int freq) {
        if (freq >= 2412 && freq <= 2484) {
            return (freq - 2412) / 5 + 1;
        } else if (freq >= 5170 && freq <= 5825) {
            return (freq - 5170) / 5 + 34;
        } else {
            return -1;
        }
    }

    public static int findChannelWeight()
    {
        // Get frequency of all the channel available
        for ( int i = 0 ; i < MyService.wifiScanList.size();i++)
        {
            allFrequency.add(convertFrequencyToChannel(MyService.wifiScanList.get(i).frequency));
            Log.v("Channel: " ,String.valueOf(convertFrequencyToChannel(MyService.wifiScanList.get(i).frequency)));
        }
        //Log.v("Channels Available: ", allFrequency.toString());

        //
        return 1;
    }

    public static void toggle(Context c){
        Log.v(MyService.TAG3, "Toggling randomly!!!");

        // Start wifi for the first time and then randomly toggle
        if (MyService.startwififirst == 1){
            MyService.wifiState= 1.00;
            MyService.startwififirst = 0;
        }
        else
        {
            MyService.wifiState = Math.random()*1.0;
            Log.v(MyService.TAG3, String.valueOf(MyService.wifiState));
        }
        Log.v("Battery Level:", String.valueOf(MyService.level));
        Log.v("Present State:", MyService.presentState);

        if(MyService.wifiState <= toggleBetweenHotspotWifi && MyService.level > minimumBatteryLevel ) {
            // Present State
            MyService.presentState = "hotspot";

            // Set ImageView to Hotspot
            MainActivity.img_wifi_state.setImageResource(R.drawable.hotspot);


            // Find channel weight of all Wifis
            int bestAvailableChannel = findChannelWeight();

            // Set text to textConnect TextView
            String apHotspotName = "DH" + MyService.phoneVal;
            MainActivity.textConnect.setText(apHotspotName);

            // Hotspot Mode Activated
            Log.v(MyService.TAG1,"hptoggling for " +String.valueOf(addIncreasehp));
            Logger.addRecordToLog("HA : " + addIncreasehp + " secs," + "Random :" + String.format("%.2f", MyService.wifiState));

            // Adding hotspot increase time counter by factor of hpIncrease
            addIncreasehp += hpIncrease;

            // Disabling Wifi and Enabling Hotspot
            MyService.wifi.setWifiEnabled(false);
            MyService.isHotspotOn = ApManager.isApOn(c);


            if (!MyService.isHotspotOn) {
                ApManager.configApState(c);
            }
            Log.v(MyService.TAG3, "Hotspot Active");

        }
        else {
            MyService.presentState = "wifi";

            // Set ImageView to Wifi
            MainActivity.img_wifi_state.setImageResource(R.drawable.wifi);

            // Set text to textConnect TextView
            MainActivity.textConnect.setText("");

            // Wifi Mode Activated
            Log.v(MyService.TAG3,"wifitogging for "+ String.valueOf(addIncreasewifi));

            Logger.addRecordToLog("WA : " + addIncreasewifi + " secs," + "Random :" + String.format("%.2f", MyService.wifiState));

            // Adding wifi increase time counter by factor of wifiIncrease
            addIncreasewifi += wifiIncrease;

            // Disabling hotspot and enable Wifi
            MyService.isHotspotOn = ApManager.isApOn(c);

            if(MyService.isHotspotOn)
            {
                ApManager.configApState(c);
            }
            MyService.wifi.setWifiEnabled(true);
            Log.v(MyService.TAG3, "Wifi Active");

            MyService.wifi.startScan();

        }

        if(addIncreasewifi == maxWifiIncrease){
            addIncreasewifi = 10000;
        }
        else if(addIncreasehp == maxHPIncrease){
            addIncreasehp = 10000;
        }

    }
}
