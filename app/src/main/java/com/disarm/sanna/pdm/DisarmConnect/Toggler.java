package com.disarm.sanna.pdm.DisarmConnect;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.disarm.sanna.pdm.MainActivity;
import com.disarm.sanna.pdm.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

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
    public static ArrayList<Integer> allFrequency = new ArrayList<>();

    // Set hotspot creation minimum battery level
    private static double minimumBatteryLevel = 10;

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
        for ( int i = 0 ; i < MyService.wifiScanList.size();i++) {

            allFrequency.add(convertFrequencyToChannel(MyService.wifiScanList.get(i).frequency));
            Log.v("Channel  + AP: ", String.valueOf(convertFrequencyToChannel(MyService.wifiScanList.get(i).frequency)) + "->" + String.valueOf(MyService.wifiScanList.get(i).SSID));
        }
        Log.v("All frequency:",allFrequency.toString());

        int channelArray[] = new int[15];

        // Fill all the elements with 0
        Arrays.fill(channelArray,0);

        // Increment the value of array elements for each channel
        for (int s:allFrequency)
        {
            channelArray[s]++;
        }

        // Find minimum value from the channel array
        int small = channelArray[0];
        int index = 0;
        for (int i = 1; i < channelArray.length; i++) {
            if (channelArray[i] < small) {
                small = channelArray[i];
                index = i;
            }
        }
        Log.v("Channel Array:",Arrays.toString(channelArray));
        Log.v("Minimum Channel Value:", String.valueOf(small));

        // Find in channel array the elements with minimum channel value
        ArrayList bestFoundAvailableChannels = new ArrayList();
        for(int items = 0;items < channelArray.length;items++)
        {
            if(channelArray[items] == small)
            {
                bestFoundAvailableChannels.add(items);
            }
        }

        Log.v("Best Found Available Channels : ",Arrays.deepToString(bestFoundAvailableChannels.toArray()));

        // Find a random available channel from best found available channels
        Random rand = new Random();
        Integer randGeneratedBestFoundChannel = (Integer) bestFoundAvailableChannels.get(rand.nextInt(bestFoundAvailableChannels.size()));
        Log.v("Generated Random Available Channel:" ,randGeneratedBestFoundChannel.toString() );
        return randGeneratedBestFoundChannel;
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
            //MainActivity.img_wifi_state.setImageResource(R.drawable.hotspot);

            // Set text to textConnect TextView
            String apHotspotName = "DH" + MyService.phoneVal;
//            MainActivity.textConnect.setText(apHotspotName);

            // Find channel weight of all Wifis
            MyService.bestAvailableChannel = findChannelWeight();

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
            //MainActivity.img_wifi_state.setImageResource(R.drawable.wifi);

            // Set text to textConnect TextView
            //MainActivity.textConnect.setText("");

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
