package com.disarm.sanna.pdm.Service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.disarm.sanna.pdm.SelectCategoryActivity;
import com.disarm.sanna.pdm.Util.PrefUtils;
import com.nextgis.maplib.util.SettingsConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import bishakh.psync.Controller;
import bishakh.psync.Discoverer;
import bishakh.psync.FileManager;
import bishakh.psync.FileTransporter;
import bishakh.psync.Logger;
import bishakh.psync.WebServer;

public class SyncService extends Service {

    private static final String BROADCAST_IP = "192.168.43.255";
    private static final int PORT = 4446;
    private static final int syncInterval = 5;
    private static final int maxRunningDownloads = 5;

    private static String sdcard = Environment.getExternalStorageDirectory().toString();
    private String syncDirectory = sdcard +"/DMS/Working/";
    private String mapDirectory = sdcard +"/DMS/Map/";
    private String databaseDirectory = sdcard + "/DMS/";
    private static String databaseName = "fileDB.txt";
    static String line;
    public WebServer webServer;
    public Discoverer discoverer;
    public FileManager fileManager;
    public FileTransporter fileTransporter;
    public Controller controller;
    private String source;
    public Logger logger;
    private final IBinder syncServiceBinder = new SyncServiceBinder();

    public SyncService() {
        source = SelectCategoryActivity.SOURCE_PHONE_NO;

        logger =new Logger(databaseDirectory,source);
        discoverer = new Discoverer(BROADCAST_IP,source, PORT,logger);
        fileManager = new FileManager(source, databaseName, databaseDirectory,syncDirectory,mapDirectory,logger);
        fileTransporter = new FileTransporter(syncDirectory,logger);
        controller = new Controller(discoverer, fileManager, fileTransporter, syncInterval, maxRunningDownloads, logger, false);
        webServer = new WebServer(8080, controller,logger);
    }

    @Override
    public void onCreate() {
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        discoverer.startDiscoverer();
        fileManager.startFileManager();
        controller.startController();
        try {
            webServer.start();
        } catch(IOException ioe) {
            Log.w("Httpd", "The server could not start.");
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return syncServiceBinder;
    }


    @Override
    public boolean stopService(Intent name) {
        discoverer.stopDiscoverer();
        fileManager.stopFileManager();
        controller.stopController();
        webServer.stop();
        return super.stopService(name);
    }

    @Override
    public void onDestroy() {
        discoverer.stopDiscoverer();
        fileManager.stopFileManager();
        controller.stopController();
        webServer.stop();
        super.onDestroy();
    }

    public class SyncServiceBinder extends Binder {
        public SyncService getService() {
            // Return this instance of SyncService so activity can call public methods
            return SyncService.this;
        }
    }


}
