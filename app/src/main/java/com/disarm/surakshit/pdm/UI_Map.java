package com.disarm.surakshit.pdm;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Contacts;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.disarm.surakshit.pdm.BackgroundProcess.FileTask;
import com.disarm.surakshit.pdm.Capture.AudioCapture;
import com.disarm.surakshit.pdm.Capture.Photo;
import com.disarm.surakshit.pdm.Capture.Video;
import com.disarm.surakshit.pdm.DisarmConnect.DCService;
import com.disarm.surakshit.pdm.Service.SyncService;
import com.disarm.surakshit.pdm.Util.KmzCreator;
import com.disarm.surakshit.pdm.Util.PrefUtils;
import com.disarm.surakshit.pdm.Util.Reset;
import com.disarm.surakshit.pdm.location.LocationState;
import com.disarm.surakshit.pdm.location.MLocation;
import com.snatik.storage.Storage;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.kml.KmlPlacemark;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.zip.ZipFile;

public class UI_Map extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    public static Context contextOfApplication;
    Button draw_save,undo_back,cancel,btn_save_current_marker;
    public static boolean first_time = true;
    static MapView map;
    public static GeoPoint current_point;
    View bottomsheet;
    ArrayList<Marker> markerpoints=new ArrayList<>();
    ITileSource tileSource;
    CompassOverlay mCompassOverlay;
    ScaleBarOverlay mScaleBarOverlay;
    IMapController mapController;
    final int MIN_ZOOM=14,MAX_ZOOM=20,PIXEL=256;
    SyncService syncService;
    public DCService myService;
    private boolean syncServiceBound = false;
    private boolean myServiceBound = false;
    private boolean gpsService = false;
    public static int total_file=0;
    LocationManager lm;
    Handler currentMarkerLocation;
    Marker currentLocationMarker;
    LocationListener locationListener;
    ArrayList<GeoPoint> polygon_points=new ArrayList<>();
    FloatingActionButton fab;
    int draw_flag=1,curr_loation_flag=0,can_current_loc_flag=0;
    final Polygon polygon = new Polygon();
    String text_description="";
    final ArrayList<Marker> all_markers = new ArrayList<>();
    final static HashMap<String,Boolean> all_kmz_overlay_map = new HashMap<>();
    final static ArrayList<Overlay> allOverlays = new ArrayList<>();
    HandlerThread refreshThread = new HandlerThread("refreshThread");
    HandlerThread currentMarkerLocationThread=new HandlerThread("currentMarkerLocation");
    HandlerThread syncThread = new HandlerThread("syncHandler");
    Handler refresh;
    Handler setCenter = new Handler();
    Handler syncServiceHandle;
    boolean center = false;
    private int flag=0;
    @Override
    protected void onCreate(Bundle drawdInstanceState) {
        super.onCreate(drawdInstanceState);
        setContentView(R.layout.activity_ui__map);
        contextOfApplication = getApplicationContext();
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

               // Toast.makeText(UI_Map.this, "", Toast.LENGTH_SHORT).show();
                if(currentLocationMarker==null ) {
                    draw_save.setVisibility(View.VISIBLE);
                    draw_save.setText(" ");
                    undo_back.setEnabled(false);
                    undo_back.setVisibility(View.INVISIBLE);
                    cancel.setVisibility(View.INVISIBLE);
                    fab.setVisibility(View.INVISIBLE);
                    dialog_set_postion();
                }
                else
                {

                    draw_save.setVisibility(View.VISIBLE);
                    cancel.setVisibility(View.VISIBLE);
                    undo_back.setEnabled(true);
                    undo_back.setText("UNDO");
                    btn_save_current_marker.setVisibility(View.INVISIBLE);
                    undo_back.setVisibility(View.VISIBLE);
                    fab.setVisibility(View.INVISIBLE);
                    polygon_points.clear();
                    total_file=0;
                    flag=0;
                    removeInfo();
                    removeInfoWindow();
                }

            }
        });
        refreshThread.start();
        syncThread.start();
        currentMarkerLocationThread.start();
        Looper refreshLoop = refreshThread.getLooper();
        refresh = new Handler(refreshLoop);
        Looper syncLoop = syncThread.getLooper();
        syncServiceHandle = new Handler(syncLoop);
        Looper currentLoc=currentMarkerLocationThread.getLooper();
        currentMarkerLocation=new Handler(currentLoc);
        Storage storage = new Storage(getApplicationContext());
        storage.deleteDirectory(Environment.getExternalStoragePublicDirectory("DMS/tmpOpen").toString());
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, null, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        SelectCategoryActivity.SOURCE_PHONE_NO = PrefUtils.getFromPrefs(this, SplashActivity.PHONE_NO, "NA");
        Log.d("Phone No",SelectCategoryActivity.SOURCE_PHONE_NO);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        crashLog();
        startService();
        intialize();
        setBottomsheet();
        setMapData();
        setMapClick();
        setDrawClick();
        setCurrentMarker();
        setCancelClick(fab);
        setSaveClick(fab);
        refreshWorkingData();
        markerpoints.clear();
    }

    private void dialog_set_postion()
    {
        draw_save.setText("");
        draw_save.setEnabled(false);
        draw_save.setVisibility(View.VISIBLE);
        undo_back.setVisibility(View.INVISIBLE);

        AlertDialog.Builder dialogxyz = new AlertDialog.Builder(UI_Map.this);
        dialogxyz.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                cancel.setVisibility(View.VISIBLE);
               can_current_loc_flag=1;
            }
        });

        dialogxyz.setTitle("Please select your current location");
        dialogxyz.setCancelable(false);
        dialogxyz.show();
    }



    @Override
    public void onBackPressed() {
        Boolean isOpen = false;
        try {
            for (Overlay overlay : allOverlays) {
                if (overlay instanceof Polygon) {
                    if (((Polygon) overlay).getInfoWindow().isOpen()) {
                        isOpen = true;
                        ((Polygon) overlay).getInfoWindow().close();
                    }

                } else if (overlay instanceof Marker) {
                    if (((Marker) overlay).getInfoWindow().isOpen()) {
                        isOpen = true;
                        ((Marker) overlay).getInfoWindow().close();
                    }
                }
            }
        }
        catch (Exception ex){

        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if(!isOpen) {
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindAllService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.ui__map, menu);
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
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void intialize(){
        map = (MapView) findViewById(R.id.ui_map);
        String[] s = {"http://127.0.0.1:8080/getTile/"};

        //tileSource = new MyOSMTileSource(
        //        "Mapnik", MIN_ZOOM, MAX_ZOOM, PIXEL, ".png", s);
        tileSource = new XYTileSource("tiles",MIN_ZOOM,MAX_ZOOM,PIXEL,".png",new String[]{});
        map.setTileSource(tileSource);
        draw_save = (Button) findViewById(R.id.btn_map_draw_save);
        cancel = (Button) findViewById(R.id.btn_map_cancel);
        undo_back = (Button) findViewById(R.id.btn_map_undo_back);
        bottomsheet = findViewById(R.id.map_bottomsheet);
        btn_save_current_marker = (Button) findViewById(R.id.btn_current_loc);

    }

    private void setBottomsheet(){
        bottomsheet.setVisibility(View.GONE);
        final BottomSheetBehavior behave = BottomSheetBehavior.from(bottomsheet);
        bottomsheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                behave.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        behave.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_DRAGGING:
                        Log.i("BottomSheetCallback", "BottomSheetBehavior.STATE_DRAGGING");
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        Log.i("BottomSheetCallback", "BottomSheetBehavior.STATE_SETTLING");
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        Log.i("BottomSheetCallback", "BottomSheetBehavior.STATE_EXPANDED");
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        Log.i("BottomSheetCallback", "BottomSheetBehavior.STATE_COLLAPSED");
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        Log.i("BottomSheetCallback", "BottomSheetBehavior.STATE_HIDDEN");
                        break;
                }
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                Log.i("BottomSheetCallback", "slideOffset: " + slideOffset);
            }
        });
    }

    private void setMapData()
    {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        mapController = map.getController();
        mapController.setZoom(17);
        GeoPoint startPoint = new GeoPoint(23.5477,87.2931);
        mapController.setCenter(startPoint);
        setCenter(mapController);
        mCompassOverlay = new CompassOverlay(ctx, new InternalCompassOrientationProvider(ctx), map);
        mCompassOverlay.enableCompass();
        map.getOverlays().add(mCompassOverlay);
        mScaleBarOverlay = new ScaleBarOverlay(map);
        mScaleBarOverlay.setCentred(true);
        mScaleBarOverlay.setScaleBarOffset(width/2, 10);
        map.getOverlays().add(mScaleBarOverlay);
        setWorkingData(true);
    }
    private void setCenter(final IMapController mapController){
        Runnable r = new Runnable()
        {
            @Override
            public void run() {
                if(!center){
                    Location l = MLocation.getLocation(getApplicationContext());
                    if(l != null){
                        GeoPoint g = new GeoPoint(l.getLatitude(),l.getLongitude());
                        currentMarkerLocation();
                        can_current_loc_flag=0;
                        center = true;
                        mapController.setCenter(g);
                        btn_save_current_marker.setVisibility(View.VISIBLE);
                    }
                    else{
                        setCenter.postDelayed(this,500);
                    }

                }
            }
        };
        setCenter.postDelayed(r,500);

    }

    private void initCurrentLocationMarker()
    {
        currentLocationMarker = new Marker(map);
        currentLocationMarker.setIcon(getResources().getDrawable(R.drawable.location_pin_32));
        currentLocationMarker.setTitle("You are here");
    }
    private void currentMarkerLocation()
    {
        Runnable r = new Runnable()
        {
            @Override
            public void run() {
                Location l = MLocation.getLocation(getApplicationContext());
                current_point = new GeoPoint(l.getLatitude(),l.getLongitude());

                if(currentLocationMarker==null)
                {
                    initCurrentLocationMarker();

                }
                currentLocationMarker.setPosition(current_point);
                if(map.getOverlays().contains(currentLocationMarker))
                {
                    map.getOverlays().remove(currentLocationMarker);
                    map.getOverlays().add(currentLocationMarker);
                }
                else
                    map.getOverlays().add(currentLocationMarker);

                currentMarkerLocation.postDelayed(this,500);
            }
        };
        currentMarkerLocation.postDelayed(r,500);

    }


    private void startService(){
        Runnable run = new Runnable() {
            @Override
            public void run() {
                if(SelectCategoryActivity.SOURCE_PHONE_NO!=null) {
                    final Intent syncServiceIntent = new Intent(getBaseContext(), SyncService.class);
                    bindService(syncServiceIntent, syncServiceConnection, Context.BIND_AUTO_CREATE);
                    startService(syncServiceIntent);
                }
                else{
                    SelectCategoryActivity.SOURCE_PHONE_NO = PrefUtils.getFromPrefs(UI_Map.this, SplashActivity.PHONE_NO, "NA");
                    syncServiceHandle.postDelayed(this,1000);
                }
            }
        };
        syncServiceHandle.postDelayed(run,1000);


        final Intent myServiceIntent = new Intent(getBaseContext(), DCService.class);
        bindService(myServiceIntent, myServiceConnection, Context.BIND_AUTO_CREATE);
        startService(myServiceIntent);

        if (!LocationState.with(UI_Map.this).locationServicesEnabled()){
            enableGPS();
        }
        MLocation.subscribe(UI_Map.this);
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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 5 && resultCode == 0) {
            String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            if (provider != null) {
                switch (provider.length()) {
                    case 0:
                        //GPS still not enabled..
                        Toast.makeText(UI_Map.this, "Please enable GPS!!!", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        MLocation.subscribe(UI_Map.this);
                        Toast.makeText(this, R.string.enabled_gps, Toast.LENGTH_LONG).show();
                        break;
                }
            }
        } else {
            //the user did not enable his GPS
            enableGPS();
        }
    }

    private void crashLog(){
        // draw crash logs in a file every time the application crashes
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {

                Calendar cal = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                File crashLogFile =new File (SplashActivity.DMS_PATH+"PDM_CrashLog" );
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

    Marker marker;
    private void setMapClick(){
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(final GeoPoint p) {

                if(can_current_loc_flag==1 && currentLocationMarker==null && fab.getVisibility()==View.INVISIBLE && draw_save.isEnabled()==false)
                {
                    draw_save.setText("SAVE LOCATION");
                    marker = new Marker(map);
                    marker.setPosition(p);
                    marker.setDraggable(true);
                    map.getOverlays().add(marker);
                    draw_save.setEnabled(true);
                }
                else
                if(draw_save.getVisibility()==View.VISIBLE && can_current_loc_flag==0)
                {
                    draw_save.setEnabled(true);
                    undo_back.setEnabled(true);
                    polygon_points.add(p);
                    final Marker marker = new Marker(map);
                    markerpoints.add(marker);
                    marker.setPosition(p);
                    marker.setDraggable(true);
                    final GeoPoint g = new GeoPoint(p);
                    marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(Marker marker, MapView mapView) {
                            marker.getInfoWindow().close();
                            return false;
                        }
                    });
                    marker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
                        @Override
                        public void onMarkerDrag(Marker new_marker) {
                            int index = polygon_points.indexOf(g);
                            polygon_points.set(index,new_marker.getPosition());
                            map.getOverlays().remove(polygon);
                            polygon.getPoints().clear();
                            polygon.setPoints(polygon_points);
                            map.getOverlays().add(polygon);

                            g.setLatitude(new_marker.getPosition().getLatitude());
                            g.setLongitude(new_marker.getPosition().getLongitude());
                            g.setAltitude(new_marker.getPosition().getAltitude());
                        }

                        @Override
                        public void onMarkerDragEnd(Marker new_marker) {
                            int index = polygon_points.indexOf(g);
                            polygon_points.set(index,new_marker.getPosition());
                            map.getOverlays().remove(polygon);
                            polygon.getPoints().clear();
                            polygon.setPoints(polygon_points);
                            map.getOverlays().add(polygon);
                            g.setLatitude(new_marker.getPosition().getLatitude());
                            g.setLongitude(new_marker.getPosition().getLongitude());
                            g.setAltitude(new_marker.getPosition().getAltitude());
                        }

                        @Override
                        public void onMarkerDragStart(Marker marker) {

                        }
                    });
                    marker.setTitle(marker.getPosition().toString());
                    map.getOverlays().add(marker);
                    all_markers.add(marker);
                    removeInfoWindow();
                    removeInfo();
                    return true;
                }
                removeInfoWindow();
                removeInfo();
                return false;
            }
            @Override
            public boolean longPressHelper(GeoPoint p) {
                return true;
            }
        };
        MapEventsOverlay OverlayEvents = new MapEventsOverlay(getBaseContext(), mReceive);
        map.getOverlays().add(OverlayEvents);
    }
    private  void setCurrentMarker()
    {
        btn_save_current_marker.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                if (currentLocationMarker != null)
                {
                    curr_loation_flag = 1;
                    createTextDialog();
                }
                else
                {
                    Toast.makeText(UI_Map.this, "Please turn on GPS.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setDrawClick(){
        draw_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // draw btn
                if(draw_save.getText().toString()=="SAVE LOCATION")
                {
                    AlertDialog.Builder dialogxyz=new AlertDialog.Builder(UI_Map.this);
                    dialogxyz.setTitle("Are you sure to save current position?");
                    dialogxyz.setCancelable(false);


                    dialogxyz.setPositiveButton("yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            //save the current location
                            map.getOverlays().remove(marker);
                            can_current_loc_flag = 0;
                            current_point =new GeoPoint(marker.getPosition().getLatitude(),marker.getPosition().getLongitude());
                            initCurrentLocationMarker();
                            currentLocationMarker.setPosition(current_point);
                            map.getOverlays().add(currentLocationMarker);
                            fab.setVisibility(View.VISIBLE);
                            cancel.setVisibility(View.INVISIBLE);
                            draw_save.setVisibility(View.INVISIBLE);
                            draw_save.setText("DRAW");
                            btn_save_current_marker.setVisibility(View.VISIBLE);
                        }
                    });
                    dialogxyz.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // go to again map to click again marker in map

                            can_current_loc_flag=1;
                            map.getOverlays().remove(marker);
                            cancel.setVisibility(View.INVISIBLE);
                            draw_save.setVisibility(View.INVISIBLE);
                            fab.setVisibility(View.VISIBLE);

                        }
                    });

                     dialogxyz.show();

                }
                else
                if(flag==0)
                {
                    if(polygon_points.size()!=0)
                    {
                        polygon.setPoints(polygon_points);
                        map.getOverlays().add(polygon);
                        map.invalidate();
                        removeInfoWindow();
                        removeInfo();
                        undo_back.setText("BACK");
                        draw_save.setText("SAVE");
                        flag = 1;
                    }
                    else Toast.makeText(getBaseContext(), "No marker is selected.", Toast.LENGTH_SHORT).show();
                }
                // else part for save data
                else {

                    for (Marker m : all_markers) {
                        m.getInfoWindow().close();
                    }
                    createTextDialog();
                    map.getOverlays().remove(polygon);
                    for (int i = 0; i < all_markers.size(); i++) {
                        map.getOverlays().remove(all_markers.get(i));
                    }
                    map.invalidate();
                    undo_back.setText("UNDO");
                    draw_save.setText("DRAW");
                    draw_save.setVisibility(View.GONE);
                    cancel.setVisibility(View.GONE);
                    undo_back.setVisibility(View.GONE);
                    fab.setVisibility(View.VISIBLE);

                }
            }
        });

    }

    private void setCancelClick(final FloatingActionButton fab){
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                undo_back.setText("UNDO");
                flag=0;
                total_file=0;
                draw_save.setText("Draw");
                fab.setVisibility(View.VISIBLE);
                if(can_current_loc_flag==0)
                btn_save_current_marker.setVisibility(View.VISIBLE);
                if(marker!=null)
                {
                    map.getOverlays().remove(marker);
                }
                draw_save.setVisibility(View.GONE);
                cancel.setVisibility(View.GONE);
                undo_back.setVisibility(View.GONE);
                draw_flag=1;
                map.getOverlays().remove(polygon);
                polygon_points.clear();
                removeInfoWindow();
                removeInfo();
                for(int i=0;i<all_markers.size();i++){
                    map.getOverlays().remove(all_markers.get(i));
                }
                map.invalidate();
            }
        });
    }

    private void setSaveClick(final FloatingActionButton fab){
        undo_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(undo_back.getText().toString().equals("UNDO"))
                {
                    if (markerpoints.size() != 0 && polygon_points.size() != 0) {
                        markerpoints.get(markerpoints.size() - 1).remove(map);
                        markerpoints.remove(markerpoints.size() - 1);

                        polygon_points.remove(polygon_points.size() - 1);


                    } else {

                        Toast.makeText(getBaseContext(), "There is no marker ", Toast.LENGTH_SHORT).show();

                    }
                }
                else
                {
                    flag=0;
                    draw_save.setText("DRAW");
                    undo_back.setText("UNDO");
                    map.getOverlays().remove(polygon);
                }


            }

        });
    }

    private void removeInfoWindow(){

        for(Marker m : all_markers){
            m.getInfoWindow().close();
        }

        if(currentLocationMarker!=null)
         currentLocationMarker.getInfoWindow().close();
    }
    private void createTextDialog(){
        text_description="";
        View dialog_view = getLayoutInflater().inflate(R.layout.dialog_text,null);
        final EditText textmsg;
        Button save,add,cancel;
        textmsg = (EditText) dialog_view.findViewById(R.id.dialog_text_description);
        save = (Button) dialog_view.findViewById(R.id.dialog_button_save);
        add = (Button) dialog_view.findViewById(R.id.dialog_button_add);
        cancel = (Button) dialog_view.findViewById(R.id.dialog_button_cancel);
        if(current_point!=null)
        {
            if(btn_save_current_marker.getVisibility()==View.INVISIBLE)
            {
                btn_save_current_marker.setVisibility(View.VISIBLE);
            }
        }
        final AlertDialog.Builder dialog_builder = new AlertDialog.Builder(UI_Map.this);
        dialog_builder.setTitle("Please describe the situation in less than 50 words !!!");
        dialog_builder.setCancelable(false);
        dialog_builder.setView(dialog_view);
        final AlertDialog dialog_parent = dialog_builder.create();
        dialog_parent.show();

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flag=0;

                draw_save.setText("Draw");

                total_file=0;
                dialog_parent.dismiss();
            }
        });

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(textmsg.getText().toString().length()>0){
                    text_description = textmsg.getText().toString();
                    createDialog();
                }
                else
                {
                    Toast.makeText(getBaseContext(),"No info found to be saved!!! Please describe the situation there",Toast.LENGTH_SHORT).show();
                }
                dialog_parent.dismiss();
            }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                text_description = textmsg.getText().toString();
                AlertDialog.Builder dialogxyz = new AlertDialog.Builder(UI_Map.this);
                dialogxyz.setNeutralButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        flag=0;
                        if(textmsg.getText().toString().length()>0){
                            KmlDocument kml = new KmlDocument();
                            if(polygon_points.size()==1 || curr_loation_flag==1)
                            {
                                if(curr_loation_flag==1)
                                {
                                    curr_loation_flag=0;
                                    polygon_points.add(current_point);
                                    all_markers.add(new Marker(map));
                                }

                                Marker marker = new Marker(map);
                                marker.setPosition(polygon_points.get(0));
                                marker.setSnippet(textmsg.getText().toString());
                                KmlPlacemark placemark = new KmlPlacemark(marker);
                                String latlng = FileTask.getloc(getContextOfApplication());
                                String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                                String file_name = "TXT_50_data_"+
                                        SelectCategoryActivity.SOURCE_PHONE_NO+
                                        "_defaultMcs_"
                                        +latlng
                                        +"_"+timeStamp+
                                        "_1.kmz";
                                kml.mKmlRoot.add(placemark);
                                kml.mKmlRoot.setExtendedData("Media Type","TXT");
                                kml.mKmlRoot.setExtendedData("Group Type","data");
                                kml.mKmlRoot.setExtendedData("Time Stamp",timeStamp);
                                kml.mKmlRoot.setExtendedData("Source",SelectCategoryActivity.SOURCE_PHONE_NO);
                                kml.mKmlRoot.setExtendedData("Destination","defaultMcs");
                                kml.mKmlRoot.setExtendedData("Lat Long",latlng);
                                kml.mKmlRoot.setExtendedData("Group ID","1");
                                kml.mKmlRoot.setExtendedData("Priority","50");
                                kml.mKmlRoot.setExtendedData("KML Type","Point");
                                File tempKmzFolder = Environment.getExternalStoragePublicDirectory("DMS/tmpKMZ");
                                if(!tempKmzFolder.exists()){
                                    tempKmzFolder.mkdir();
                                }
                                File file = Environment.getExternalStoragePublicDirectory("DMS/tmpKMZ/index.kml");
                                kml.saveAsKML(file);
                                KmzCreator kmz = new KmzCreator();
                                kmz.zipIt(Environment.getExternalStoragePublicDirectory("DMS/Working/"+file_name).toString());
                                Storage storage = new Storage(getContextOfApplication());
                                storage.deleteDirectory(tempKmzFolder.toString());
                            }
                            else if(polygon_points.size()>1){
                                polygon_points.add(polygon_points.get(0));
                                polygon.setPoints(polygon_points);
                                polygon.setSnippet(textmsg.getText().toString());
                                String latlng = FileTask.getloc(getContextOfApplication());
                                String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                                String file_name = "TXT_50_data_"+
                                        SelectCategoryActivity.SOURCE_PHONE_NO+
                                        "_defaultMcs_"
                                        +latlng
                                        +"_"+timeStamp+
                                        "_1.kmz";
                                kml.mKmlRoot.setExtendedData("Media Type","TXT");
                                kml.mKmlRoot.setExtendedData("Group Type","data");
                                kml.mKmlRoot.setExtendedData("Time Stamp",timeStamp);
                                kml.mKmlRoot.setExtendedData("Source",SelectCategoryActivity.SOURCE_PHONE_NO);
                                kml.mKmlRoot.setExtendedData("Destination","defaultMcs");
                                kml.mKmlRoot.setExtendedData("Lat Long",latlng);
                                kml.mKmlRoot.setExtendedData("Group ID","1");
                                kml.mKmlRoot.setExtendedData("Priority","50");
                                kml.mKmlRoot.setExtendedData("KML Type","Polygon");
                                kml.mKmlRoot.addOverlay(polygon,kml);
                                File tempKmzFolder = Environment.getExternalStoragePublicDirectory("DMS/tmpKMZ");
                                if(!tempKmzFolder.exists()){
                                    tempKmzFolder.mkdir();
                                }

                                File file = Environment.getExternalStoragePublicDirectory("DMS/tmpKMZ/index.kml");
                                kml.saveAsKML(file);
                                KmzCreator kmz = new KmzCreator();
                                kmz.zipIt(Environment.getExternalStoragePublicDirectory("DMS/Working/"+file_name).toString());
                                Storage storage = new Storage(getContextOfApplication());

                                storage.deleteDirectory(tempKmzFolder.toString());
                            }
                            setWorkingData(true);
                            dialog.dismiss();
                            dialog_parent.dismiss();
                            if(currentLocationMarker!=null)
                            {
                                map.getOverlays().remove(currentLocationMarker);
                                currentLocationMarker=null;
                                btn_save_current_marker.setVisibility(View.INVISIBLE);
                            }
                        }
                        else {
                            Toast.makeText(getBaseContext(),"No info found to be saved!!! Please describe the situation there",Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            dialog_parent.dismiss();
                        }


                    }
                });
                dialogxyz.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        dialog_parent.dismiss();
                    }
                });
                dialogxyz.setNegativeButton("Add Media", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        dialog_parent.dismiss();
                        text_description = textmsg.getText().toString();
                        createDialog();

                    }
                });
                dialogxyz.setTitle("Do you really want to save?");
                dialogxyz.setCancelable(false);
                AlertDialog ad = dialogxyz.create();
                ad.show();
            }
        });
    }
    private void createDialog(){
        View dialog_view = getLayoutInflater().inflate(R.layout.dialog_list_type,null);
        final TextView img,vid,aud;
        Button submit,discard;
        final EditText importance,destination;
        dialog_view.setPadding(10,10,10,10);

        img = (TextView) dialog_view.findViewById(R.id.dialog_tv_image);
        vid = (TextView) dialog_view.findViewById(R.id.dialog_tv_video);
        aud = (TextView) dialog_view.findViewById(R.id.dialog_tv_audio);
        submit = (Button) dialog_view.findViewById(R.id.dialog_save);
        discard = (Button) dialog_view.findViewById(R.id.dialog_discard);
        importance = (EditText) dialog_view.findViewById(R.id.dialog_importance);
        destination = (EditText) dialog_view.findViewById(R.id.dialog_destination);

        AlertDialog.Builder dialog_builder = new AlertDialog.Builder(UI_Map.this);
        dialog_builder.setTitle("Please select the media type which describes the situition best !!!");
        dialog_builder.setCancelable(false);
        dialog_builder.setView(dialog_view);
        final AlertDialog dialog = dialog_builder.create();
        dialog.show();

        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(total_file==0) {
                    total_file++;
                    Intent intent = new Intent(UI_Map.this, Photo.class);
                    intent.putExtra("Intent type", "Data");
                    startActivity(intent);
                }
                else{
                    Toast.makeText(getBaseContext(),"Only one media is allowed",Toast.LENGTH_SHORT).show();
                }
            }
        });

        vid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(total_file==0){
                    total_file++;
                    Intent intent = new Intent(UI_Map.this, Video.class);
                    intent.putExtra("Intent type","Data");
                    startActivity(intent);
                }
                else{
                    Toast.makeText(getBaseContext(),"Only one media is allowed",Toast.LENGTH_SHORT).show();
                }
            }
        });

        aud.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(total_file==0){
                    total_file++;
                    Intent intent = new Intent(UI_Map.this, AudioCapture.class);
                    intent.putExtra("Intent type","Data");
                    startActivity(intent);
                }
                else{
                    Toast.makeText(getBaseContext(),"Only one media is allowed",Toast.LENGTH_SHORT).show();
                }
            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dest = destination.getText().toString().trim();
                String imp = importance.getText().toString().trim();
                if(curr_loation_flag==1)
                {
                    curr_loation_flag=0;
                    polygon_points.add(current_point);
                    all_markers.add(new Marker(map));
                }
                if(btn_save_current_marker!=null)
                {
                    map.getOverlays().remove(currentLocationMarker);
                    currentLocationMarker=null;
                    btn_save_current_marker.setVisibility(View.INVISIBLE);

                }
                if(dest.isEmpty() || dest.length() == 0 || dest.equals("") || dest == null) {
                    dest = "defaultMcs";
                }

                if(imp.isEmpty() || img.length() ==0 || imp.equals("") || imp == null){
                    imp = "50";
                }
                total_file=0;
                new FileTask().execute(imp,dest,polygon_points,map,text_description);
                dialog.dismiss();
            }
        });

        discard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(UI_Map.this);
                alertDialogBuilder
                        .setMessage(R.string.files_discard_msg)
                        .setCancelable(false)
                        .setPositiveButton(R.string.discard_files,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        File dir = new File(Environment.getExternalStorageDirectory() + "/DMS/tmp");
                                        if (Reset.deleteContents(dir)) {
                                            Toast.makeText(UI_Map.this, R.string.files_discarded, Toast.LENGTH_SHORT).show();
                                            total_file=0;
                                        }
                                        dialog.dismiss();
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
                dialog.dismiss();
            }
        });
    }



    public static void setWorkingData(final boolean is_mine){
        File working = Environment.getExternalStoragePublicDirectory("DMS/Working");
        File[] files = working.listFiles();
        for(final File file : files){
            if(file.getName().contains("MapDisarm")){
                continue;
            }

            if(all_kmz_overlay_map.containsKey(file.getName())){
                continue;
            }
            if(!isValid(file))
                continue;
            all_kmz_overlay_map.put(file.getName(),true);
            final KmlDocument kml = new KmlDocument();
            if(file.getName().contains("kmz")){
                kml.parseKMZFile(file);
            }
            else{
                kml.parseKMLFile(file);
            }
            if(!first_time && !is_mine) {
                Log.d("Media", "Before");
                MediaPlayer thePlayer = MediaPlayer.create(contextOfApplication, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                thePlayer.start();
                Log.d("Media", "After");
            }
            final FolderOverlay kmlOverlay = (FolderOverlay)kml.mKmlRoot.buildOverlay(map, null, null, kml);
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    for(int i=0;i<kmlOverlay.getItems().size();i++){
                        if(kmlOverlay.getItems().get(i) instanceof Polygon){
                            String desciption = ((Polygon) kmlOverlay.getItems().get(i)).getSnippet();
                            String latlon = kml.mKmlRoot.getExtendedData("Lat Long");
                            if(!first_time && !is_mine){
                                ((Polygon) kmlOverlay.getItems().get(i)).setStrokeColor(Color.BLUE);
                            }
                            ((Polygon) kmlOverlay.getItems().get(i)).setInfoWindow(new CustomInfoWindow(R.layout.custom_info_window,map,desciption,latlon,file.getName()));
                            allOverlays.add(((Polygon) kmlOverlay.getItems().get(i)));
                        }
                        else if(kmlOverlay.getItems().get(i) instanceof Marker){
                            String description = ((Marker) kmlOverlay.getItems().get(i)).getSnippet();
                            String latlon = kml.mKmlRoot.getExtendedData("Lat Long");
                            Drawable draw = null;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                if(!first_time && !is_mine)
                                    draw = getContextOfApplication().getDrawable(R.drawable.marker_blue);
                                else
                                    draw = getContextOfApplication().getDrawable(R.drawable.marker_red);
                            }
                            ((Marker) kmlOverlay.getItems().get(i)).setIcon(draw);
                            ((Marker) kmlOverlay.getItems().get(i)).setInfoWindow(new CustomInfoWindow(R.layout.custom_info_window,map,description,latlon,file.getName()));
                            allOverlays.add(((Marker) kmlOverlay.getItems().get(i)));
                        }
                    }
                }
            });
            t.start();
            map.getOverlays().add(kmlOverlay);
        }
        first_time=false;
    }
    private void refreshWorkingData(){

        Runnable r = new Runnable() {
            @Override
            public void run() {
                setWorkingData(false);
                refresh.postDelayed(this,5000);
            }
        };
        refresh.postDelayed(r,5000);
    }
    private void removeInfo(){
        Boolean isOpen = false;
        try{
            for(Overlay overlay : allOverlays){
                if(overlay instanceof Polygon){
                    if(((Polygon) overlay).getInfoWindow().isOpen()){
                        isOpen = true;
                        ((Polygon) overlay).getInfoWindow().close();
                    }

                }
                else if(overlay instanceof Marker){
                    if(((Marker) overlay).getInfoWindow().isOpen())
                    {
                        isOpen = true;
                        ((Marker) overlay).getInfoWindow().close();
                    }
                }
            }
        }
        catch (Exception ex){

        }

    }
    public static Context getContextOfApplication(){
        return contextOfApplication;
    }

    public static boolean isValid(File file){
        ZipFile zipfile = null;
        try {
            zipfile = new ZipFile(file);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                if (zipfile != null) {
                    zipfile.close();
                    zipfile = null;
                }
            } catch (IOException e) {
            }
        }
    }
}