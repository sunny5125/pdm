package com.disarm.surakshit.pdm;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.disarm.surakshit.pdm.Util.Params;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

//First Activity
public class SplashActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int PERMISSION_ALL = 1;

    public File dmsFolder = Environment.getExternalStoragePublicDirectory("DMS/");
    public File workingFolder = Environment.getExternalStoragePublicDirectory("DMS/Working");
    public File mapFolder = Environment.getExternalStoragePublicDirectory("DMS/Map");
    public File tempMedia = Environment.getExternalStoragePublicDirectory("DMS/tempMedia");
    public File tempFolder = Environment.getExternalStoragePublicDirectory("DMS/temp");
    public File SurakshitImages = Environment.getExternalStoragePublicDirectory("DMS/Working/SurakshitImages");
    public File SurakshitVideos = Environment.getExternalStoragePublicDirectory("DMS/Working/SurakshitVideos");
    public File SurakshitAudio = Environment.getExternalStoragePublicDirectory("DMS/Working/SurakshitAudio");
    public File SurakshitMap = Environment.getExternalStoragePublicDirectory("DMS/Working/SurakshitMap");
    public File SurakshitKml = Environment.getExternalStoragePublicDirectory("DMS/Working/SurakshitKml");
    public File SurakshitDiff = Environment.getExternalStoragePublicDirectory("DMS/Working/SurakshitDiff");
    public File Kml = Environment.getExternalStoragePublicDirectory("DMS/KML");
    public File Source = Environment.getExternalStoragePublicDirectory("DMS/KML/Source");
    public File Dest = Environment.getExternalStoragePublicDirectory("DMS/KML/Dest");
    public File LatestSource = Environment.getExternalStoragePublicDirectory("DMS/KML/Source/LatestKml");
    public File SourceSourceKml = Environment.getExternalStoragePublicDirectory("DMS/KML/Source/SourceKml");
    public File LatestDest = Environment.getExternalStoragePublicDirectory("DMS/KML/Dest/LatestKml");
    public File DestSourceKml = Environment.getExternalStoragePublicDirectory("DMS/KML/Dest/SourceKml");
    public File tempDecrypt = Environment.getExternalStoragePublicDirectory("DMS/tempDecrypt");

    private EditText phoneText1;
    private Button submitButton;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    //Adding new param
    public static String ROOT                             = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static String DMS_PATH                         = ROOT + "/DMS/";


    String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = getSharedPreferences("Surakshit",MODE_PRIVATE);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkPermissions(this, PERMISSIONS)) {
                Log.i("permission","request permissions");
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            }
        }
        else{
            creatingFolders();
        }

        if (sp.getString("phone_no",null)!=null){
            Params.SOURCE_PHONE_NO = sp.getString("phone_no",null);
            Intent i = new Intent(this,RegisterActivity.class);
            startActivity(i);
            finish();
        }

        else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setContentView(R.layout.activity_splash);
            submitButton = (Button) findViewById(R.id.submitButton);
            phoneText1 = (EditText) findViewById(R.id.phoneText);
            submitButton.setOnClickListener( this);
        }
    }

    private void creatingFolders(){
        if (!dmsFolder.exists()){
            dmsFolder.mkdir();
        }
        if (!workingFolder.exists()){
            workingFolder.mkdir();
        }
        if (!mapFolder.exists()){
            mapFolder.mkdir();
        }
        if(!SurakshitAudio.exists()){
            SurakshitAudio.mkdir();
        }
        if(!SurakshitDiff.exists()){
            SurakshitDiff.mkdir();
        }
        if(!SurakshitImages.exists()){
            SurakshitImages.mkdir();
        }
        if(!SurakshitMap.exists()){
            SurakshitMap.mkdir();
        }
        if(!SurakshitVideos.exists()){
            SurakshitVideos.mkdir();
        }
        if(!SurakshitKml.exists()){
            SurakshitKml.mkdir();
        }
        if(!Kml.exists()){
            Kml.mkdir();
        }
        if(!Source.exists()){
            Source.mkdir();
        }
        if(!Dest.exists()){
            Dest.mkdir();
        }
        if(!LatestSource.exists()){
            LatestSource.mkdir();
        }
        if(!SourceSourceKml.exists()){
            SourceSourceKml.mkdir();
        }
        if(!DestSourceKml.exists()){
            DestSourceKml.mkdir();
        }
        if(!LatestDest.exists()){
            LatestDest.mkdir();
        }
        if(!tempFolder.exists()){
            tempFolder.mkdir();
        }
        if(!tempMedia.exists()){
            tempMedia.mkdir();
        }
        if(!tempDecrypt.exists()){
            tempDecrypt.mkdir();
        }
        copyAssets();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.submitButton){
            final String phoneTextVal = phoneText1.getText().toString();

            if(phoneTextVal.length() == 10 && phoneTextVal.matches("^[789]\\d{9}$")) {
                editor = sp.edit();
                editor.putString("phone_no",phoneTextVal);
                editor.apply();
                Params.SOURCE_PHONE_NO = phoneTextVal;
                Intent i = new Intent(this,RegisterActivity.class);
                startActivity(i);
                finish();
            }
            else
            {
                phoneText1.setError("Enter Valid Number");
            }
        }
    }

    private boolean checkPermissions(Context context, String[] permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ALL: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    creatingFolders();

                }
                else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void copyAssets() {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e("tag", "Failed to get asset file list.", e);
        }
        if (files != null) for (String filename : files) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = assetManager.open(filename);
                File outDir = Environment.getExternalStoragePublicDirectory("DMS/Working/pgpKey");
                outDir.mkdirs();
                File outFile = Environment.getExternalStoragePublicDirectory("DMS/Working/pgpKey/"+filename);
                //File outFile = new File(getExternalFilesDir(null), filename);
                out = new FileOutputStream(outFile);
                copyFile(in, out);
            } catch(IOException e) {
                Log.e("tag", "Failed to copy asset file: " + filename, e);
            }
            finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // NOOP
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // NOOP
                    }
                }
            }
        }
    }
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }
}
