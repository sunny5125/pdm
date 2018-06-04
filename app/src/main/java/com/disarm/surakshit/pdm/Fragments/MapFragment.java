package com.disarm.surakshit.pdm.Fragments;

import android.app.Application;
import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.disarm.surakshit.pdm.DB.DBEntities.App;
import com.disarm.surakshit.pdm.DB.DBEntities.Receiver;
import com.disarm.surakshit.pdm.DB.DBEntities.Sender;
import com.disarm.surakshit.pdm.R;
import com.disarm.surakshit.pdm.Util.CustomInfoWindow;
import com.disarm.surakshit.pdm.Util.LatLonUtil;
import com.disarm.surakshit.pdm.location.MLocation;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.objectbox.Box;

/**
 * Created by naman on 25/2/18.
 */

public class MapFragment extends Fragment {
    public static MapView map;
    final int MIN_ZOOM=14,MAX_ZOOM=19,PIXEL=256;
    public static List<Overlay> allPlotted = new ArrayList<>();
    public static Marker marker;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_map , container , false);
        map = (MapView) view.findViewById(R.id.fragment_mapView);
        setMapData();
        HandlerThread ht = new HandlerThread("Map");
        ht.start();
        parseKml(getActivity().getApplication(),getContext());
        final Handler locHandler = new Handler(ht.getLooper());
        locHandler.post(new Runnable() {
            @Override
            public void run() {
                Location l = MLocation.getLocation(getContext().getApplicationContext());
                if(l == null){
                    locHandler.postDelayed(this,1000);
                }
                else{
                   GeoPoint g = new GeoPoint(l.getLatitude(),l.getLongitude());
                   if(marker==null) {
                       marker = new Marker(map);
                       marker.setPosition(g);
                       marker.setIcon(getContext().getDrawable(R.drawable.marker_google));
                       marker.setSnippet("You are here");
                       map.getOverlays().add(marker);
                   }
                   else {
                       map.getOverlays().remove(marker);
                       marker.setPosition(g);
                       map.getOverlays().add(marker);
                   }
                }
            }
        });
        return view;
    }



    public void setMapData(){
        ITileSource tileSource = new XYTileSource("tiles",MIN_ZOOM,MAX_ZOOM,PIXEL,".png",new String[]{});
        map.setTileSource(tileSource);
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        Context ctx = getActivity().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        map.setBuiltInZoomControls(false);
        map.setMultiTouchControls(true);
        final IMapController mapController = map.getController();
        mapController.setZoom(15.0);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final GeoPoint startPoint = LatLonUtil.getBoundaryOfTiles();
                if(startPoint!=null) {
                    try {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mapController.setCenter(startPoint);
                            }
                        });
                    }
                    catch (Exception e){

                    }

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
        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                for(Overlay overlay : allPlotted){
                    if(overlay instanceof Polygon){
                        ((Polygon) overlay).getInfoWindow().close();
                    }
                    else if(overlay instanceof Marker){
                        ((Marker) overlay).getInfoWindow().close();
                    }
                }
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(mapEventsReceiver);
        map.getOverlays().add(mapEventsOverlay);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void parseKml(final Application app, final Context context){
        HandlerThread handlerThread = new HandlerThread("ParseKML");
        handlerThread.start();
        Handler h = new Handler(handlerThread.getLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                try {
                    final Box<Receiver> receiverBox = ((App) app).getBoxStore().boxFor(Receiver.class);
                    final Box<Sender> senderBox = ((App) app).getBoxStore().boxFor(Sender.class);
                    List<Receiver> receivers = receiverBox.getAll();
                    List<Sender> senders = senderBox.getAll();
                    for(Overlay overlay : map.getOverlays()){
                        if( ! (overlay instanceof MapEventsOverlay)){
                            if( overlay instanceof Marker){
                                if(overlay != marker){
                                    map.getOverlays().remove(overlay);
                                }
                            }
                        }
                    }
                    allPlotted.clear();
                    for(Receiver receiver : receivers){
                        String kmlString = receiver.getKml();
                        ByteArrayInputStream is = new ByteArrayInputStream(kmlString.getBytes());
                        KmlDocument kml = new KmlDocument();
                        kml.parseKMLStream(is,null);

                        FolderOverlay overlay = (FolderOverlay) kml.mKmlRoot.buildOverlay(map,null,null,kml);
                        Object[] objects = extractPointsFromOverlay(overlay);
                        HashMap<String,String> pointsToId = (HashMap<String, String>) objects[1];
                        ArrayList<GeoPoint> points = (ArrayList<GeoPoint>) objects[0];
                        List<Overlay> overlays = (List<Overlay>)objects[2];
                        assignMediaToGISObjects(points,pointsToId,kml,overlays,context,true);
                    }
                    for(Sender sender : senders){
                        String kmlString = sender.getKml();
                        ByteArrayInputStream is = new ByteArrayInputStream(kmlString.getBytes());
                        KmlDocument kml = new KmlDocument();
                        kml.parseKMLStream(is,null);
                        FolderOverlay overlay = (FolderOverlay) kml.mKmlRoot.buildOverlay(map,null,null,kml);
                        Object[] objects = extractPointsFromOverlay(overlay);
                        HashMap<String,String> pointsToId = (HashMap<String, String>) objects[1];
                        ArrayList<GeoPoint> points = (ArrayList<GeoPoint>) objects[0];
                        List<Overlay> overlays = (List<Overlay>)objects[2];
                        assignMediaToGISObjects(points,pointsToId,kml,overlays,context,false);
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

    }

    public static Object[] extractPointsFromOverlay(FolderOverlay folderOverlay){
        HashMap<String,String> pointsToId = new HashMap<>();
        List<GeoPoint> points = new ArrayList<>();
        List<Overlay> overlayList = new ArrayList<>();
        for( Overlay overlay : folderOverlay.getItems()){
            if(overlay instanceof Polygon){
                String id = ((Polygon) overlay).getTitle();
                for(GeoPoint point : ((Polygon) overlay).getPoints()){
                    pointsToId.put(point.toDoubleString(),id);
                    points.add(point);
                }
                overlayList.add(overlay);
            }
            else if(overlay instanceof Marker){
                String id = ((Marker) overlay).getTitle();
                pointsToId.put(((Marker) overlay).getPosition().toDoubleString(),id);
                points.add(((Marker) overlay).getPosition());
                overlayList.add(overlay);
            }
        }
        return new Object[]{points,pointsToId,overlayList};
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void assignMediaToGISObjects(ArrayList<GeoPoint> points, HashMap<String,String> pointsToId, KmlDocument kml, List<Overlay> overlayList, Context context, Boolean color){
        String key = "source";
        HashMap<String,String> idToSnippet = new HashMap<>();
        while(kml.mKmlRoot.mExtendedData.containsKey(key)){
            String msg[] = kml.mKmlRoot.getExtendedData(key).split("-");
            key = msg[0];
            String type = msg[1];
            if(type.contains("image") || type.contains("video") || type.contains("audio")){
                String latlon[] = msg[4].split("_");
                Double lat = Double.parseDouble(latlon[0]);
                Double lon = Double.parseDouble(latlon[1]);
                GeoPoint g = new GeoPoint(lat,lon);
                GeoPoint minDisPoint = null;
                Double minDis = 9999999999.0;
                for(GeoPoint geoPoint : points){
                    Double distance = g.distanceToAsDouble(geoPoint);
                    if( distance < 100){
                        Log.d("Snippet","Minimum distance found");
                        if(minDisPoint == null) {
                            minDisPoint = geoPoint;
                            minDis = distance;
                        }
                        else if ( minDis > distance){
                            minDis = distance;
                            minDisPoint = geoPoint;
                        }
                    }
                }
                if(minDisPoint==null){
                    Marker m = new Marker(map);
                    m.setPosition(g);
                    m.setSnippet(msg[3]);
                    CustomInfoWindow ciw = new CustomInfoWindow(R.layout.custom_info_window,map,m,context);
                    m.setInfoWindow(ciw);
                    if(color) {
                        Drawable d = context.getDrawable(R.drawable.marker_red);
                        m.setIcon(d);
                    }
                    else{
                        Drawable d = context.getDrawable(R.drawable.marker_blue);
                        m.setIcon(d);
                    }
                    map.getOverlays().add(m);
                    Log.d("Snippet","No minimum distance point");
                    allPlotted.add(m);
                }
                else{
                    String id = pointsToId.get(minDisPoint.toDoubleString());
                    if(idToSnippet.containsKey(id)){
                        String f = idToSnippet.get(id);
                        f = f + ";" + msg[3];
                        Log.d("Snippet",f);
                        idToSnippet.put(id,f);
                    }
                    else{
                        idToSnippet.put(id,msg[3]);
                    }
                }
            }
        }
        for(Overlay overlay : overlayList){
            if(overlay instanceof Polygon){
                String id = ((Polygon) overlay).getTitle();
                String snippet = idToSnippet.get(id);
                ((Polygon) overlay).setSnippet(snippet);
                CustomInfoWindow ciw = new CustomInfoWindow(R.layout.custom_info_window,map,(Polygon)overlay,context);
                if(color){
                    ((Polygon) overlay).setStrokeColor(R.color.blue);
                }
                else {
                    ((Polygon) overlay).setStrokeColor(R.color.red);
                }
                ((Polygon) overlay).setInfoWindow(ciw);
                map.getOverlays().add(overlay);
                allPlotted.add(overlay);
            }

            else if(overlay instanceof Marker){
                String id = ((Marker) overlay).getTitle();
                String snippet = idToSnippet.get(id);
                ((Marker) overlay).setSnippet(snippet);
                if(color){
                    Drawable d = context.getDrawable(R.drawable.marker_blue);
                    ((Marker) overlay).setImage(d);
                }
                else{
                    Drawable d = context.getDrawable(R.drawable.marker_red);
                    ((Marker) overlay).setImage(d);
                }
                CustomInfoWindow ciw = new CustomInfoWindow(R.layout.custom_info_window,map,(Marker) overlay,context);
                ((Marker) overlay).setInfoWindow(ciw);
                map.getOverlays().add(overlay);
                allPlotted.add(overlay);
            }
        }
    }
}
