package com.disarm.sanna.pdm.Capture;

import android.app.Activity;
import android.content.Intent;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Created by Sanna on 21-06-2016.
 */
public class Photo extends Activity {

    File createImages;
    static String root = Environment.getExternalStorageDirectory().toString();
    static String path =root + "/" + "DMS" + "/" + "tmp",group,type,groupID;
    private Uri fileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent myIntent = getIntent();
        type = myIntent.getStringExtra("IntentType");
        captureImage();
        Photo.this.finish();
    }

    public void captureImage(){
        createImages = new File(path);
        if (!createImages.exists())
            createImages.mkdir();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        fileUri = getOutputMediaFileUri();// create a file to save the image
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
        // start the image capture Intent
        startActivity(intent);

    }

    private Uri getOutputMediaFileUri() {
        return Uri.fromFile(getOutputMediaFile());
    }


    private File getOutputMediaFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        File mediaFile;
        group = type;
        groupID = "1";
        mediaFile = new File(path, "IMG_" + group + "_" + timeStamp + "_" + ".jpg");
        return mediaFile;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
