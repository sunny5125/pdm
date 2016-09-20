package com.disarm.sanna.pdm;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;


import com.disarm.sanna.pdm.Util.CopyAssets;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created by disarm on 11/7/16.
 */
public class SplashActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int PERMISSION_ALL = 1;
    public File dmsFolder = Environment.getExternalStoragePublicDirectory("DMS/");
    public File workingFolder = Environment.getExternalStoragePublicDirectory("DMS/Working");
    public File tmpFolder = Environment.getExternalStoragePublicDirectory("DMS/tmp");
    public File mapFolder = Environment.getExternalStoragePublicDirectory("DMS/Map");
    final File configFile = new File(dmsFolder,"source.txt");
    private ProgressDialog progress;
    private EditText phoneText1;
    private Button submitButton;
    private Spinner spinner2;
    boolean isAllFolderExit = false;
    public Locale myLocale;
    private String definedlanguage ;
    public static final String Lang = "language";
    String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);
        submitButton = (Button) findViewById(R.id.submitButton);
        phoneText1 = (EditText) findViewById(R.id.phoneText);
        spinner2 = (Spinner)findViewById(R.id.language);
        spinner2.setVisibility(View.GONE);
        submitButton.setVisibility(View.GONE);
        phoneText1.setVisibility(View.GONE);
        phoneText1.setCursorVisible(false);
        progress=new ProgressDialog(this);
        progress.setMessage("Checking All Configuration");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.show();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkPermissions(this, PERMISSIONS)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            }
        }else{
            afterPermissionExecute();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        definedlanguage = prefs.getString(Lang,"");
        setLocale(definedlanguage);

        submitButton.setOnClickListener( this);

        String[] langArray = getResources().getStringArray(R.array.lang_list);
        List<String> lang = new ArrayList<String>(Arrays.asList(langArray));
        ArrayAdapter<String> adapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item, lang);
        adapter.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);
        spinner2.setAdapter(adapter);

        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SplashActivity.this);
                SharedPreferences.Editor editor = prefs.edit();
                if (pos == 0) {
                    setLocale("en");
                    editor.putString(Lang,"en");
                    editor.commit();
                } else if (pos == 1) {
                    setLocale("bn");
                    editor.putString(Lang,"bn");
                    editor.commit();
                } else if (pos == 2) {
                    setLocale("hi");
                    editor.putString(Lang,"hi");
                    editor.commit();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void afterPermissionExecute(){
        if (!dmsFolder.exists()){
            dmsFolder.mkdir();
        }if (!workingFolder.exists()){
            workingFolder.mkdir();
        }if (!tmpFolder.exists()){
            tmpFolder.mkdir();
        }if (!mapFolder.exists()){
            mapFolder.mkdir();
        }
        //checkAllFolder();
        copyingAssets();

        if (checkSourceFile()){
            progress.setMessage("Source : OK");
            progress.dismiss();
            callWriteSettingActivity();
        }else {
            phoneText1.setVisibility(View.VISIBLE);
            submitButton.setVisibility(View.VISIBLE);
            spinner2.setVisibility(View.VISIBLE);
            copyingAssets();
            progress.dismiss();
        }

    }

    private void checkAllFolder() {
        if (!dmsFolder.exists() && !workingFolder.exists() && !tmpFolder.exists() &&!mapFolder.exists()) {
            Log.v("File","creating files");
            dmsFolder.mkdir();
            workingFolder.mkdir();
            tmpFolder.mkdir();
            mapFolder.mkdir();
            isAllFolderExit = true;
        }
    }

    private boolean checkSourceFile() {
        boolean isSourceExit = false;
        if (configFile.exists()) {
            // TODO Auto-generated method stub
            isSourceExit = true;
        }
        return isSourceExit;
    }

    private void callWriteSettingActivity(){
        Intent intent = new Intent(this,WriteSettingActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.submitButton){
            final String phoneTextVal = phoneText1.getText().toString();

            if(phoneTextVal.length() == 10 && phoneTextVal.matches("^[789]\\d{9}$")) {
                if (!configFile.exists())  {
                    try  {
                        Log.d("source File created ", " source File created ");
                        configFile.createNewFile();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                try {
                    BufferedWriter buf = new BufferedWriter(new FileWriter(configFile, true));
                    buf.write(phoneTextVal);
                    buf.flush();
                    buf.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                callWriteSettingActivity();
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

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ALL: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    afterPermissionExecute();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void copyingAssets(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("firstTime", false)) {
            // <---- run your one time code here
            // Copy files from assets folder

            CopyAssets copy = new CopyAssets(this);
            copy.copyFileOrDir("");

            // mark first time has runned.
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("firstTime", true);
            editor.commit();
        }
    }

    public void setLocale(String lang) {
        myLocale = new Locale(lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
    }
}
