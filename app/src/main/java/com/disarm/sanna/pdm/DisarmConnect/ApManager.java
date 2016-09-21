package com.disarm.sanna.pdm.DisarmConnect;

/**
 * Created by sanna on 2/12/15.
 */

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.lang.reflect.Method;

public class ApManager {
    private static final String TAG = "AP Creation";
    //check whether wifi hotspot on or off
    public static boolean isApOn(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        try {
            Method method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifiManager);
        }
        catch (Throwable ignored) {}
        return false;
    }


    // toggle wifi hotspot on or off
    public static boolean configApState(Context context) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        WifiConfiguration wificonfiguration = null;
        try {
            // if WiFi is on, turn it off
            if (isApOn(context)) {
                wifimanager.setWifiEnabled(false);
            }
            WifiConfiguration netConfig = new WifiConfiguration();
            //Change Name of the Created Hotspot

            WifiConfiguration wifiCon = new WifiConfiguration();
            wifiCon.SSID = "DH-" + MyService.phoneVal;
            wifiCon.preSharedKey = "password123";

            wifiCon.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            wifiCon.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wifiCon.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wifiCon.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            try {
                Method setWifiApMethod = wifimanager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                boolean apstatus = (Boolean) setWifiApMethod.invoke(wifimanager, wifiCon, true);
            } catch (Exception e) {

            }
        }
        catch (Exception e)
        {}

        return false;
    }

}