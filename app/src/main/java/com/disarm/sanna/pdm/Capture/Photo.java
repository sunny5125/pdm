package com.disarm.sanna.pdm.Capture;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import com.disarm.sanna.pdm.BuildConfig;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.disarm.sanna.pdm.ActivityList.type;


/**
 * Created by Sanna on 21-06-2016.
 */
public class Photo extends Activity {
    private File output = null;
    private static final String FILENAME = getOutputMediaFile().toString();
    private static final String AUTHORITY =
            BuildConfig.APPLICATION_ID + ".provider";
    private static final String EXTRA_FILENAME =
            "com.example.sanna.test.EXTRA_FILENAME";
    public static final String TMP_FOLDER="tmp";
    static String group,groupID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState==null) {
            output=new File(getExternalFilesDir(TMP_FOLDER), FILENAME);
            if (output.exists()) {
                output.delete();
            }
            else {
                output.getParentFile().mkdirs();
            }

            Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri outputUri= FileProvider.getUriForFile(this, AUTHORITY, output);
            i.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);

            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
                i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            else if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN) {
                ClipData clip=
                        ClipData.newUri(getContentResolver(), "A photo", outputUri);

                i.setClipData(clip);
                i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            else {
                List<ResolveInfo> resInfoList=
                        getPackageManager()
                                .queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY);

                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    grantUriPermission(packageName, outputUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }
            }

            try {
                startActivity(i);
            }
            catch (ActivityNotFoundException e) {
                Toast.makeText(this, "no camera", Toast.LENGTH_LONG).show();
                finish();
            }
        }
        else {
            output=(File)savedInstanceState.getSerializable(EXTRA_FILENAME);
        }
        finish();
    }
    private static File getOutputMediaFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        group = type;
        groupID = "1";
        return new File("IMG_" + group + "_" + timeStamp + "_" + ".jpg");
    }
}