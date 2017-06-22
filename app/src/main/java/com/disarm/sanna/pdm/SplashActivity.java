package com.disarm.sanna.pdm;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.disarm.sanna.pdm.Util.CopyAssets;
import com.disarm.sanna.pdm.Util.PrefUtils;
import com.nextgis.maplib.util.SettingsConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created by sanna on 11/7/16.
 */
public class SplashActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int PERMISSION_ALL = 1;

    public File dmsFolder = Environment.getExternalStoragePublicDirectory("DMS/");
    public File workingFolder = Environment.getExternalStoragePublicDirectory("DMS/Working");
    public File tmpFolder = Environment.getExternalStoragePublicDirectory("DMS/tmp");
    public File mapFolder = Environment.getExternalStoragePublicDirectory("DMS/Map");

    private EditText phoneText1;
    private Button submitButton;
    private Spinner spinner2;
    public Locale myLocale;
    private String definedlanguage ;
    public static final String Lang = "language";
    String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkPermissions(this, PERMISSIONS)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            }
        }else{
            creatingFolders();
        }
        if (checkPhoneNo()){
            callWriteSettingActivity();
        }else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setContentView(R.layout.activity_splash);

            submitButton = (Button) findViewById(R.id.submitButton);
            phoneText1 = (EditText) findViewById(R.id.phoneText);
            spinner2 = (Spinner)findViewById(R.id.language);

            definedlanguage = PrefUtils.getFromPrefs(this,Lang,"en");
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
                    if (pos == 0) {
                        setLocale("en");
                        PrefUtils.saveToPrefs(getApplicationContext(),Lang,"en");
                    } else if (pos == 1) {
                        setLocale("bn");
                        PrefUtils.saveToPrefs(getApplicationContext(),Lang,"bn");
                    } else if (pos == 2) {
                        setLocale("hi");
                        PrefUtils.saveToPrefs(getApplicationContext(),Lang,"hi");
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }

    }

    private void creatingFolders(){
        if (!dmsFolder.exists()){
            dmsFolder.mkdir();
        }if (!workingFolder.exists()){
            workingFolder.mkdir();
        }if (!tmpFolder.exists()){
            tmpFolder.mkdir();
        }if (!mapFolder.exists()){
            mapFolder.mkdir();
        }
        run1stTimeOnly();

    }

    private boolean checkPhoneNo() {
        boolean isSourceExit = false;
        if (!PrefUtils.getFromPrefs(this, SettingsConstants.PHONE_NO, "NA").equals("NA")) {
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
                PrefUtils.saveToPrefs(this, SettingsConstants.PHONE_NO, phoneTextVal);
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
                    creatingFolders();

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

    private void run1stTimeOnly(){
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
