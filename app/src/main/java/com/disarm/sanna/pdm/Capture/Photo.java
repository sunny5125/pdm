package com.disarm.sanna.pdm.Capture;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.BuildConfig;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.disarm.sanna.pdm.MainActivity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import Sensors.getSensorsData;

import static Sensors.readWriteMetaData.getImageAttributes;
import static Sensors.readWriteMetaData.saveExif;


/**
 * Created by Sanna on 21-06-2016.
 */
public class Photo extends Activity implements SensorEventListener {

    File createImages;
    static String root = Environment.getExternalStorageDirectory().toString();
    static String path = root + "/" + "DMS" + "/" + "tmp", group, type, groupID;
    private Uri fileUri;
    private Uri outputFileUri;
    String mCurrentPhotoPath;
    private static final int SELECT_PICTURE_CAMARA = 101, SELECT_PICTURE = 201, CROP_IMAGE = 301;
    private File mediaFile;
    private SensorManager sensorManager;
    private double accelerometerX,accelerometerY,accelerometerZ;

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
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
        String sensorName = sensorEvent.sensor.getName();
        Log.v(sensorName, ": X: " + sensorEvent.values[0] + "; Y: " + sensorEvent.values[1] + "; Z: " + sensorEvent.values[2] + ";");
        accelerometerX = sensorEvent.values[0];
        accelerometerY = sensorEvent.values[1];
        accelerometerZ = sensorEvent.values[2];

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        Intent myIntent = getIntent();
        type = myIntent.getStringExtra("IntentType");
        TakeImage();

        // REmove this else onActivityResult not working
        // Photo.this.finish();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_PICTURE_CAMARA && resultCode == RESULT_OK) {
            Log.v("Camera is working", String.valueOf(requestCode) + String.valueOf(resultCode));

            changeMetaData(mediaFile);
        }
    }

    void TakeImage() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getApplicationContext().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = getOutputMediaFile();
            } catch (IOException ex) {
            }
            if (photoFile != null) {

                Log.v("PhotoFile:", photoFile.toString());
                Uri photoURI;
                if (Build.VERSION.SDK_INT >= 24) {
                    photoURI = FileProvider.getUriForFile(Photo.this,
                            getApplicationContext().getPackageName() + ".provider", photoFile);
                } else {
                    photoURI = Uri.fromFile(photoFile);
                }

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, SELECT_PICTURE_CAMARA);
            }
        }
    }

    void changeMetaData(File file) {

        Bundle extraVals = new Bundle();

        extraVals.putString("Gyroscope", "22.23");
        Log.v("Acceloremeter: ", "Locked Acceloremeter" + String.valueOf(accelerometerX) + "," + String.valueOf(accelerometerY) +"," +String.valueOf(accelerometerZ));
        extraVals.putString("Accelerometer",  String.valueOf(accelerometerX) + "," + String.valueOf(accelerometerY) +"," +String.valueOf(accelerometerZ));

        try {
            Log.v("FilePath:", file.toString());
            // Write to file
            saveExif(file.toString(), extraVals);
            Bundle vals = getImageAttributes(file.toString());
            Log.v("Sensors 2: ", vals.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private File getOutputMediaFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        group = type;
        groupID = "1";
        mediaFile = new File(path, "IMG_" + MainActivity.phoneVal + "_" + group + "_" + timeStamp + "_" + ".jpg");
        // populateSensorsMap();
        return mediaFile;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }
    private void cropImage(Uri selectedImageUri) {
        Intent cropIntent = new Intent("com.android.camera.action.CROP");

        cropIntent.setDataAndType(selectedImageUri, "image/*");

        cropIntent.putExtra("crop", "true");
        cropIntent.putExtra("aspectX", 1);
        cropIntent.putExtra("aspectY", 1.5);
        cropIntent.putExtra("return-data", true);

        outputFileUri = Uri.fromFile(createCropFile());

        cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        startActivityForResult(cropIntent, CROP_IMAGE);
        Log.v("camera", "check1");
    }

    private File createCropFile() {
        File storageDir = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //  path = path + (timeStamp + "1jpg");
        File file = null;
        try {
            file = File.createTempFile(timeStamp, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCurrentPhotoPath = String.valueOf(Uri.fromFile(file));
        return file;
    }

}
