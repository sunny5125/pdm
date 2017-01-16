package com.disarm.sanna.pdm;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.InputStreamReader;

public class MyLocationListener extends SurakshitActivity implements LocationListener {
    Logger logger;
    String sCurrentLine;
    BufferedReader br;
    FileInputStream in;
    String phoneVal;
    File inFolder, logFile;
    Double distance = 0.0, bearing = 0.0;


    public MyLocationListener(Logger logger1, String phoneVal1) {
        this.logger = logger1;
        this.phoneVal = phoneVal1;

    }
    @Override
    public void onLocationChanged(Location locFromGps) {
        Log.v("onLocationChanged"," Working");

        // called when the listener is notified with a location update from the GPS
        longitude = locFromGps.getLongitude();
        latitude = locFromGps.getLatitude();
        speed=locFromGps.getSpeed();

        // Calculate from GPS
        inFolder = Environment.getExternalStoragePublicDirectory("DMS/Working/");
        File[] foundFiles = inFolder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {

                return name.startsWith("MapDisarm_Log_" + phoneVal);
            }
        });
        if(foundFiles != null && foundFiles.length > 0) {
            logFile = new File(foundFiles[0].toString());
            Log.v("LogFile:", foundFiles[0].toString());
            try {
                in = new FileInputStream(logFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            String lastLine = "";

            br = new BufferedReader(new InputStreamReader(in));
            try {
                while ((sCurrentLine = br.readLine()) != null) {
                    lastLine = sCurrentLine;
                }
            } catch (Exception e) {
            }
            Log.v("LastLine:", lastLine);
            Log.v("LastLine Location New:", "Lat:" + latitude + "Long:" + longitude);

            // Calculate Distance between 2 GPS coordinates
            distance = calculateGPSDistance(lastLine.split(",")[0], lastLine.split(",")[1], latitude, longitude);
            Log.v("Distance :", distance.toString());

            if (distance.doubleValue() > 0.00) {
                bearing = bearing(lastLine.split(",")[0], lastLine.split(",")[1], latitude, longitude);

                if (bearing > 90.00 && bearing < 270.00) {
                    bearing = Math.abs(180.00 - bearing);
                } else if (bearing > 270.00) {
                    bearing = Math.abs(360.00 - bearing);
                }
                Log.v("Bearing:", bearing.toString());

                if (latitude != 0.0 && longitude != 0.0 && bearing.doubleValue() > 0.00) {
                    logger.addRecordToLog(String.valueOf(latitude) + "," + String.valueOf(longitude) + "," + String.valueOf(speed) + "," + String.valueOf(distance) + "," + String.valueOf(bearing));
                }
                Log.v("Speed :", Float.toString(speed));

            }

        }
        else {
            logger.addRecordToLog(String.valueOf(latitude) + "," + String.valueOf(longitude) + "," + String.valueOf(speed) + "," + String.valueOf(distance) + "," + String.valueOf(bearing));

        }
    }
    // Haversing method implementation in java
    public double calculateGPSDistance(String slat1,String slong1,double lat2, double lon2)
    {
        double lat1 = Double.parseDouble(slat1);
        double lon1 = Double.parseDouble(slong1);
        final int R = 6371; // Radius of the earth

        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = 0.0;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }
    public double bearing(String slat1, String slong1, double lat2, double lon2)
    {
        double lat1 = Double.parseDouble(slat1);
        double long1 = Double.parseDouble(slong1);

        double longDiff= lon2-long1;
        double y = Math.sin(longDiff)*Math.cos(lat2);
        double x = Math.cos(lat1)*Math.sin(lat2)-Math.sin(lat1)*Math.cos(lat2)*Math.cos(longDiff);

        double bearingValue = ( Math.toDegrees(Math.atan2(y, x)) + 360 ) % 360;

        return bearingValue;
    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
