package com.disarm.sanna.pdm.Util;

import android.os.FileObserver;
import android.util.Log;
import android.widget.Toast;

import com.disarm.sanna.pdm.ShareActivity;
import com.disarm.sanna.pdm.SocialShareActivity;

import java.io.File;

/**
 * Created by arka on 18/9/16.
 * Detect file change using FileObserver on Android
 */
public class PathFileObserver extends FileObserver {
    static final String TAG="FILE_OBSERVER";
    /**
     * should be end with File.separator
     */
    String rootPath;
    SocialShareActivity socialShareActivity;
    ShareActivity shareActivity;

    String number;
    static final int mask = (FileObserver.CREATE |
            FileObserver.DELETE |
            FileObserver.DELETE_SELF |
            FileObserver.MODIFY |
            FileObserver.MOVED_FROM |
            FileObserver.MOVED_TO |
            FileObserver.MOVE_SELF);

    public PathFileObserver(SocialShareActivity activity, String path) {
        super(path, mask);
        if (! path.endsWith(File.separator)){
            path += File.separator;
        }
        rootPath = path;

        socialShareActivity = activity;
        number = null;
    }

    public PathFileObserver(ShareActivity activity, String path, String number) {
        super(path, mask);
        if (! path.endsWith(File.separator)){
            path += File.separator;
        }
        if(rootPath == null) {
            rootPath = path;
        }

        shareActivity = activity;
        number = number;
    }

    @Override
    public void onEvent(int event, String path) {
        switch(event){
            case FileObserver.CREATE:
                Log.d(TAG, "CREATE:" + rootPath + path);
                break;
            case FileObserver.DELETE:
                Log.d(TAG, "DELETE:" + rootPath + path);
                break;
            case FileObserver.DELETE_SELF:
                Log.d(TAG, "DELETE_SELF:" + rootPath + path);
                break;
            case FileObserver.MODIFY:
                Log.d(TAG, "MODIFY:" + rootPath + path);
                break;
            case FileObserver.MOVED_FROM:
                Log.d(TAG, "MOVED_FROM:" + rootPath + path);
                break;
            case FileObserver.MOVED_TO:
                Log.d(TAG, "MOVED_TO:" + path);


                File file = new File(rootPath + path);
                socialShareActivity.refreshList(rootPath + path, file);
                /*
                if(number != null && path.contains(number)) {
                    shareActivity.refreshList(rootPath + path, file);
                }
                */
                break;
            case FileObserver.MOVE_SELF:
                Log.d(TAG, "MOVE_SELF:" + path);
                break;
            default:
                // just ignore
                break;
        }
    }

    public void close() {
        super.finalize();
    }
}
