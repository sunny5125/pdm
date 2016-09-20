package com.disarm.sanna.pdm.Util;

import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by disarm on 14/8/16.
 */
public class Reset {

    public static void resetWorking(File dir){
        File[] foundFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {

                return name.startsWith("MapDisarm_Log");
            }
        });

    }

    public static boolean deleteContents(File dir) {
        File[] files = dir.listFiles();
        boolean success = true;
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    success &= deleteContents(file);
                }
                if (!file.delete()) {
                    Log.w("delete", "Failed to delete " + file);
                    success = false;
                }
            }
        }
        return success;
    }
}
