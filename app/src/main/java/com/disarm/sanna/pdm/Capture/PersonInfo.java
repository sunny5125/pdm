package com.disarm.sanna.pdm.Capture;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.disarm.sanna.pdm.ActivityList;
import com.disarm.sanna.pdm.BackgroundProcess.FileTask;
import com.disarm.sanna.pdm.BuildConfig;
import com.disarm.sanna.pdm.R;
import com.disarm.sanna.pdm.location.MLocation;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


/**
 * Created by sanna on 6/15/17.
 */

public class PersonInfo extends AppCompatActivity implements Button.OnClickListener{
    private static String group = "PI";
    Button submit,cancel,takePicture,fromGallery;
    ImageView displayPicture;
    EditText name,phone,des;
    private int CHOICE_AVATAR_FROM_GALLERY = 2;
    private int CHOICE_AVATAR_FROM_CAMERA = 1;
    private File output = null;
    private static final String FILENAME = getOutputMediaFile().toString();
    private static final String AUTHORITY =
            BuildConfig.APPLICATION_ID + ".provider";
    private static final String EXTRA_FILENAME =
            "com.example.sanna.test.EXTRA_FILENAME";
    public static final String TMP_FOLDER="tmp";
    private String type = "PI";
    static String root = Environment.getExternalStorageDirectory().toString();
    static String path =root + "/" + "DMS" + "/" + "tmp" + "/";
    static String working_path =root + "/" + "DMS" + "/" + "Working" + "/";
    private String ttlString, dest, latlong;
    String displaypictureUri;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_info);
        setTitle("Person Information");
        submit = (Button) findViewById(R.id.person_submit);
        cancel = (Button) findViewById(R.id.person_cancel);
        takePicture = (Button) findViewById(R.id.person_take_picture);
        fromGallery = (Button) findViewById(R.id.person_gallery);
        displayPicture = (ImageView) findViewById(R.id.person_image);
        name = (EditText) findViewById(R.id.person_name);
        phone = (EditText) findViewById(R.id.person_phone);
        des = (EditText) findViewById(R.id.person_des);
        fromGallery.setOnClickListener(this);
        takePicture.setOnClickListener(this);
        cancel.setOnClickListener(this);
        submit.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.person_gallery:
                choiceAvatarFromGallery();
                break;
            case R.id.person_take_picture:
                choiceAvatarFromCamera();
                break;
            case R.id.person_submit:
            //work need to be started from here.
                if (MLocation.isGPS) {

                    String personName = name.getText().toString();
                    String personDes = des.getText().toString();
                    String personPhone = phone.getText().toString();

                    if (displayPicture.getDrawable() == null || TextUtils.isEmpty(personName)
                            || TextUtils.isEmpty(personDes) || TextUtils.isEmpty(personPhone)) {
                        Toast.makeText(this, "Please provide all information...!", Toast.LENGTH_SHORT).show();

                    } else {
     /*                   File source = new File(displaypictureUri);
                        File destination = new File(working_path);
                        try
                        {
                            copyFileUsingStream(source,destination);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
    */
                        //save text file
                        if (personName != null && personDes != null && personPhone != null) {
                            StringBuilder msgString = new StringBuilder(type + ":" + personName + ":" + personDes + ":" + personPhone);
                            output=new File(getExternalFilesDir(TMP_FOLDER), getFilename());
                            Log.v("check", output.toString());
                            // If file does not exists, then create it
                            if (!output.exists()) {
                                try {
                                    output.createNewFile();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            try {
                                FileWriter fw = new FileWriter(output.getAbsoluteFile());
                                BufferedWriter bw = new BufferedWriter(fw);
                                Log.v("File", "Writing " + msgString.toString());
                                bw.write(msgString.toString());
                                bw.flush();
                                bw.close();
                                Log.v("FIle", "written");

                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            Log.v("final ", msgString.toString());

                                ttlString = "50";
                                dest = "defaultMcs";

                            new FileTask().execute(ttlString, dest, latlong);
                            this.finish();
                            //Toast.makeText(this, R.string.file_success, Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast.makeText(this, "GPS is not ON or GPS is not LOCKED", Toast.LENGTH_LONG).show();

                }
                break;
            case R.id.person_cancel:
                finish();
                break;
        }
    }

    private String getFilename() {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        return ("TXT_" +  group + "_" + timeStamp + ".txt");
    }

    private static void copyFileUsingStream(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }

    public void choiceAvatarFromCamera() {
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
                startActivityForResult(i,CHOICE_AVATAR_FROM_CAMERA);
            }
            catch (ActivityNotFoundException e) {
                Toast.makeText(this, "no camera", Toast.LENGTH_LONG).show();
                finish();
            }
        }

    public void choiceAvatarFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(getCropIntent(intent), CHOICE_AVATAR_FROM_GALLERY);
    }

    private Intent getCropIntent(Intent intent) {
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 320);
        intent.putExtra("outputY", 320);
        intent.putExtra("return-data", true);
        return intent;
    }

    private static File getOutputMediaFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        return new File("IMG_" + group + "_" + timeStamp + "_" + ".jpg");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == CHOICE_AVATAR_FROM_GALLERY) {
                //ToastUtils.toastType0(mActivity, "CHOICE_AVATAR_FROM_CAMERA", Toast.LENGTH_SHORT);
                Uri uri = data.getData();
                displaypictureUri = data.getData().toString();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    //Bitmap avatar = getBitmapFromData(data);
                    displayPicture.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // this bitmap is the finish image
            } else if (requestCode == CHOICE_AVATAR_FROM_CAMERA) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                Log.v("check",String.valueOf(imageBitmap));
                displayPicture.setImageBitmap(imageBitmap);
            }
        }
    }

    /**
     * Use for decoding camera response data.
     *
     * @param data
     * @return
     */
    public static Bitmap getBitmapFromData(Intent data) {
        Bitmap photo = null;
        Uri photoUri = data.getData();
        if (photoUri != null) {
            photo = BitmapFactory.decodeFile(photoUri.getPath());
        }
        if (photo == null) {
            Bundle extra = data.getExtras();
            if (extra != null) {
                photo = (Bitmap) extra.get("data");
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                photo.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            }
        }

        return photo;
    }
}
