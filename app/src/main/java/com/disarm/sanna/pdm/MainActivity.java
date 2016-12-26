package com.disarm.sanna.pdm;

import android.Manifest;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.disarm.sanna.pdm.Adapters.MyAdapter;
import com.disarm.sanna.pdm.DisarmConnect.MyService;
import com.disarm.sanna.pdm.Service.SyncService;
import com.disarm.sanna.pdm.Util.DividerItemDecoration;
import com.disarm.sanna.pdm.Util.Reset;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

import static Sensors.readWriteMetaData.getImageAttributes;
import static Sensors.readWriteMetaData.saveExif;


public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SwitchCompat syncTog, connTog, gpsTog, emulation;
    SyncService syncService;
    MyService myService;
    float speed;
    double latitude, longitude;
    LocationManager lm;
    boolean gps_enabled;
    LocationListener locationListener;
    private boolean syncServiceBound = false;
    private boolean myServiceBound = false;
    private boolean gpsService = false;
    public static String phoneVal = "DefaultNode";
    Logger logger;
    public static ImageView img_wifi_state;
    public static TextView textConnect;
    static String root = Environment.getExternalStorageDirectory().toString();
    public final static String TARGET_DMS_PATH = root + "/DMS/";
    public static int[] prgmNameList = {R.string.health,
            R.string.food,
            R.string.shelter,
            R.string.victim};
    private boolean doubleBackToExitPressedOnce = false;
    public String TAG_Emulation = "Emulator";
    public Emulator emulator;

    public interface ClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        img_wifi_state = (ImageView) findViewById(R.id.img_wifi_state);
        syncTog = (SwitchCompat) findViewById(R.id.synctoggle);
        connTog = (SwitchCompat) findViewById(R.id.conntoggle);
        gpsTog = (SwitchCompat) findViewById(R.id.gpstoggle);
        // Emulation code
        emulation = (SwitchCompat) findViewById(R.id.emulation);

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        textConnect = (TextView) findViewById(R.id.textView12);

        // Set ImageView to Hotspot
        img_wifi_state.setImageResource(R.drawable.wifi);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new MyAdapter(prgmNameList);
        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), mRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Intent intent = new Intent(getApplicationContext(), ActivityList.class);
                if (position == 0) {
                    intent.putExtra("IntentType", "Health");
                } else if (position == 1) {
                    intent.putExtra("IntentType", "Food");
                } else if (position == 2) {
                    intent.putExtra("IntentType", "Shelter");
                } else if (position == 3) {
                    intent.putExtra("IntentType", "Victim");
                }
                startActivity(intent);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
        syncTog.setOnCheckedChangeListener(this);
        connTog.setOnCheckedChangeListener(this);
        gpsTog.setOnCheckedChangeListener(this);
        // EMulation oncheck
        emulation.setOnCheckedChangeListener(this);

        boolean c = isMyServiceRunning(bishakh.psync.SyncService.class);
        if (c)
            syncTog.setChecked(true);
        else
            syncTog.setChecked(false);

        boolean d = isMyServiceRunning(MyService.class);
        if (d)
            connTog.setChecked(true);
        else
            connTog.setChecked(false);


        // Read Source name for PEerID
        // Read Device source from ConfigFile.txt
        File file = new File(TARGET_DMS_PATH,"source.txt");
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();

            this.phoneVal = new String(data, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create Emulator object
        this.emulator = new Emulator(this.phoneVal);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        switch (compoundButton.getId()) {
            case R.id.synctoggle:
                if (b) {
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
                break;

            case R.id.conntoggle:
                if (b) {
                    final Intent myServiceIntent = new Intent(getBaseContext(), MyService.class);
                    bindService(myServiceIntent, myServiceConnection, Context.BIND_AUTO_CREATE);
                    startService(myServiceIntent);
                } else {

                    final Intent myServiceIntent = new Intent(getBaseContext(), MyService.class);
                    if (myServiceBound) {
                        unbindService(myServiceConnection);
                    }
                    myServiceBound = false;
                    stopService(myServiceIntent);
                    img_wifi_state.setImageResource(R.drawable.wifi);
                    textConnect.setText("");
             }
                break;

            case R.id.gpstoggle:
                if (b) {
                    requestLocation();
                } else {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    if (gpsService) {
                        lm.removeUpdates(locationListener);
                        gpsService = false;
                    }
                }
                break;

            case R.id.emulation:
                if(b)
                {
                   // emulator.startEmulator();
                    try {


                    }
                    catch (Exception e)
                    {}

                }
                else
                {
                   // emulator.stopEmulator();
                    Log.v(TAG_Emulation,"Emulation Stopped");
                }
                break;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.webView) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://192.168.43.1:8000"));
            startActivity(browserIntent);
        } else if (id == R.id.mapView) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://127.0.0.1:8080/getMapAsset/index.html"));
            startActivity(browserIntent);
        } else if (id == R.id.reset) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder
                    .setMessage(R.string.reset_working)
                    .setCancelable(false)
                    .setPositiveButton(R.string.reset,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    File dir = new File(Environment.getExternalStorageDirectory() + "/DMS/Working");
                                    if (Reset.deleteContents(dir)) {
                                        Toast.makeText(MainActivity.this, R.string.reset_done, Toast.LENGTH_SHORT).show();
                                    }

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
        } else if (id == R.id.resetall) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder
                    .setMessage(R.string.reset_all_data)
                    .setCancelable(false)
                    .setPositiveButton(R.string.reset,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    ProgressDialog pd = new ProgressDialog(MainActivity.this);
                                    pd.show();
                                    pd.setMessage("Reset is on its way !");
                                    File dir = new File(Environment.getExternalStorageDirectory() + "/DMS");
                                    if (Reset.deleteContents(dir)) {
                                        pd.setMessage("Reset Done");
                                        Toast.makeText(MainActivity.this, R.string.reset_done, Toast.LENGTH_SHORT).show();
                                    }
                                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.clear();
                                    editor.commit();
                                    pd.setMessage("Restarting");
                                    Intent i = getBaseContext().getPackageManager()
                                            .getLaunchIntentForPackage(getBaseContext().getPackageName());
                                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    finish();
                                    startActivity(i);
                                    pd.dismiss();
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
        } else if (id == R.id.exit) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);
            int pid = android.os.Process.myPid();
            android.os.Process.killProcess(pid);
            System.exit(0);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            unbindAllService();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.press_back_to_exit, Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
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
            MyService.MyServiceBinder binder = (MyService.MyServiceBinder) service;
            myService= binder.getService();
            myServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            myServiceBound = false;
        }
    };



    public void enableGPS(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder
                .setMessage(R.string.gps_msg)
                .setCancelable(false)
                .setPositiveButton(R.string.enable_gps,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
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

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == 5 && resultCode == 0){
            String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            if(provider != null){
                switch(provider.length()){
                    case 0:
                        //GPS still not enabled..
                        break;
                    default:
                        Toast.makeText(this, R.string.enabled_gps, Toast.LENGTH_LONG).show();
                        //startService(new Intent(getBaseContext(), LocationUpdateService.class));
                        break;
                }
            }
        }
        else{
            //the user did not enable his GPS
        }
    }
    private void requestLocation(){
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean statusOfGPS = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (statusOfGPS) {
            // Call logger constructor using phoneVal
            logger = new Logger(phoneVal);


            lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            gps_enabled = false;

            try {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch (Exception ex) {
            }

            // Check if gps and network provider is on or off
            if (!gps_enabled ) {

                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(myIntent);
            }

            locationListener = new MyLocationListener(logger,phoneVal);

            lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, locationListener);
            gpsService = true;
        } else {
            enableGPS();
        }
    }

    private void unbindAllService(){
        final Intent syncServiceIntent = new Intent(getBaseContext(), SyncService.class);
        if (syncServiceBound) {
            unbindService(syncServiceConnection);
        }
        syncServiceBound = false;
        stopService(syncServiceIntent);

        final Intent myServiceIntent = new Intent(getBaseContext(), MyService.class);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindAllService();
    }
}


