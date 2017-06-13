package com.disarm.sanna.pdm.Capture;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.disarm.sanna.pdm.BuildConfig;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.disarm.sanna.pdm.ActivityList.type;
import static com.disarm.sanna.pdm.Capture.Photo.TMP_FOLDER;

/**
 * Created by Sanna on 22-06-2016.
 */
public class Video extends AppCompatActivity {
    File output = null;
    private static final String FILENAME = getOutputMediaFile().toString();
    private static final String AUTHORITY =
            BuildConfig.APPLICATION_ID + ".provider";
    private static final String EXTRA_FILENAME =
            "com.example.sanna.test.EXTRA_FILENAME";
    static String group,groupID;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState==null) {
            output=new File(getExternalFilesDir(TMP_FOLDER), FILENAME);
            if (output.exists()) {
                output.delete();
            }
            else {
                output.getParentFile().mkdirs();
            }

            Intent i=new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            Uri outputUri= FileProvider.getUriForFile(this, AUTHORITY, output);
            i.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
            i.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);

            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
                i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            else if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN) {
                ClipData clip=
                        ClipData.newUri(getContentResolver(), "A video", outputUri);

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
            finish();
        }
        else {
            output=(File)savedInstanceState.getSerializable(EXTRA_FILENAME);
        }
    }

    private static File getOutputMediaFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        group = type;
        groupID = "1";
        return new File("VID_" + group + "_" + timeStamp + "_" + ".3gp");
    }

}
