package com.disarm.sanna.pdm;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ListView;
import android.widget.Toast;

import com.disarm.sanna.pdm.Adapters.ShareChatsAdapter;
import com.disarm.sanna.pdm.BackgroundProcess.FileTask;
import com.disarm.sanna.pdm.Capture.AudioCapture;
import com.disarm.sanna.pdm.Capture.Photo;
import com.disarm.sanna.pdm.Capture.SmsCaptrue;
import com.disarm.sanna.pdm.Capture.Text;
import com.disarm.sanna.pdm.Capture.Video;

import java.io.File;
import java.util.ArrayList;


/**
 * Created by arka on 16/9/16.
 */
public class ShareActivity extends AppCompatActivity implements
        FloatingActionButton.OnClickListener, ShareChatsAdapter.ShareChatsInterface{
    ListView listview;
    String number;
    ArrayList<File> allFiles,imagefiles,videofiles,recordingfiles,textfiles,smsfiles;
    static Context applicationContext;
    ArrayList<String> category = new ArrayList<>();

    //PathFileObserver pathFileObserver;
    private String category_active;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_activity);
        applicationContext = getApplicationContext();
        final FloatingActionButton showPhoto = (FloatingActionButton)findViewById(R.id.sh_photo);
        showPhoto.setOnClickListener(this);
        final FloatingActionButton showVideo = (FloatingActionButton)findViewById(R.id.sh_video);
        showVideo.setOnClickListener(this);
        final FloatingActionButton showAudio = (FloatingActionButton)findViewById(R.id.sh_audio);
        showAudio.setOnClickListener(this);
        final FloatingActionButton showText = (FloatingActionButton)findViewById(R.id.sh_text);
        showText.setOnClickListener(this);
        final FloatingActionButton showSms = (FloatingActionButton)findViewById(R.id.sh_sms);
        showSms.setOnClickListener(this);
        final FloatingActionButton sharePhoto = (FloatingActionButton)findViewById(R.id.b_share_photo);
        sharePhoto.setOnClickListener(this);
        final FloatingActionButton shareVideo = (FloatingActionButton)findViewById(R.id.b_share_video);
        shareVideo.setOnClickListener(this);
        final FloatingActionButton shareAudio = (FloatingActionButton)findViewById(R.id.b_share_audio);
        shareAudio.setOnClickListener(this);
        final FloatingActionButton shareText = (FloatingActionButton)findViewById(R.id.b_share_text);
        shareText.setOnClickListener(this);
        final FloatingActionButton shareSms = (FloatingActionButton)findViewById(R.id.b_share_sms);
        shareSms.setOnClickListener(this);
        final FloatingActionButton send = (FloatingActionButton)findViewById(R.id.b_share_send);
        send.setOnClickListener(this);
        listview = (ListView)findViewById(R.id.share_received_files);
        Bundle extra = getIntent().getExtras();
        if(extra != null) {
            Senders sender = extra.getParcelable("SENDER_DATA");
            if(sender != null && sender.getNumber() != null) {
                number = sender.getNumber();
                setTitle(number);
            }
            if(sender != null && sender.getAllFiles() != null) {
                allFiles = sender.getAllFiles();
                imagefiles = sender.getImageFiles();
                videofiles = sender.getVideoFiles();
                recordingfiles = sender.getRecordingFiles();
                textfiles = sender.getTextFiles();
                smsfiles = sender.getSmsFiles();
            }
        }


        for(File file: allFiles) {
            category.add(file.getName());
        }

        ShareChatsAdapter adapter = new ShareChatsAdapter(category, number, this);
        adapter.setAnInterface(this);
        listview.setAdapter(adapter);
        category_active = "ALL";

        /*
        pathFileObserver = new PathFileObserver(this,
                Environment.getExternalStorageDirectory().toString()
                        + SocialShareActivity.WORKING_DIRECTORY, number);
        pathFileObserver.startWatching();
        */
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //pathFileObserver.stopWatching();
    }

    public static Context getContextOfApplication(){
        return applicationContext;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.b_share_photo:
                Intent intent=new Intent(applicationContext, Photo.class);
                intent.putExtra("IntentType", "SocialShare");
                startActivity(intent);
                break;
            case R.id.b_share_video:
                Intent intent1=new Intent(applicationContext, Video.class);
                intent1.putExtra("IntentType", "SocialShare");
                startActivity(intent1);
                break;
            case R.id.b_share_text:
                FragmentManager fm = getSupportFragmentManager();
                Text text= new Text();
                text.show(fm, "activity_text");
                break;
            case R.id.b_share_audio:
                Intent intent2=new Intent(applicationContext, AudioCapture.class);
                intent2.putExtra("IntentType", "SocialShare");
                startActivity(intent2);
                break;
            case R.id.b_share_send:
                new FileTask().execute("50", number, null);
                Toast.makeText(this, "Sending : Refresh to see changes", Toast.LENGTH_SHORT).show();
                break;
            case R.id.b_share_sms:
                FragmentManager fm1 = getSupportFragmentManager();
                SmsCaptrue smsCaptrue = new SmsCaptrue();
                smsCaptrue.show(fm1,"activity_sms");
                break;
            case R.id.sh_photo:
                category.clear();
                for(File file: imagefiles) {
                    category.add(file.getName());
                }
                ShareChatsAdapter adapter1 = new ShareChatsAdapter(category, number, this);
                adapter1.setAnInterface(this);
                listview.setAdapter(adapter1);
                category_active = "IMAGE";
                break;
            case R.id.sh_video:
                category.clear();
                for(File file: videofiles) {
                    category.add(file.getName());
                }
                ShareChatsAdapter adapter2 = new ShareChatsAdapter(category, number, this);
                adapter2.setAnInterface(this);
                listview.setAdapter(adapter2);
                category_active = "VIDEO";
                break;
            case R.id.sh_audio:
                category.clear();
                for(File file: recordingfiles) {
                    category.add(file.getName());
                }
                ShareChatsAdapter adapter5 = new ShareChatsAdapter(category, number, this);
                adapter5.setAnInterface(this);
                listview.setAdapter(adapter5);
                category_active = "AUDIO";
                break;
            case R.id.sh_text:
                category.clear();
                for(File file: textfiles) {
                    category.add(file.getName());
                }
                ShareChatsAdapter adapter3 = new ShareChatsAdapter(category, number, this);
                adapter3.setAnInterface(this);
                listview.setAdapter(adapter3);
                category_active = "TEXT";
                break;
            case R.id.sh_sms:
                category.clear();
                for(File file: smsfiles) {
                    category.add(file.getName());
                }
                ShareChatsAdapter adapter4 = new ShareChatsAdapter(category, number, this);
                adapter4.setAnInterface(this);
                listview.setAdapter(adapter4);
                category_active = "SMS";
                break;
        }
    }

    /**
     * Open File on click
     * @param row
     * @param position
     */
    @Override
    public void onClick(View row, int position) {
        //File file = allFiles.get(position);
        File file = null;
        for(int i = 0; i < allFiles.size(); i++) {
            if(allFiles.get(i).getName().equals(category.get(position))) {
                file = allFiles.get(i);
                break;
            }
        }

        Intent openFile = new Intent();
        openFile.setAction(Intent.ACTION_VIEW);

        MimeTypeMap fileMime = MimeTypeMap.getSingleton();
        String filePath = "" + file;
        String mimeType;
        mimeType = fileMime.getMimeTypeFromExtension(
                filePath.substring(filePath.lastIndexOf('.') + 1));
        openFile.setDataAndType(Uri.parse("file://" + file.getAbsolutePath()), mimeType);
        openFile.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(Intent.createChooser(openFile, null));
    }

    public void refreshList(final String path, final File file) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                addFileToList(path, file);
                refreshView();
            }
        });
    }

    /**
     * Add a new file observed by File Observer to list
     * @param path
     * @param file
     */
    private void addFileToList(String path, File file) {
        allFiles.add(file);
        if(path.contains("IMG")) {
            imagefiles.add(file);
        } else if(path.contains("VID")) {
            videofiles.add(file);
        } else if(path.contains("SVS")) {
            recordingfiles.add(file);
        } else if(path.contains("TXT")) {
            textfiles.add(file);
        } else if(path.contains("SMS")) {
            smsfiles.contains(file);
        }
    }

    /**
     * Refresh the list view
     */
    private void refreshView() {
        category.clear();

        if(category_active.equals("ALL")) {
            for(File file: allFiles) {
                category.add(file.getName());
            }
        } else if(category_active.equals("IMAGE")) {
            for(File file: imagefiles) {
                category.add(file.getName());
            }
        } else if(category_active.equals("VIDEO")) {
            for(File file: videofiles) {
                category.add(file.getName());
            }
        } else if(category_active.equals("AUDIO")) {
            for(File file: recordingfiles) {
                category.add(file.getName());
            }
        } else if(category_active.equals("TEXT")) {
            for(File file: textfiles) {
                category.add(file.getName());
            }
        } else if(category_active.equals("SMS")) {
            for(File file: smsfiles) {
                category.add(file.getName());
            }
        }

        ShareChatsAdapter adapter2 = new ShareChatsAdapter(category, number, this);
        adapter2.setAnInterface(this);
        listview.setAdapter(adapter2);
    }
}