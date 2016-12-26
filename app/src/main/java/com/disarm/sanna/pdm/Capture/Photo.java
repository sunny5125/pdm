package com.disarm.sanna.pdm.Capture;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.BuildConfig;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import com.disarm.sanna.pdm.MainActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import static Sensors.readWriteMetaData.getImageAttributes;
import static Sensors.readWriteMetaData.saveExif;
import static com.disarm.sanna.pdm.R.id.imageView;


/**
 * Created by Sanna on 21-06-2016.
 */
public class Photo extends Activity {

    File createImages;
    static String root = Environment.getExternalStorageDirectory().toString();
    static String path =root + "/" + "DMS" + "/" + "tmp",group,type,groupID;
    private Uri fileUri;
    private Uri outputFileUri;
    String mCurrentPhotoPath;
    private static final int SELECT_PICTURE_CAMARA = 101, SELECT_PICTURE = 201, CROP_IMAGE = 301;
    private File mediaFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent myIntent = getIntent();
        type = myIntent.getStringExtra("IntentType");
        TakeImage();
        Photo.this.finish();

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

                Uri photoURI;
                if (Build.VERSION.SDK_INT >= 24) {
                    photoURI = FileProvider.getUriForFile(Photo.this,
                            getApplicationContext().getPackageName() + ".provider", photoFile);
                } else {
                    photoURI = Uri.fromFile(photoFile);
                }

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, SELECT_PICTURE_CAMARA);


               // File imgFile = new File(Environment.getExternalStorageDirectory() + "/DMS/Working/IMG_50_Health_9635547701_defaultMcs_10000.0000_10000.0000_20161220031532_0.jpg");

            }
        }
    }
   /* private Uri getOutputMediaFileUri() {
        return Uri.fromFile(getOutputMediaFile());
    }*/


    private File getOutputMediaFile() throws IOException{
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        group = type;
        groupID = "1";
        mediaFile = new File(path, "IMG_" + MainActivity.phoneVal + "_" + group + "_" + timeStamp + "_" + ".jpg");
        return mediaFile;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
        Log.v("camera","check1");
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
