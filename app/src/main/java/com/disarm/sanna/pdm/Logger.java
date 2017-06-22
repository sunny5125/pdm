package com.disarm.sanna.pdm;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.FileHandler;

import static com.disarm.sanna.pdm.SelectCategoryActivity.SOURCE_PHONE_NO;

public class Logger {

    private static String filename = "MapDisarm_Log", logFileUpdated;
    public static File logFile;
    static boolean isExternalStorageAvailable = false;
    static boolean isExternalStorageWriteable = false;
    static String state = Environment.getExternalStorageState();


    public static void addRecordToLog(String message) {

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            isExternalStorageAvailable = isExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            isExternalStorageAvailable = true;
            isExternalStorageWriteable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            isExternalStorageAvailable = isExternalStorageWriteable = false;
        }

        // Log File Updated with every new entry for onLocationChanged()
        File dir = Environment.getExternalStoragePublicDirectory("DMS/Working");
        Log.v("Logger Initiated","");
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            if(!dir.exists()) {
                Log.d("Dir created ", "Dir created ");
                dir.mkdirs();
            }
            File[] foundFiles = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {

                    return name.startsWith("MapDisarm_Log_" + SOURCE_PHONE_NO);
                }
            });

            Log.v("Found Files starting with MapDisarm_Log:" , foundFiles.length+"");

            if(foundFiles != null && foundFiles.length > 0)
            {
                logFile = new File(foundFiles[0].toString());
                Log.v("LogFile:", foundFiles[0].toString());
            }
            else {
                DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
                String date = df.format(Calendar.getInstance().getTime());
                logFile = new File(dir, filename + "_" + SOURCE_PHONE_NO + "_" + date + ".txt");
                if (!logFile.exists()) {
                    try {
                        Log.d("File created ", "File created ");
                        logFile.createNewFile();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            try {
                Log.v("log file check :", logFile.toString());
                FileWriter buf = new FileWriter(logFile, true);
                DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
                String date = df.format(Calendar.getInstance().getTime());
                buf.write(message+','+date +"\r\n");

                buf.flush();
                buf.close();


                logFileUpdated = logFile.toString().substring(0, logFile.toString().length()-19);

                Log.v("Phone ID: ", SOURCE_PHONE_NO);
                Log.v("Log File:",logFile.toString());
                File to = new File(logFileUpdated.toString() + "_" + date + ".txt");
                Log.v("Log File Updated:",logFileUpdated.toString()+ "_" + date + ".txt");

                boolean success = logFile.renameTo(to);

                Log.v("Success:",String.valueOf(success));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
