package com.disarm.surakshit.pdm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.location.Location;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.disarm.surakshit.pdm.Chat.ChatActivity;
import com.disarm.surakshit.pdm.Chat.Utils.ChatUtils;
import com.disarm.surakshit.pdm.DB.DBEntities.App;
import com.disarm.surakshit.pdm.DB.DBEntities.Sender;
import com.disarm.surakshit.pdm.DB.DBEntities.Sender_;
import com.disarm.surakshit.pdm.Encryption.KeyBasedFileProcessor;
import com.disarm.surakshit.pdm.Encryption.SignedFileProcessor;
import com.disarm.surakshit.pdm.Util.DiffUtils;
import com.disarm.surakshit.pdm.Util.LatLonUtil;
import com.disarm.surakshit.pdm.Util.Params;
import com.disarm.surakshit.pdm.location.MLocation;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.kml.KmlPlacemark;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import io.objectbox.Box;

public class CollectMapDataActivity extends AppCompatActivity {

    MapView map;
    final int MIN_ZOOM=14,MAX_ZOOM=19,PIXEL=256;
    final int RESULT_OK =-1;
    Marker currentPosition;
    final Polygon polygon = new Polygon();
    Location l;
    KmlDocument kml;
    File kmlFile;
    FloatingActionButton fab;
    Button draw_save,undo_back,cancel,btn_save_current_marker;
    int flag = 0;
    int draw_flag=1,diff_flag=0;
    ArrayList<GeoPoint> polygon_points=new ArrayList<>();
    ArrayList<Marker> all_markers = new ArrayList<>();
    ArrayList<Marker> markerpoints=new ArrayList<>();
    String kmlFileName;
    String number;
    Boolean isKey;
    boolean source = false, curr = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_collect_map_data);
        kmlFileName = getIntent().getStringExtra("kml");
        number = getIntent().getStringExtra("number");
        isKey = getIntent().getBooleanExtra("key",false);
        if(isKey)
            kmlFile = Environment.getExternalStoragePublicDirectory("DMS/KML/Source/LatestKml/"+kmlFileName);
        else{
            kmlFile = Environment.getExternalStoragePublicDirectory("DMS/temp/"+kmlFileName);
        }
        kml = new KmlDocument();
        if(kmlFile.exists()) {
            kml.parseKMLFile(kmlFile);
            source = true;
        }
        else
            kml.mKmlRoot.setExtendedData("total", "0");
        //initialize mapView
        setMapData();
        fab = findViewById(R.id.fab_add_data);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fab.setVisibility(View.INVISIBLE);
                draw_save.setVisibility(View.VISIBLE);
                cancel.setVisibility(View.VISIBLE);
                btn_save_current_marker.setVisibility(View.INVISIBLE);
                undo_back.setVisibility(View.VISIBLE);
                polygon_points.clear();
                flag=0;

            }
        });

        draw_save = findViewById(R.id.btn_mapActivity_draw_save);
        cancel = findViewById(R.id.btn_mapActivity_cancel);
        undo_back = findViewById(R.id.btn_mapActivity_undo_back);
        btn_save_current_marker = findViewById(R.id.btn_mapActivity_current_loc);
        setCancelClick();
        setDrawClick();
        setSaveClick();
        btn_save_current_marker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                draw_save.setVisibility(View.GONE);
                cancel.setVisibility(View.GONE);
                undo_back.setVisibility(View.GONE);
                btn_save_current_marker.setVisibility(View.GONE);
                fab.setVisibility(View.GONE);
                curr = true;
                Bitmap bmp = takeScreenshot();
                String mapFileName = FilenameUtils.getBaseName(kmlFileName);
                mapFileName = mapFileName + "_" + generateRandomString() + ".png";
                String path;
                if(isKey){
                    path = "DMS/Working/SurakshitMap/";
                }
                else{
                    path = "DMS/tempMedia/";
                }
                File f = Environment.getExternalStoragePublicDirectory(path+mapFileName);
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(f);
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                String uniqueId = generateRandomString();
                currentPosition.setTitle(uniqueId);
                KmlPlacemark kmlPlacemark = new KmlPlacemark(currentPosition);
                kml.mKmlRoot.add(kmlPlacemark);
                String lastKey = getLatestKey();
                String message = ChatUtils.getExtendedDataFormatName(mapFileName,"map",uniqueId);
                kml.mKmlRoot.setExtendedData(lastKey,message);
                int total = Integer.parseInt(kml.mKmlRoot.getExtendedData("total"));
                total++;
                kml.mKmlRoot.setExtendedData("total", total + "");
                kml.saveAsKML(kmlFile);
                if(!source){
                    if(isKey) {
                        File dest = Environment.getExternalStoragePublicDirectory("DMS/KML/Source/SourceKml/" + FilenameUtils.getBaseName(kmlFile.getName()) + ".kml");
                        try {
                            FileUtils.copyFile(kmlFile, dest);
                            encryptIt(dest);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                final Box<Sender> senderBox = ((App) getApplication()).getBoxStore().boxFor(Sender.class);
                List<Sender> senderList = senderBox.query().contains(Sender_.number, number).build().find();
                Sender s;
                if(senderList.size()==0){
                    s = new Sender();
                    s.setNumber(number);
                }
                else {
                    s = senderList.get(0);
                    diff_flag = 1;
                }
                s.setLastUpdated(true);
                s.setLastMessage(message);
                String kmlString = "";
                try {
                    assert kmlFile != null;
                    kmlString = FileUtils.readFileToString(kmlFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                s.setKml(kmlString);
                senderBox.put(s);
                senderBox.closeThreadResources();
                if(diff_flag == 1 && isKey)
                        generateDiff(kmlFile);
                else{
                    setResult(RESULT_OK);
                    finish();
                }
            }
        });
        l = MLocation.getLocation(getApplicationContext());
        if(l == null){
            //Code to get user's location
            currentPosition = new Marker(map);
            currentPosition.setPosition(ChatActivity.currLoc);
            currentPosition.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker, MapView mapView) {
                    return false;
                }
            });
            map.getOverlays().add(currentPosition);
        }
        else{
            currentPosition = new Marker(map);
            currentPosition.setPosition(new GeoPoint(l.getLatitude(),l.getLongitude()));
            currentPosition.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker, MapView mapView) {
                    return false;
                }
            });
            map.getOverlays().add(currentPosition);
        }
        setMapTouch();

    }

    public void setMapData(){
        map = findViewById(R.id.map_collect_data);
        ITileSource tileSource = new XYTileSource("tiles",MIN_ZOOM,MAX_ZOOM,PIXEL,".png",new String[]{});
        map.setTileSource(tileSource);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        final IMapController mapController = map.getController();
        mapController.setZoom(15.0);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final GeoPoint startPoint = LatLonUtil.getBoundaryOfTiles();
                if(startPoint!=null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mapController.setCenter(startPoint);
                        }
                    });

                }
            }
        });
        thread.start();
        CompassOverlay mCompassOverlay = new CompassOverlay(ctx, new InternalCompassOrientationProvider(ctx), map);
        mCompassOverlay.enableCompass();
        map.getOverlays().add(mCompassOverlay);
        ScaleBarOverlay mScaleBarOverlay = new ScaleBarOverlay(map);
        mScaleBarOverlay.setCentred(true);
        mScaleBarOverlay.setScaleBarOffset(width/2, 10);
        map.getOverlays().add(mScaleBarOverlay);
    }

    public void setMapTouch(){
        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                draw_save.setEnabled(true);
                undo_back.setEnabled(true);
                if(draw_save.getVisibility()==View.VISIBLE && undo_back.getText().toString().equals(getString(R.string.undo))){
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
                    return true;
                }
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return true;
            }
        };
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(mapEventsReceiver);
        map.getOverlays().add(mapEventsOverlay);
    }

    public Bitmap takeScreenshot(){
        if(!curr){
            map.getOverlays().remove(currentPosition);
        }
        View view = getWindow().getDecorView().getRootView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache(true);
        Bitmap b = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);
        return b;
    }

    private void setDrawClick(){
        draw_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // draw btn
                if(flag==0)
                {
                    if(polygon_points.size()!=0)
                    {
                        polygon.setPoints(polygon_points);
                        map.getOverlays().add(polygon);
                        map.invalidate();
                        undo_back.setText(R.string.back);
                        draw_save.setText(R.string.save);
                        flag = 1;
                    }
                    else
                        Toast.makeText(getBaseContext(), "No marker is selected.", Toast.LENGTH_SHORT).show();
                }
                else {
                    //Save Part
                    draw_save.setVisibility(View.INVISIBLE);
                    undo_back.setVisibility(View.INVISIBLE);
                    cancel.setVisibility(View.INVISIBLE);
                    btn_save_current_marker.setVisibility(View.INVISIBLE);
                    fab.setVisibility(View.INVISIBLE);
                    Bitmap bmp = takeScreenshot();
                    String mapFileName = FilenameUtils.getBaseName(kmlFileName);
                    mapFileName = mapFileName + "_" + generateRandomString() + ".png";
                    String path;
                    if(isKey){
                        path = "DMS/Working/SurakshitMap/";
                    }
                    else{
                        path = "DMS/tempMedia/";
                    }
                    File f = Environment.getExternalStoragePublicDirectory(path+mapFileName);
                    FileOutputStream out = null;
                    try {
                        out = new FileOutputStream(f);
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    finally {
                        try {
                            if (out != null) {
                                out.close();
                            }
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    String uniqueId = generateRandomString();
                    if(polygon_points.size() == 1) {
                        Marker marker = new Marker(map);
                        marker.setPosition(polygon_points.get(0));
                        marker.setTitle(uniqueId);
                        KmlPlacemark kmlPlacemark = new KmlPlacemark(marker);
                        kml.mKmlRoot.add(kmlPlacemark);
                    }
                    else{
                        polygon.setPoints(polygon_points);
                        polygon.setTitle(uniqueId);
                        kml.mKmlRoot.addOverlay(polygon,kml);
                    }
                        String lastKey = getLatestKey();
                        String message = ChatUtils.getExtendedDataFormatName(mapFileName,"map",uniqueId);
                        kml.mKmlRoot.setExtendedData(lastKey,message);
                        int total = Integer.parseInt(kml.mKmlRoot.getExtendedData("total"));
                        total++;
                        kml.mKmlRoot.setExtendedData("total", total + "");
                        kml.saveAsKML(kmlFile);
                        if(!source && isKey){
                            File dest = Environment.getExternalStoragePublicDirectory("DMS/KML/Source/SourceKml/"+FilenameUtils.getBaseName(kmlFile.getName()) + ".kml");
                            try {
                                FileUtils.copyFile(kmlFile,dest);
                                encryptIt(dest);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        final Box<Sender> senderBox = ((App) getApplication()).getBoxStore().boxFor(Sender.class);
                        List<Sender> senderList = senderBox.query().contains(Sender_.number, number).build().find();
                        Sender s;
                        if(senderList.size()==0){
                            s = new Sender();
                            s.setNumber(number);
                        }
                        else {
                            s = senderList.get(0);
                            diff_flag = 1;
                        }
                        s.setLastUpdated(true);
                        s.setLastMessage(message);
                        String kmlString = "";
                        try {
                            assert kmlFile != null;
                            kmlString = FileUtils.readFileToString(kmlFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        s.setKml(kmlString);
                        senderBox.put(s);
                        senderBox.closeThreadResources();
                        if(diff_flag == 1 && isKey)
                            generateDiff(kmlFile);
                        else{
                            setResult(RESULT_OK);
                            finish();
                        }


                }
            }
        });
    }

    private void setCancelClick(){
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                undo_back.setText(R.string.undo);
                flag=0;
                draw_save.setText(R.string.draw);
                fab.setVisibility(View.VISIBLE);
                if(currentPosition!=null)
                    btn_save_current_marker.setVisibility(View.VISIBLE);
                draw_save.setVisibility(View.GONE);
                cancel.setVisibility(View.GONE);
                undo_back.setVisibility(View.GONE);
                draw_flag=1;
                map.getOverlays().remove(polygon);
                polygon_points.clear();
                for(int i=0;i<all_markers.size();i++){
                    map.getOverlays().remove(all_markers.get(i));
                }
                map.invalidate();
            }
        });
    }


    private void setSaveClick(){
        undo_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(undo_back.getText().toString().equals("UNDO"))
                {
                    if (markerpoints.size() != 0 && polygon_points.size() != 0) {
                        markerpoints.get(markerpoints.size() - 1).remove(map);
                        markerpoints.remove(markerpoints.size() - 1);
                        polygon_points.remove(polygon_points.size() - 1);

                    }
                    else{
                        Toast.makeText(getBaseContext(), "There is no marker ", Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    flag=0;
                    draw_save.setText(R.string.draw);
                    undo_back.setText(R.string.undo);
                    map.getOverlays().remove(polygon);
                }
            }
        });
    }

    private String generateRandomString(){
        SecureRandom secureRandom = new SecureRandom();
        byte[] token = new byte[8];
        secureRandom.nextBytes(token);
        return new BigInteger(1, token).toString(16);
    }

    private String getLatestKey(){
        String nextKey = "source";
        String msg;
        while (kml.mKmlRoot.mExtendedData!=null && kml.mKmlRoot.mExtendedData.containsKey(nextKey)){
            msg = kml.mKmlRoot.getExtendedData(nextKey);
            nextKey = getTimeStampFromMsg(msg);
        }
        return nextKey;
    }
    private String getTimeStampFromMsg(String msg){
        Pattern p = Pattern.compile("-");
        String[] s = p.split(msg,4);
        return s[0];
    }

    private void generateDiff(final File dest){
        final HandlerThread ht = new HandlerThread("Diff");
        ht.start();
        Handler diffHandler = new Handler(ht.getLooper());
        diffHandler.post(new Runnable() {
            @Override
            public void run() {
                File source =null;
                File sourceDir = Environment.getExternalStoragePublicDirectory("DMS/KML/Source/SourceKml");
                for(File file : sourceDir.listFiles()){
                    if(file.getName().contains(number)){
                        source =file;
                        break;
                    }
                }
                try {
                    DiffUtils.createDiff(source,dest,getApplication(),CollectMapDataActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ht.quit();
                setResult(RESULT_OK);
                finish();
            }
        });
    }
    private void encryptIt(File file){
        String inputPath = file.getAbsolutePath();
        String publicKeyPath = Environment.getExternalStoragePublicDirectory("DMS/Working/pgpKey/pub_"+number+".bgp").getAbsolutePath();
        String outputFilePath = Environment.getExternalStoragePublicDirectory("DMS/Working/SurakshitKml/"
                +FilenameUtils.getBaseName(file.getName())+".bgp")
                .getAbsolutePath();
        try {
            KeyBasedFileProcessor.encrypt(inputPath,publicKeyPath,outputFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void signAndEncrypt(File file, String broadcastTo) throws IOException {
        String inputPath = file.getAbsolutePath();
        String secretKeyPath;
        String outputPath = Environment.getExternalStoragePublicDirectory("DMS/temp/"+FilenameUtils.getBaseName(file.getName())+".asc").getAbsolutePath();
        if(broadcastTo.equals("user") || broadcastTo.equals("volunteer")){
            secretKeyPath = Environment.getExternalStoragePublicDirectory("DMS/Working/pgpKey/pri_"+broadcastTo+".bgp").getAbsolutePath();
        }
        else{
            secretKeyPath = Environment.getExternalStoragePublicDirectory("DMS/pgpPrivate/pri_"+number+".bgp").getAbsolutePath();
        }
        SignedFileProcessor signedFileProcessor = new SignedFileProcessor();
        String pass;
        if(broadcastTo.equals("volunteer")){
            pass = "volunteer@disarm321";
        }
        else{
            pass = Params.PASS_PHRASE;
        }
        signedFileProcessor.signFile(inputPath,outputPath,secretKeyPath,pass);
        String publicKeyPath = Environment.getExternalStoragePublicDirectory("DMS/Working/pgpKey/pub_"+number+".bgp").getAbsolutePath();
        String outputFilePath = Environment.getExternalStoragePublicDirectory("DMS/Working/SurakshitKml/"
                +FilenameUtils.getBaseName(file.getName())+".bgp")
                .getAbsolutePath();
        try {
            KeyBasedFileProcessor.encrypt(outputPath,publicKeyPath,outputFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        FileUtils.forceDelete(Environment.getExternalStoragePublicDirectory("DMS/temp/temp.asc"));
    }
}
