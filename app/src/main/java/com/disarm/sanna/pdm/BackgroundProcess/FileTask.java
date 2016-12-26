package com.disarm.sanna.pdm.BackgroundProcess;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import static Sensors.readWriteMetaData.getImageAttributes;
import static Sensors.readWriteMetaData.saveExif;

import com.disarm.sanna.pdm.ActivityList;
import com.disarm.sanna.pdm.ShareActivity;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import static Sensors.readWriteMetaData.getImageAttributes;
import static Sensors.readWriteMetaData.saveExif;


/**
 * Created by Sanna on 05-07-2016.
 */
public class FileTask extends AsyncTask  {
    String fileType,groupType,timestamp,ttl,dest,source,fileFormat;
    String[] fileName;
    public File sourceFile = Environment.getExternalStoragePublicDirectory("DMS/source.txt");
    public static final String GROUPID = "Group No";
    Context applicationContext = ActivityList.getContextOfApplication();
    Context shareActivityContext = ShareActivity.getContextOfApplication();
    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    int idNumber,groupID;
    private boolean increaseSession = false;


    @Override
    protected synchronized void onPreExecute() {
        super.onPreExecute();

        if(applicationContext != null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        } else if (shareActivityContext != null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(shareActivityContext);
        }

        editor = prefs.edit();
        idNumber=prefs.getInt(GROUPID, 0);
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        ttl = (String) objects[0];
        dest = (String) objects[1];
        source = ReadLastLine.tail(sourceFile);
        File logFile = null;

       String state = Environment.getExternalStorageState();
        File dir = Environment.getExternalStoragePublicDirectory("DMS/Working");
        Log.v("Logger Initiated","");
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            if (!dir.exists()) {
                Log.d("Dir created ", "Dir created ");
                dir.mkdirs();
            }
            File[] foundFiles = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {

                    return name.startsWith("MapDisarm_Log_");
                }
            });

          //  Log.v("Found Files starting with MapDisarm_Log:", foundFiles.length + "");

            if (foundFiles != null && foundFiles.length > 0) {
                logFile = new File(foundFiles[0].toString());
                Log.v("LogFile:", foundFiles[0].toString());
            }

        }
        String latlng;
        try {
            String[] latlongline = ReadLastLine.tail(logFile).split(",");
            latlng = latlongline[0]+"_"+latlongline[1];
        } catch (Exception e) {
            latlng = "10000.0000_10000.0000";
        }

        String pathFrom = Environment.getExternalStorageDirectory().toString()+"/DMS/tmp";
        String pathTo = Environment.getExternalStorageDirectory().toString()+"/DMS/Working";
        Log.d("Files", "Path: " + pathFrom);
        File f = new File(pathFrom);
        File file[] = f.listFiles();
        Log.d("No. Files", String.valueOf(file.length));
        if (file.length>0){
            increaseSession = true;
        }
        for (int i=0; i < file.length; i++)
        {
            changeMetaData(file[i]);
            fileName = file[i].getName().split("_");
            fileType = fileName[0];
            source = fileName[1];
            groupType = fileName[2];
            timestamp = fileName[3];
            fileFormat = fileName[4];

            groupID = idNumber;
            Log.v("FIleNames",fileType+ttl+groupType+source+dest+latlng+timestamp+groupID);
            File from = new File(pathFrom,file[i].getName());
            String acutalFileName = fileType+"_"+
                                    ttl+"_"+
                                    groupType+"_"+
                                    source+"_"+
                                    dest+"_"+
                                    latlng+"_"+
                                    timestamp+"_"+
                                    groupID+
                                    fileFormat;
            File to = new File(pathTo,acutalFileName);
            from.renameTo(to);

        }
        return null;
    }
    void changeMetaData(File file)
    {

        Bundle extraVals = new Bundle();

        extraVals.putString("Accelometer","22.23");
        extraVals.putString("Gyroscope","100.01");
        Log.v("Sensor 1 ","working");
        try {
            // Write to file
            saveExif(file.toString(), extraVals);
            Bundle vals = getImageAttributes(file.toString());
            Log.v("Sensors 2: ", vals.toString());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        if (increaseSession) {
            idNumber += 1;
            editor.putInt(GROUPID, idNumber);
            editor.commit();
        }
    }
}
