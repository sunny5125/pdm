package Sensors;

import android.location.Location;
import android.media.ExifInterface;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Set;

/**
 * Created by hridoy on 26/12/16.
 */

public class readWriteMetaData {

    public static Bundle getImageAttributes(String fileNameWithPath) {
        Bundle exifBundle = new Bundle();
        try {
            String latitudeRef, longitudeRef,accVal;
            Float latitude, longitude;
            ExifInterface exifInterface = new ExifInterface(fileNameWithPath);
            latitudeRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            accVal = exifInterface.getAttribute(ExifInterface.TAG_USER_COMMENT);
            Log.v("Accval",accVal);

            exifBundle.putString(ExifInterface.TAG_IMAGE_LENGTH, exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH));
            exifBundle.putString(ExifInterface.TAG_IMAGE_WIDTH, exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH));
            exifBundle.putString(ExifInterface.TAG_DATETIME, exifInterface.getAttribute(ExifInterface.TAG_DATETIME));
            exifBundle.putString(ExifInterface.TAG_MAKE, exifInterface.getAttribute(ExifInterface.TAG_MAKE));
            exifBundle.putString(ExifInterface.TAG_MODEL, exifInterface.getAttribute(ExifInterface.TAG_MODEL));
            exifBundle.putString(ExifInterface.TAG_ORIENTATION, exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION));
            exifBundle.putString(ExifInterface.TAG_WHITE_BALANCE, exifInterface.getAttribute(ExifInterface.TAG_WHITE_BALANCE));
            exifBundle.putString(ExifInterface.TAG_FOCAL_LENGTH, exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH));
            exifBundle.putString(ExifInterface.TAG_FLASH, exifInterface.getAttribute(ExifInterface.TAG_FLASH));
            exifBundle.putString(ExifInterface.TAG_GPS_PROCESSING_METHOD, exifInterface.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD));
            exifBundle.putString(ExifInterface.TAG_GPS_DATESTAMP, exifInterface.getAttribute(ExifInterface.TAG_GPS_DATESTAMP));
            exifBundle.putString(ExifInterface.TAG_GPS_TIMESTAMP, exifInterface.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP));

        } catch (IOException e) {
            e.printStackTrace();
        }
        return exifBundle;
    }
    private static String convertToDegreeMinuteSeconds(double latitude) {
        latitude = Math.abs(latitude);
        int degree = (int) latitude;
        latitude *= 60;
        latitude -= (degree * 60.0d);
        int minute = (int) latitude;
        latitude *= 60;
        latitude -= (minute * 60.0d);
        int second = (int) (latitude * 1000.0d);
        StringBuilder sb = new StringBuilder();
        sb.append(degree);
        sb.append("/1,");
        sb.append(minute);
        sb.append("/1,");
        sb.append(second);
        sb.append("/1000,");
        return sb.toString();
    }
    private static Float convertFromDegreeMinuteSeconds(String stringDMS) {
        Float result = null;
        String[] DMS = stringDMS.split(",", 3);
        String[] stringD = DMS[0].split("/", 2);
        Double D0 = Double.valueOf(stringD[0]);
        Double D1 = Double.valueOf(stringD[1]);
        Double FloatD = D0 / D1;
        String[] stringM = DMS[1].split("/", 2);
        Double M0 = Double.valueOf(stringM[0]);
        Double M1 = Double.valueOf(stringM[1]);
        Double FloatM = M0 / M1;
        String[] stringS = DMS[2].split("/", 2);
        Double S0 = Double.valueOf(stringS[0]);
        Double S1 = Double.valueOf(stringS[1]);
        Double FloatS = S0 / S1;
        result = (float) (FloatD + (FloatM / 60) + (FloatS / 3600));
        return result;
    }
    public static void saveExif(String fileNameWithPath, Bundle extraInfo) throws IOException {
        if (extraInfo != null) {
            ExifInterface exif = new ExifInterface(fileNameWithPath);
            for (String key : extraInfo.keySet()) {
                Object obj = extraInfo.get(key);
                if (obj instanceof Location) {
                    Location location = (Location) obj;
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convertToDegreeMinuteSeconds(location.getLatitude()));
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, getLatitudeRef(location.getLatitude()));
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convertToDegreeMinuteSeconds(location.getLongitude()));
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, getLongitudeRef(location.getLongitude()));
                    break;
                }
            }
            JSONObject json = new JSONObject();
            Set<String> keys = extraInfo.keySet();
            for (String key : keys) {
                try {
                    json.put(key, extraInfo.get(key));
                } catch (JSONException e) {
                    //Handle exception here
                }
            }
            Log.v("UserComment",json.toString());
            exif.setAttribute("UserComment", json.toString());
            exif.saveAttributes();
        }
    }
    /**
     * returns ref for latitude which is S or N.
     *
     * @param latitude
     * @return S or N
     */
    private static String getLatitudeRef(double latitude) {
        return latitude < 0.0d ? "S" : "N";
    }
    /**
     * returns ref for latitude which is S or N.
     *
     * @param longitude
     * @return W or E
     */
    private static String getLongitudeRef(double longitude) {
        return longitude < 0.0d ? "W" : "E";
    }
    /**
     * convert latitude into DMS (degree minute second) format. For instance<br/>
     * -79.948862 becomes<br/>
     * 79/1,56/1,55903/1000<br/>
     * It works for latitude and longitude<br/>
     *
     * @param latitude could be longitude.
     * @return
     */

}
