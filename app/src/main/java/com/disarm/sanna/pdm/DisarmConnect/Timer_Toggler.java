package com.disarm.sanna.pdm.DisarmConnect;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.os.Handler;
import android.view.LayoutInflater;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hridoy on 19/8/16.
 */
public class Timer_Toggler implements Runnable{
    private BufferedReader br;
    private FileReader fr = null;
    private List<String> IpAddr;
    private android.os.Handler handler;
    private Context context;
    public Timer_Toggler(android.os.Handler handler, Context context)
    {
        this.handler = handler;
        this.context = context;

        this.handler.post(this);
    }
    @Override
    public void run() {
        MyService.wifi = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        WifiInfo wifiInfo = MyService.wifi.getConnectionInfo();
        MyService.checkWifiState = wifiInfo.getSSID();
        Log.v(MyService.TAG1, "Ticking");
        Log.v(MyService.TAG1, MyService.checkWifiState);
        MyService.count++;


        if (MyService.checkWifiState.equals("<unknown ssid>")) {
            Log.v(MyService.TAG1, "Hotspot Mode Detected");
            boolean isReachable = false;
            try {

                fr = new FileReader("/proc/net/arp");
                br = new BufferedReader(fr);
                String line;
                IpAddr = new ArrayList<String>();
                MyService.c = false;
                while ((line = br.readLine()) != null) {
                    String[] splitted = line.split(" +");

                    if (splitted != null) {
                        if (splitted[3].matches("..:..:..:..:..:..")) {
                            Process p1 = Runtime.getRuntime().exec("ping -c 1 -t 1 " + splitted[0]);
                            int returnVal = p1.waitFor();
                            isReachable = (returnVal == 0);

                        }
                        if (isReachable) {
                            MyService.c = true;
                            Log.v(MyService.TAG1, "C IS TRUE !!! ");

                        }

                        // Basic sanity check
                        String mac = splitted[3];
                        System.out.println("Mac : Outside If " + mac);

                        if (mac.matches("..:..:..:..:..:..")) {
                            MyService.macCount++;

                            IpAddr.add(splitted[0]);

                            Log.v(MyService.TAG1, "IP Address  " + splitted[0] + "   MAC_ADDRESS  " + mac);
                            Logger.addRecordToLog("Connected Client, IP :" + splitted[0] + ",mac:" + mac);
                        }
                    }
                }
                if (MyService.c) {
                    Log.v(MyService.TAG1, "Connected!!! ");
                } else {
                    Log.v(MyService.TAG1, "Not Connected!!! ");

                }
            } catch (Exception e) {
                Log.v(MyService.TAG1, "exception", e);
            } finally {
                if (fr != null) {
                    try {
                        fr.close();
                        br.close();
                        IpAddr.clear();
                    } catch (IOException e) {
                        // This is unrecoverable. Just report it and move on
                        e.printStackTrace();
                    }
                }
            }

            if (!MyService.c) {
                Toggler.toggle(context);
            }


        } //if Completed check

        else if(MyService.checkWifiState.contains("DisarmHotspotDB")) {
            Log.v(MyService.TAG1, "DisarmHotspotDB Not Toggling");

        }
        else if (MyService.checkWifiState.contains(MyService.mobileAPName)) {
            /////////////////////////
           // this.handler.post(searchingDisarmDB);
            Log.v(MyService.TAG1, "DisarmHotspot Not Toggling");


        }

        else
        {
            Toggler.toggle(context);
        }
        boolean apOn = ApManager.isApOn(context);
        if(apOn){
            this.handler.postDelayed(this,Toggler.addIncreasehp);
        }else{
            this.handler.postDelayed(this,Toggler.addIncreasewifi);
        }
    }

}
