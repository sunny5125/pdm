package Sensors;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;


import static android.content.Context.SENSOR_SERVICE;
import static android.hardware.Sensor.TYPE_GYROSCOPE;

/**
 * Created by hridoy on 26/12/16.
 */

public class getSensorsData implements SensorEventListener {
    private final SensorManager mSensorManager;
    private final Sensor mAccelerometer;
   // private final Sensor mGyroscope;

    public getSensorsData(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
       // mGyroscope = mSensorManager.getDefaultSensor(TYPE_GYROSCOPE);
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.v("Gyroscope Working:"," :-)");
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
            switch (accuracy) {
                case 0:
                    System.out.println("Unreliable");
                    break;
                case 1:
                    System.out.println("Low Accuracy");
                    break;
                case 2:
                    System.out.println("Medium Accuracy");

                    break;
                case 3:
                    System.out.println("High Accuracy");
                    break;
            }
        }


    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.v("Gyroscope Working 2:"," :-)");
        String sensorName = sensorEvent.sensor.getName();
        Log.v(sensorName, ": X: " + sensorEvent.values[0] + "; Y: " + sensorEvent.values[1] + "; Z: " + sensorEvent.values[2] + ";");
    }

}