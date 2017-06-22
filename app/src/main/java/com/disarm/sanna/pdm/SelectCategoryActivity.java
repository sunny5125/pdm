package com.disarm.sanna.pdm;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.disarm.sanna.pdm.DisarmConnect.DCService;
import com.disarm.sanna.pdm.DisarmConnect.DataMuleService;
import com.disarm.sanna.pdm.Service.SyncService;
import com.disarm.sanna.pdm.Util.PrefUtils;
import com.disarm.sanna.pdm.location.LocationState;
import com.disarm.sanna.pdm.location.MLocation;
import com.nextgis.maplib.util.SettingsConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import belka.us.androidtoggleswitch.widgets.BaseToggleSwitch;
import belka.us.androidtoggleswitch.widgets.ToggleSwitch;

import static com.disarm.sanna.pdm.ActivityList.GPS_LOC;

/**
 * Created by arka on 14/9/16.
 * Activity to choose between offline social sharing and disaster management category
 */
public class SelectCategoryActivity extends AppCompatActivity{
    private boolean syncServiceBound = false;
    private boolean myServiceBound = false;
    private boolean dataMuleServiceBound = false;
    private boolean gpsService = false;
    SyncService syncService;
    public DCService myService;
    public DataMuleService dataMuleService;
    LocationManager lm;
    LocationListener locationListener;
    static String root = Environment.getExternalStorageDirectory().toString();
    public static String SOURCE_PHONE_NO;
    public ToggleSwitch toggleSwitch_gps;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_activity);

        SOURCE_PHONE_NO = PrefUtils.getFromPrefs(this, SettingsConstants.PHONE_NO, "NA");

        ToggleSwitch toggleSwitch_sync = (ToggleSwitch) findViewById(R.id.choose_sync);
        ArrayList<String> labels_sync = new ArrayList<>();
        labels_sync.add("OFF");
        labels_sync.add("ON");
        toggleSwitch_sync.setLabels(labels_sync);
        toggleSwitch_sync.setOnToggleSwitchChangeListener(new ToggleSwitch.OnToggleSwitchChangeListener() {

            @Override
            public void onToggleSwitchChangeListener(int position, boolean isChecked) {
                Log.v("test", String.valueOf(position));
                if (position == 1) {
                    toggleSwitch_gps.setCheckedTogglePosition(1);
                    final Intent syncServiceIntent = new Intent(getBaseContext(), SyncService.class);
                    bindService(syncServiceIntent, syncServiceConnection, Context.BIND_AUTO_CREATE);
                    startService(syncServiceIntent);

                    Toast.makeText(getApplicationContext(), R.string.start_sync, Toast.LENGTH_SHORT).show();
                } else {
                    final Intent syncServiceIntent = new Intent(getBaseContext(), SyncService.class);
                    if (syncServiceBound) {
                        unbindService(syncServiceConnection);
                    }
                    syncServiceBound = false;
                    stopService(syncServiceIntent);
                }
            }
        });

        ToggleSwitch toggleSwitch_dc = (ToggleSwitch) findViewById(R.id.choose_dc);
        ArrayList<String> labels_dc = new ArrayList<>();
        labels_dc.add("OFF");
        labels_dc.add("ON");
        toggleSwitch_dc.setLabels(labels_dc);
        toggleSwitch_dc.setOnToggleSwitchChangeListener(new ToggleSwitch.OnToggleSwitchChangeListener() {

            @Override
            public void onToggleSwitchChangeListener(int position, boolean isChecked) {
                if (position == 1) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SelectCategoryActivity.this);
                    builder.setMessage("Use as !!")
                            .setCancelable(false)
                            .setPositiveButton("Disarm Connect", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    final Intent myServiceIntent = new Intent(getBaseContext(), DCService.class);
                                    bindService(myServiceIntent, myServiceConnection, Context.BIND_AUTO_CREATE);
                                    startService(myServiceIntent);
                                }
                            })
                            .setNegativeButton("Data Mule", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    final Intent myServiceIntent = new Intent(getBaseContext(), DataMuleService.class);
                                    bindService(myServiceIntent, dataMuleConnection, Context.BIND_AUTO_CREATE);
                                    startService(myServiceIntent);
                                }
                            });
                    builder.show();
                } else {
                    final Intent myServiceIntent = new Intent(getBaseContext(), DCService.class);
                    final Intent dataMuleIntent = new Intent(getBaseContext(), DataMuleService.class);
                    if (myServiceBound) {
                        unbindService(myServiceConnection);
                        myServiceBound = false;
                        stopService(myServiceIntent);
                    }else if (dataMuleServiceBound){
                        unbindService(dataMuleConnection);
                        dataMuleServiceBound = false;
                        stopService(dataMuleIntent);
                    }
                }
            }
        });

        toggleSwitch_gps = (ToggleSwitch) findViewById(R.id.choose_gps);
        ArrayList<String> labels_gps = new ArrayList<>();
        labels_gps.add("OFF");
        labels_gps.add("ON");
        toggleSwitch_gps.setLabels(labels_gps);
        toggleSwitch_gps.setOnToggleSwitchChangeListener(new ToggleSwitch.OnToggleSwitchChangeListener() {

            @SuppressWarnings({"deprecation", "MissingPermission"})
            @Override
            public void onToggleSwitchChangeListener(int position, boolean isChecked) {
                if (position == 1) {
                    if (!LocationState.with(SelectCategoryActivity.this).locationServicesEnabled()){
                        enableGPS();
                    }
                    MLocation.subscribe(SelectCategoryActivity.this);

                } else {
                    MLocation.unsubscribe(SelectCategoryActivity.this);
                }
            }
        });

        ToggleSwitch activity_switcher = (ToggleSwitch) findViewById(R.id.activity_switch);
        activity_switcher.setCheckedTogglePosition(0);
        activity_switcher.setOnToggleSwitchChangeListener(new BaseToggleSwitch.OnToggleSwitchChangeListener() {
            @Override
            public void onToggleSwitchChangeListener(int position, boolean isChecked) {
                if (position == 0) {
                    // Launch Disaster Management Activity
                    Intent intentDisasterManagement = new Intent(SelectCategoryActivity.this, SurakshitActivity.class);
                    startActivity(intentDisasterManagement);


                }else if (position == 1){
                    // Launch Social App
                    Intent intentSocialShare = new Intent(SelectCategoryActivity.this, SocialShareActivity.class);
                    startActivity(intentSocialShare);
                }else if (position == 2){
                    Intent intentGIS = null;
                    try {
                        intentGIS = new Intent(SelectCategoryActivity.this, Class.forName("com.nextgis.mobile.activity.MainActivity"));
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    startActivity(intentGIS);
                }
            }
        });

        // Save crash logs in a file every time the application crashes
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Calendar cal = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                File crashLogFile =new File (SettingsConstants.DMS_PATH+"PDM_CrashLog" );
                if (!crashLogFile.exists()){
                    crashLogFile.mkdir();
                }
                String filename = crashLogFile + "/" + sdf.format(cal.getTime())+".txt";

                PrintStream writer;
                try {
                    writer = new PrintStream(filename, "UTF-8");
                    writer.println(e.getClass() + ": " + e.getMessage());
                    for (int i = 0; i < e.getStackTrace().length; i++) {
                        writer.println(e.getStackTrace()[i].toString());
                    }
                    System.exit(1);
                } catch (FileNotFoundException | UnsupportedEncodingException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder
                .setMessage("Exit Application")
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                unbindAllService();
                                System.exit(0);
                            }
                        });
        alertDialogBuilder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    //Psync
    private ServiceConnection syncServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            SyncService.SyncServiceBinder binder = (SyncService.SyncServiceBinder) service;
            syncService = binder.getService();
            syncServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            syncServiceBound = false;
        }
    };

    //DisarmConnect
    private ServiceConnection myServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            DCService.MyServiceBinder binder = (DCService.MyServiceBinder) service;
            myService = binder.getService();
            myServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            myServiceBound = false;
        }
    };

    //DataMule
    private ServiceConnection dataMuleConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DataMuleService.MyServiceBinder binder = (DataMuleService.MyServiceBinder) service;
            dataMuleService = binder.getService();
            dataMuleServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            dataMuleServiceBound = false;
        }
    };


    public void enableGPS() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder
                .setMessage(R.string.gps_msg)
                .setCancelable(false)
                .setTitle("Turn on Location")
                .setPositiveButton(R.string.enable_gps,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                Intent callGPSSettingIntent = new Intent(
                                        Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                // startActivity(callGPSSettingIntent);
                                startActivityForResult(callGPSSettingIntent, 5);
                            }
                        });
        alertDialogBuilder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private void unbindAllService() {
        final Intent syncServiceIntent = new Intent(getBaseContext(), SyncService.class);
        if (syncServiceBound) {
            unbindService(syncServiceConnection);
        }
        syncServiceBound = false;
        stopService(syncServiceIntent);

        final Intent myServiceIntent = new Intent(getBaseContext(), DCService.class);
        if (myServiceBound) {
            unbindService(myServiceConnection);
        }
        myServiceBound = false;
        stopService(myServiceIntent);

        if (gpsService) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            lm.removeUpdates(locationListener);
            gpsService = false;
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == 5 && resultCode == 0){
            String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            if(provider != null){
                switch(provider.length()){
                    case 0:
                        //GPS still not enabled..
                        toggleSwitch_gps.setCheckedTogglePosition(0);
                        break;
                    default:
                        MLocation.subscribe(SelectCategoryActivity.this);
                        Toast.makeText(this, R.string.enabled_gps, Toast.LENGTH_LONG).show();
                        break;
                }
            }
        }
        else{
            //the user did not enable his GPS
            enableGPS();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindAllService();
    }
}
