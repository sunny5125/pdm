package com.disarm.sanna.pdm;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.disarm.sanna.pdm.DisarmConnect.*;
import com.disarm.sanna.pdm.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import static com.disarm.sanna.pdm.DisarmConnect.Logger.logger;

/**
 * Created by hridoy on 4/11/16.
 */
public class Emulator {
    String peerID;
    EmulatorThread emulatorThread;
    Thread thread;
    List<String> gpsLogs;
    Logger logger;
    long dateDiff = 0;

    public Emulator(String peerID) {
        this.peerID = peerID;
        this.emulatorThread = new EmulatorThread(peerID);
        gpsLogs = new ArrayList<>();
        logger = new Logger(peerID);

        // read GPS log emulation file
        File gpsFile = new File(MainActivity.TARGET_DMS_PATH, "emulation/emu_gps_" + peerID + ".txt");
        try {
            FileReader gpsFileReader = new FileReader(gpsFile);
            BufferedReader gpsBuffer = new BufferedReader(gpsFileReader);
            String line;
            while ((line = gpsBuffer.readLine()) != null) {
                gpsLogs.add(line);
            }
            Date firstLogDate = new SimpleDateFormat("yyyyMMddHHmmss").parse(gpsLogs.get(0).split(",")[5]);
            Date nowDate = new Date();
            dateDiff = nowDate.getTime() - firstLogDate.getTime();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void startEmulator() {
        if (!emulatorThread.isRunning) {
            thread = new Thread(emulatorThread);
            thread.start();
        }
    }

    public void stopEmulator() {
        if (emulatorThread.isRunning) {
            emulatorThread.stop();
        }
    }


    class EmulatorThread implements Runnable {
        volatile boolean exit;
        volatile boolean isRunning;

        public EmulatorThread(String peerID) {
            this.exit = false;
            this.isRunning = false;
        }


        @Override
        public void run() {
            try {
                this.isRunning = true;
                int i = 0;
                while (!this.exit) {
                    if (i < gpsLogs.size()) {
                        Date logDate = new SimpleDateFormat("yyyyMMddHHmmss").parse(gpsLogs.get(i).split(",")[5]);
                        if(logDate.getTime() < (new Date().getTime() - dateDiff)){
                            Log.v("Emulator:", gpsLogs.get(i));
                            writeToGpsTrail(gpsLogs.get(i));
                            i++;
                        }
                    }
                    Thread.sleep(1000);
                    Log.v("Emulator:", " Thread working :)");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            } finally {

            }
            this.exit = false;
            this.isRunning = false;

            //logger.d("DEBUG", "Broadcasting Stopped");
        }

        private void writeToGpsTrail(String logLine) {


            // Calculate from GPS
            File inFolder = Environment.getExternalStoragePublicDirectory("DMS/Working/");
            File[] foundFiles = inFolder.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {

                    return name.startsWith("MapDisarm_Log_" + peerID);
                }
            });
            String latitude, longitude, speed, distance, bearing;
            latitude = logLine.split(",")[0];
            longitude = logLine.split(",")[1];
            speed = logLine.split(",")[2];
            distance = logLine.split(",")[3];
            bearing = logLine.split(",")[4];
            if(foundFiles != null && foundFiles.length > 0) {
                File logFile = new File(foundFiles[0].toString());
                FileInputStream in = null;
                BufferedReader  br;
                Log.v("LogFile:", foundFiles[0].toString());
                try {
                    in = new FileInputStream(logFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                String lastLine = "";

                br = new BufferedReader(new InputStreamReader(in));
                String sCurrentLine;
                try {
                    while ((sCurrentLine = br.readLine()) != null) {
                        lastLine = sCurrentLine;
                    }
                } catch (Exception e) {
                }


                // Calculate Distance between 2 GPS coordinates
                logger.addRecordToLog(String.valueOf(latitude) + "," + String.valueOf(longitude) + "," + String.valueOf(speed) + "," + String.valueOf(distance) + "," + String.valueOf(bearing));


            } else {
                logger.addRecordToLog(String.valueOf(latitude) + "," + String.valueOf(longitude) + "," + String.valueOf(speed) + "," + String.valueOf(distance) + "," + String.valueOf(bearing));

            }


        }

        public void stop() {
            this.exit = true;
        }
    }
}
