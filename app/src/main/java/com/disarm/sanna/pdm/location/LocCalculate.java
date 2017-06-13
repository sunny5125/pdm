package com.disarm.sanna.pdm.location;

/**
 * Created by sanna on 2/9/17.
 */

public class LocCalculate {

    // Haversing method implementation in java
    public static double calculateGPSDistance(String slat1, String slong1, double lat2, double lon2)
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

    public static double bearing(String slat1, String slong1, double lat2, double lon2)
    {
        double lat1 = Double.parseDouble(slat1);
        double long1 = Double.parseDouble(slong1);

        double longDiff= lon2-long1;
        double y = Math.sin(longDiff)*Math.cos(lat2);
        double x = Math.cos(lat1)*Math.sin(lat2)-Math.sin(lat1)*Math.cos(lat2)*Math.cos(longDiff);

        double bearingValue = ( Math.toDegrees(Math.atan2(y, x)) + 360 ) % 360;

        return bearingValue;
    }
}
