package com.disarm.sanna.pdm.Capture;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
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
    private int SELECT_VIDEO_CAMARA = 101;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent myIntent = getIntent();
        type = myIntent.getStringExtra("IntentType");
        TakeVideo();
        Video.this.finish();
    }

  /*  private void captureVideo() {
        createImages = new File(path);
        if (!createImages.exists())
            createImages.mkdir();
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        fileUri = getOutputMediaFileUri();// create a file to save the image
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
        // start the image capture Intent
        startActivity(intent);
    }*/
    
    void TakeVideo(){
            Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

            if (takeVideoIntent.resolveActivity(getApplicationContext().getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = getOutputMediaFile();
                } catch (IOException ex) {
                }
                if (photoFile != null) {

                    Uri photoURI;
                    if (Build.VERSION.SDK_INT >= 24) {
                        photoURI = FileProvider.getUriForFile(Video.this,
                                getApplicationContext().getPackageName() + ".provider", photoFile);
                    } else {
                        photoURI = Uri.fromFile(photoFile);
                    }

                    takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY,0);
                    startActivityForResult(takeVideoIntent, SELECT_VIDEO_CAMARA);
                }
            }
        }

 /*   private Uri getOutputMediaFileUri() {
        return Uri.fromFile(getOutputMediaFile());
    }
*/

    private File getOutputMediaFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        group = type;
        groupID = "1";
        mediaFile = new File(path, "VID_" + group + "_" + timeStamp + "_" + ".3gp");

        return mediaFile;
    }

}
