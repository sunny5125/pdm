package com.disarm.sanna.pdm.Capture;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Sanna on 22-06-2016.
 */
public class Video extends AppCompatActivity {
    File createImages;
    static String root = Environment.getExternalStorageDirectory().toString();
    static String path =root + "/" + "DMS" + "/" + "tmp",group,type,groupID;
    private Uri fileUri;
    File mediaFile;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent myIntent = getIntent();
        type = myIntent.getStringExtra("IntentType");
        captureVideo();
        Video.this.finish();
    }

    private void captureVideo() {
        createImages = new File(path);
        if (!createImages.exists())
            createImages.mkdir();
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        fileUri = getOutputMediaFileUri();// create a file to save the image
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
        // start the image capture Intent
        startActivity(intent);
    }
    private Uri getOutputMediaFileUri() {
        return Uri.fromFile(getOutputMediaFile());
    }


    private File getOutputMediaFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        group = type;
        groupID = "1";
        mediaFile = new File(path, "VID_" + group + "_" + timeStamp + "_" + ".3gp");

        return mediaFile;
    }

}
