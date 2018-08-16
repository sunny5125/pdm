package com.disarm.surakshit.pdm.Merging;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import com.disarm.surakshit.pdm.DB.DBEntities.App;
import com.disarm.surakshit.pdm.DB.DBEntities.Receiver;
import com.disarm.surakshit.pdm.DB.DBEntities.Sender;
import com.disarm.surakshit.pdm.Merging.MergeUtil.KmlObject;
import com.disarm.surakshit.pdm.Merging.MergeUtil.MergeDecisionPolicy;
import com.disarm.surakshit.pdm.Merging.MergeUtil.MergePolicy;
import com.disarm.surakshit.pdm.Merging.SoftTfidf.JaroWinklerTFIDF;
import com.snatik.storage.Storage;

import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.objectbox.Box;

/**
 * Created by aman on 6/28/18.
 */

public class GISMerger {

    //return number of not merged files
    public static void mergeGIS(Application app, MapView mapView, MergeDecisionPolicy mergeDecisionPolicy) {

        int totalNotMergedFiles = 0;
        int totalMergedFiles = 0;
        Storage storage = new Storage(mapView.getContext());
        List<KmlObject> kmlObjects = getKMLObjectsFromDB(app, mapView);
        Map<String, List<KmlObject>> sameTileObjects = new HashMap<>();

        //dividing kmlObjects into buckets of same tile name
        for (KmlObject object : kmlObjects) {
            if (sameTileObjects.get(object.getTileName()) == null) {
                sameTileObjects.put(object.getTileName(), new ArrayList<KmlObject>());
            }
            sameTileObjects.get(object.getTileName()).add(object);
        }

        //use single bucket for all objects
//        sameTileObjects.clear();
//        sameTileObjects.put("singleTile", kmlObjects);

        //delete previous merged files
        File mergeDirectory = Environment.getExternalStoragePublicDirectory(MergeConstants.DMS_MERGED_KML);
        storage.deleteDirectory(mergeDirectory.getAbsolutePath());

        //recording time just before merging
        long tStart = System.currentTimeMillis();

        //For each bucket
        //comparing kmlObject of each bucket
        for (String tileName : sameTileObjects.keySet()) {
            List<KmlObject> bucket = sameTileObjects.get(tileName);
            List<KmlObject> mergedBucket = new ArrayList<>();
            while (bucket.size() > 0) {
                boolean merged = false;
                KmlObject X = bucket.get(0);
                bucket.remove(0);
                List<KmlObject> newBucket = new ArrayList<>();
                //comparing first object 'X' of the bucket with the others
                for (int i = 0; i < bucket.size(); i++) {
                    double tfidfScore = new JaroWinklerTFIDF().score(X.getMessage(), bucket.get(i).getMessage());
                    double housDroff = housedorffDistance(X, bucket.get(i));
                    boolean toMerge = mergeDecisionPolicy.mergeDecider(tfidfScore, housDroff);
                    //if it can be merged then merge and update the object 'X'
                    if (toMerge) {
                        MergePolicy mergePolicy = new MergePolicy(MergePolicy.CONVEX_HULL);
                        List<GeoPoint> mergedPoints = mergePolicy.mergeKmlObjects(X, bucket.get(i));
                        String mergedMessage = X.getMessage() + " , " + bucket.get(i).getMessage();
                        String mergedSource = X.getSource() + " , " + bucket.get(i).getMessage();
                        KmlObject mergeKmlObject = new KmlObject(bucket.get(i).getZoom()
                                , bucket.get(i).getType()
                                , mergedPoints
                                , mergedMessage
                                , mergedSource
                                , tileName);
//                        if (X.getTag() == null)
//                            mergeKmlObject.setTag(bucket.get(i).getTag());
//                        else
//                            mergeKmlObject.setTag(X.getTag() + "$" + bucket.get(i).getTag());
                        X = mergeKmlObject;
                        merged = true;
                    }
                    //objects in the bucket that aren't compatible are put in a new bucket
                    else
                        newBucket.add(bucket.get(i));
                }
                if (merged) {
                    //keep the merged 'X' object in a new bucket that will be saved in file
                    mergedBucket.add(X);
                    //count the number of messages in 'X' to calculate number of objects merged
//                    String[] message = X.getMessage().split(",");
//                    totalMergedFiles += message.length;
                }
                //repeat the process of merging for objects that haven't merged
                ListCopy(bucket, newBucket);
            }
            //save to file merged objects
            saveKmlObjectInFile(mergedBucket);
        }
        //recording time after merging
        long tEnd = System.currentTimeMillis();
        long tDelta = tEnd - tStart;
        double elapsedSeconds = tDelta / 1000.0;
//        totalNotMergedFiles = file.listFiles().length - totalMergedFiles;
        Log.d("Time", "T in seconds:" + elapsedSeconds);
//        Log.d("Total Merged Files", "" + totalMergedFiles);
//        Log.d("Total Not Merged Files", "" + totalNotMergedFiles);
//        return totalNotMergedFiles;
    }

    //helper method to copy elements between objects
    private static void ListCopy(List<KmlObject> dest, List<KmlObject> source) {
        dest.clear();
        dest.addAll(source);
    }

    //save List of KmlObjects in file
    private static void saveKmlObjectInFile(List<KmlObject> newBucket) {
        for (KmlObject object : newBucket) {
            saveKMlInFile(object);
        }
    }

    //Method to calculate Hausdorff Distance between two Polygons
    public static double housedorffDistance(KmlObject object1, KmlObject object2) {
        double hDistance1, hDistance2;
        hDistance1 = hDistance2 = Double.MIN_VALUE;
        for (GeoPoint GeoPoint1 : object1.getPoints()) {
            double mind = Double.MAX_VALUE;
            for (GeoPoint GeoPoint2 : object2.getPoints()) {
                double d = distance(GeoPoint1.getLatitude(), GeoPoint2.getLatitude(), GeoPoint1.getLongitude(), GeoPoint2.getLongitude(), GeoPoint1.getAltitude(), GeoPoint2.getAltitude());
                if (d < mind)
                    mind = d;
            }
            if (hDistance1 < mind)
                hDistance1 = mind;
        }
        for (GeoPoint GeoPoint1 : object2.getPoints()) {
            double mind = Double.MAX_VALUE;
            for (GeoPoint GeoPoint2 : object1.getPoints()) {
                double d = distance(GeoPoint1.getLatitude(), GeoPoint2.getLatitude(), GeoPoint1.getLongitude(), GeoPoint2.getLongitude(), GeoPoint1.getAltitude(), GeoPoint2.getAltitude());
                if (d < mind)
                    mind = d;
            }
            if (hDistance2 < mind)
                hDistance2 = mind;
        }
        if (hDistance1 > hDistance2)
            return hDistance1;
        return hDistance2;
    }

    //Method to form KMLObject from values
    //returns a kmlObject
    public static KmlObject getKMLObject(String sourceId, String message, List<GeoPoint> polyPoints, int type) {
        KmlObject object = new KmlObject();
        object.setMessage(message);
        object.setPoints(polyPoints);
        object.setSource(sourceId);
        object.setType(type);
        object = addTileNameAndLevel(object);
        return object;
    }

    //Method to form Tagged KmlObjects from File
//    public static KmlObject getTaggedKMlObject(File file, MapView mapView) {
//        String sourceid = file.getName().split("_")[3];
//        KmlDocument kml = new KmlDocument();
//        kml.parseKMLFile(file);
//        KmlObject kmlObject = new KmlObject();
//        String tag = kml.mKmlRoot.getExtendedData(MergeConstants.EXTENDED_DATA_TAG);
//        FolderOverlay kmlOverlay = (FolderOverlay) kml.mKmlRoot.buildOverlay(mapView, null, null, kml);
//        for (int i = 0; i < kmlOverlay.getItems().size(); i++) {
//            if (kmlOverlay.getItems().get(i) instanceof org.osmdroid.views.overlay.Polygon) {
//                List<GeoPoint> polyPoints = ConversionUtil.getGeoPointList(((org.osmdroid.views.overlay.Polygon) kmlOverlay.getItems().get(i)).getPoints());
//                String message = ((org.osmdroid.views.overlay.Polygon) kmlOverlay.getItems().get(i)).getSnippet();
//                kmlObject = getKMLObject(sourceid, message, polyPoints, KmlObject.KMLOBJECT_TYPE_POLYGON, file);
//
//            } else if (kmlOverlay.getItems().get(i) instanceof org.osmdroid.views.overlay.Marker) {
//                GeoPoint point = ConversionUtil.getGeoPoint(((org.osmdroid.views.overlay.Marker) kmlOverlay.getItems().get(i)).getPosition());
//                String message = ((org.osmdroid.views.overlay.Marker) kmlOverlay.getItems().get(i)).getSnippet();
//                List<GeoPoint> pointList = new ArrayList<>();
//                pointList.add(point);
//                kmlObject = getKMLObject(sourceid, message, pointList, KmlObject.KMLOBJECT_TYPE_MARKER, file);
//            }
//        }
//        kmlObject.setTag(tag);
//        return kmlObject;
//    }

    //Method to find the tile name and zoom level of KmlObject
    public static KmlObject addTileNameAndLevel(KmlObject kmlobject) {
        List<GeoPoint> GeoPoints = kmlobject.getPoints();
        int minLevel = 10;
        int maxLevel = 20;
        int currentLevel = 15;
        int zoomLevel = binarySearchZoom(minLevel, maxLevel, currentLevel, GeoPoints);
        String tileName = getTileNumber(GeoPoints.get(0).getLatitude(), GeoPoints.get(0).getLongitude(), zoomLevel);
        kmlobject.setZoom(zoomLevel);
        kmlobject.setTileName(tileName);
        return kmlobject;

    }

    //returns the smallest fit zoom level of the polygon
    private static int binarySearchZoom(int minLevel, int maxLevel, int currentLevel, List<GeoPoint> GeoPoints) {
        if (currentLevel <= minLevel) {
            return currentLevel;
        }
        if (currentLevel >= maxLevel) {
            return currentLevel;
        }
        boolean allInSameZoom = true;
        String previousTileName = null;
        for (GeoPoint ll : GeoPoints) {
            double lat = ll.getLatitude();
            double lon = ll.getLongitude();
            String tileName = getTileNumber(lat, lon, currentLevel);
            if (previousTileName == null) {
                previousTileName = tileName;
            }
            if (!previousTileName.equals(tileName)) {
                allInSameZoom = false;
                break;
            }
        }

        if (allInSameZoom) {
            return binarySearchZoom(currentLevel, maxLevel, currentLevel + ((maxLevel - currentLevel) / 2), GeoPoints);
        } else {
            return binarySearchZoom(minLevel, currentLevel - 1, (currentLevel - (currentLevel - minLevel) / 2), GeoPoints);
        }
    }

    //returns the Tile name wrt to lat long and zoom level
    public static String getTileNumber(final double lat, final double lon, final int zoom) {
        int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
        int ytile = (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoom));
        if (xtile < 0)
            xtile = 0;
        if (xtile >= (1 << zoom))
            xtile = ((1 << zoom) - 1);
        if (ytile < 0)
            ytile = 0;
        if (ytile >= (1 << zoom))
            ytile = ((1 << zoom) - 1);
        return ("" + zoom + "/" + xtile + "/" + ytile);
    }

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     * <p>
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     *
     * @returns Distance in Meters
     */
    public static double distance(double lat1, double lat2, double lon1,
                                  double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    private static File saveKMlInFile(KmlObject kmlObject) {
        String file_name = "TXT_50_data_" +
                kmlObject.getSource() +
                "_" + kmlObject.hashCode() + "_"
                + ".kml";
        KmlDocument kml = new KmlDocument();
        Polygon polygon = new Polygon();
        polygon.setPoints(kmlObject.getPoints());
        polygon.setSnippet(kmlObject.getMessage());
        kml.mKmlRoot.addOverlay(polygon, kml);
//        kml.mKmlRoot.setExtendedData(Constants.EXTENDED_DATA_TAG, kmlObject.getTag());
        File mergeDirectory = Environment.getExternalStoragePublicDirectory(MergeConstants.DMS_MERGED_KML);
        File mergeFile = Environment.getExternalStoragePublicDirectory(MergeConstants.DMS_MERGED_KML + file_name);
        if (!mergeDirectory.exists())
            mergeDirectory.mkdir();
        kml.saveAsKML(mergeFile);
        return mergeFile;
    }

    public static ArrayList<KmlObject> getKMLObjectsFromDB(Application app, MapView mapView) {
        ArrayList<KmlObject> kmlObjects = new ArrayList<>();
        Box<Receiver> receiverBox = ((App) app).getBoxStore().boxFor(Receiver.class);
        Box<Sender> senderBox = ((App) app).getBoxStore().boxFor(Sender.class);
        List<Receiver> receivers = receiverBox.getAll();
        List<Sender> senders = senderBox.getAll();
        for (Receiver receiver : receivers) {
            String kmlString = receiver.getKml();
            ByteArrayInputStream is = new ByteArrayInputStream(kmlString.getBytes());
            KmlDocument kml = new KmlDocument();
            kml.parseKMLStream(is, null);
            FolderOverlay folderOverlay = (FolderOverlay) kml.mKmlRoot.buildOverlay(mapView, null, null, kml);
            for (Overlay overlay : folderOverlay.getItems()) {
                Log.d("Merging", "Receiver"+receiver.getNumber());
                if (overlay instanceof Polygon) {
                    String id = ((Polygon) overlay).getTitle();
//                    String snippet = ((Polygon) overlay).getSnippet();
                    List<GeoPoint> points = ((Polygon) overlay).getPoints();
                    KmlObject object = new KmlObject();
                    object.setMessage(id);
                    object.setPoints(points);
                    object.setSource(receiver.getNumber());
                    object.setType(KmlObject.KMLOBJECT_TYPE_POLYGON);
                    object = addTileNameAndLevel(object);
                    Log.d("Merging", "Id Source TileName:" + object.getMessage() + " " + receiver.getNumber() + " " + object.getTileName());
                    kmlObjects.add(object);
                } else if (overlay instanceof Marker) {
                    String id = ((Marker) overlay).getTitle();
                    GeoPoint point = ((Marker) overlay).getPosition();
                }
            }
        }
        for (Sender sender : senders) {
            Log.d("Merging", "Sender"+sender.getNumber());
            String kmlString = sender.getKml();
            ByteArrayInputStream is = new ByteArrayInputStream(kmlString.getBytes());
            KmlDocument kml = new KmlDocument();
            kml.parseKMLStream(is, null);
            FolderOverlay folderOverlay = (FolderOverlay) kml.mKmlRoot.buildOverlay(mapView, null, null, kml);
            for (Overlay overlay : folderOverlay.getItems()) {
                if (overlay instanceof Polygon) {
                    String id = ((Polygon) overlay).getTitle();
//                    String snippet = ((Polygon) overlay).getSnippet();
                    List<GeoPoint> points = ((Polygon) overlay).getPoints();
                    KmlObject object = new KmlObject();
                    object.setMessage(id);
                    object.setPoints(points);
                    object.setSource(sender.getNumber());
                    object.setType(KmlObject.KMLOBJECT_TYPE_POLYGON);
                    object = addTileNameAndLevel(object);
                    Log.d("Merging", "Id Source TileName:" + object.getMessage() + " " + sender.getNumber() + " " + object.getTileName());
                    kmlObjects.add(object);
                } else if (overlay instanceof Marker) {
                    String id = ((Marker) overlay).getTitle();
                    GeoPoint point = ((Marker) overlay).getPosition();
                }
            }
        }


        /* Mark k and tk to each file */
//        for (File kmlFile : file.listFiles()) {
//            String sourceid = kmlFile.getName().split("_")[3];
//            KmlDocument kml = new KmlDocument();
//            if (kmlFile.getName().contains("kml")) {
//                kml.parseKMLFile(kmlFile);
//                String tag = kml.mKmlRoot.getExtendedData(MergeConstants.EXTENDED_DATA_TAG);
//                final FolderOverlay kmlOverlay = (FolderOverlay) kml.mKmlRoot.buildOverlay(mapView, null, null, kml);
//                for (int i = 0; i < kmlOverlay.getItems().size(); i++) {
//                    if (kmlOverlay.getItems().get(i) instanceof org.osmdroid.views.overlay.Polygon) {
//                        List<GeoPoint> polyPoints = ((Polygon) kmlOverlay.getItems().get(i)).getPoints();
//                        String message = ((org.osmdroid.views.overlay.Polygon) kmlOverlay.getItems().get(i)).getSnippet();
//                        KmlObject kmlObject = getKMLObject(sourceid, message, polyPoints, KmlObject.KMLOBJECT_TYPE_POLYGON, kmlFile);
//                        kmlObject.setTag(tag);
//                        kmlObjects.add(kmlObject);
//
//                    } else if (kmlOverlay.getItems().get(i) instanceof org.osmdroid.views.overlay.Marker) {
//                        GeoPoint point = ((Marker) kmlOverlay.getItems().get(i)).getPosition();
//                        String message = ((org.osmdroid.views.overlay.Marker) kmlOverlay.getItems().get(i)).getSnippet();
//                    }
//                }
//            }
//        }
        return kmlObjects;
    }
}
