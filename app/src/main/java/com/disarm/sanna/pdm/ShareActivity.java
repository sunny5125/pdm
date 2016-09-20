package com.disarm.sanna.pdm;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;


import com.disarm.sanna.pdm.BackgroundProcess.FileTask;
import com.disarm.sanna.pdm.Capture.AudioCapture;
import com.disarm.sanna.pdm.Capture.Photo;
import com.disarm.sanna.pdm.Capture.Text;
import com.disarm.sanna.pdm.Capture.Video;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by arka on 16/9/16.
 */
public class ShareActivity extends AppCompatActivity implements AdapterView.OnItemClickListener,
        View.OnClickListener {
    ListView receivedFiles;
    String number;
    ArrayList<File> allFiles;
    static Context applicationContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_activity);
        applicationContext = getApplicationContext();

        receivedFiles = (ListView)findViewById(R.id.share_received_files);
        Bundle extra = getIntent().getExtras();
        if(extra != null) {
            Senders sender = extra.getParcelable("SENDER_DATA");
            if(sender != null && sender.getNumber() != null) {
                number = sender.getNumber();
                setTitle(number);
            }
            if(sender != null && sender.getAllFiles() != null) {
                allFiles = sender.getAllFiles();
            }
        }

        ArrayList<String> allFilesName = new ArrayList<>();
        for(File file: allFiles) {
            allFilesName.add(file.getName());
        }

        ArrayAdapter<String> chatListAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, allFilesName);
        receivedFiles.setAdapter(chatListAdapter);
        receivedFiles.setOnItemClickListener(this);

        Button sharePhoto, shareVideo, shareText, shareRecording, shareSend;
        sharePhoto = (Button)findViewById(R.id.b_share_photo);
        shareVideo = (Button)findViewById(R.id.b_share_video);
        shareText = (Button)findViewById(R.id.b_share_text);
        shareRecording = (Button)findViewById(R.id.b_share_recording);
        shareSend = (Button)findViewById(R.id.b_share_send);

        sharePhoto.setOnClickListener(this);
        shareVideo.setOnClickListener(this);
        shareRecording.setOnClickListener(this);
        shareText.setOnClickListener(this);
        shareSend.setOnClickListener(this);
    }

    /**
     * Open File on click
     * @param adapterView
     * @param view
     * @param position
     * @param l
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

        File file = allFiles.get(position);
        Intent openFile = new Intent();
        openFile.setAction(Intent.ACTION_VIEW);
        //openFile.setAction(Intent.ACTION_GET_CONTENT);
        //openFile.setData(Uri.parse("" + file));
        openFile.setData(Uri.fromFile(file));

        /*
        if(file.getName().startsWith("IMG")) {
            openFile.setType("image/*");
        } else if(file.getName().startsWith("VID")) {
            openFile.setType("video/*");
        } else  if(file.getName().startsWith("TXT")) {
            openFile.setType("text/*");
        } else if(file.getName().startsWith("SVS")) {
            openFile.setType("audio/*");
        } else if(file.getName().startsWith("SMS")) {
            openFile.setType("vnd.android-dir/mms-sm");
        }
        */

        MimeTypeMap fileMime = MimeTypeMap.getSingleton();
        String filePath = "" + file;
        String mimeType = null;
        mimeType = fileMime.getMimeTypeFromExtension(
                filePath.substring(filePath.lastIndexOf('.') + 1));
        openFile.setType(mimeType);
        openFile.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Toast.makeText(this, "FILE : " + filePath.substring(filePath.lastIndexOf('.') + 1), Toast.LENGTH_LONG).show();
        Toast.makeText(this, "FILE : " + mimeType, Toast.LENGTH_LONG).show();

        startActivity(Intent.createChooser(openFile, null));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.b_share_photo:
                Intent intent=new Intent(getApplicationContext(), Photo.class);
                intent.putExtra("IntentType", "SocialShare");
                startActivity(intent);
                break;
            case R.id.b_share_video:
                Intent intent1=new Intent(getApplicationContext(), Video.class);
                intent1.putExtra("IntentType", "SocialShare");
                startActivity(intent1);
                break;
            case R.id.b_share_text:
                FragmentManager fm = getSupportFragmentManager();
                Text text= Text.newInstance("Add Text", "SocialShare");
                text.show(fm, "activity_text");
                break;
            case R.id.b_share_recording:
                Intent intent2=new Intent(getApplicationContext(), AudioCapture.class);
                intent2.putExtra("IntentType", "SocialShare");
                startActivity(intent2);
                break;
            case R.id.b_share_send:
                new FileTask().execute("50", "defaultMcs", null); // Set dest as MCS for now
                break;
        }
    }

    public static Context getContextOfApplication(){
        return applicationContext;
    }
}
