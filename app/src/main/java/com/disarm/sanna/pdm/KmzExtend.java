package com.disarm.sanna.pdm;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import org.osmdroid.bonuspack.kml.KmlDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.concurrent.Exchanger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by naman on 15/9/17.
 */

public class KmzExtend extends KmlDocument {
    @Override
    public boolean parseKMZFile(File file) {
        this.mLocalFile = file;
        Log.d("BONUSPACK", "KmlProvider.parseKMZFile:" + this.mLocalFile.getAbsolutePath());

        try {
            ZipFile e = new ZipFile(this.mLocalFile);
            String rootFileName = null;
            Enumeration list = e.entries();

            while(list.hasMoreElements()) {
                ZipEntry result = (ZipEntry)list.nextElement();
                String rootEntry = result.getName();
                if(rootEntry.endsWith(".kml") && !rootEntry.contains("/")) {
                    rootFileName = rootEntry;
                }
                if(rootEntry.endsWith(".JPG")){
                    final InputStream is = e.getInputStream(result);
                    File f = new File(Environment.getExternalStoragePublicDirectory(rootEntry.toString()).toString());
                    final OutputStream os = new FileOutputStream(f);
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            int read;
                            try {
                                while ((read = is.read()) != -1) {
                                    os.write(read);
                                }
                                os.flush();
                                os.close();
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    });
                    t.start();
                    break;
                }
            }

            boolean result1;
            if(rootFileName != null) {
                ZipEntry rootEntry1 = e.getEntry(rootFileName);
                InputStream stream = e.getInputStream(rootEntry1);
                Log.d("BONUSPACK", "KML root:" + rootFileName);
                result1 = this.parseKMLStream(stream, e);
            } else {
                Log.d("BONUSPACK", "No .kml entry found.");
                result1 = false;
            }

            e.close();
            return result1;
        } catch (Exception var8) {
            var8.printStackTrace();
            return false;
        }

    }
}