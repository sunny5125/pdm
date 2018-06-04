package com.disarm.surakshit.pdm.Util;

import android.app.Application;
import android.content.Context;
import android.os.Environment;


import com.disarm.surakshit.pdm.Fragments.MapFragment;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.osmdroid.bonuspack.kml.KmlDocument;

import java.io.File;
import java.io.IOException;

import ie.wombat.jbdiff.JBDiff;
import ie.wombat.jbdiff.JBPatch;

/**
 * Created by naman on 24/12/17.
 */

public class DiffUtils {


    public static void createDiff(File source, File destination, Application app, Context context) throws IOException {
        //source - sourceKML
        //destination - latestKML
        File delta = Environment.getExternalStoragePublicDirectory("/DMS/Working/SurakshitDiff/"+getDeltaName(destination));
        try {
            //delta -- diff file name
            JBDiff.bsdiff(source, destination, delta);
            MapFragment.parseKml(app,context);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


    public static boolean applyPatch(File source,File delta, Application app, Context context){
        if(delta!=null){
            File destination = getDestinationFile(delta);
            try {
                JBPatch.bspatch(source, destination, delta);
                FileUtils.forceDelete(delta);
                MapFragment.parseKml(app,context);
                return true;
            }
            catch (Exception  e){
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    private static String getDeltaName(File source) throws IOException {
        File diffDir = Environment.getExternalStoragePublicDirectory("DMS/Working/SurakshitDiff");
        String name = FilenameUtils.getBaseName(source.getName());
        KmlDocument kml = new KmlDocument();
        kml.parseKMLFile(source);
        int version = Integer.parseInt(kml.mKmlRoot.getExtendedData("total"))-1;
        return name+"_"+version+".diff";
    }

    private static File getDestinationFile(File delta){
        String deltaName = FilenameUtils.getBaseName(delta.getName());
        return Environment.getExternalStoragePublicDirectory("DMS/KML/Dest/LatestKml/"+deltaName+".kml");
    }
}
