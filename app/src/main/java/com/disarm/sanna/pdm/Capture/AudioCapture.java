package com.disarm.sanna.pdm.Capture;

import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.Toast;

import com.disarm.sanna.pdm.R;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Created by Sanna on 22-06-2016.
 */
public class AudioCapture extends AppCompatActivity implements View.OnClickListener {
    static String root = Environment.getExternalStorageDirectory().toString();
    static String path =root + "/" + "DMS" + "/" + "tmp" + "/",type,group,groupID;
    MediaRecorder myAudioRecorder = new MediaRecorder();
    ImageButton record,play;
    Button back;
    private String outputFile = null;
    boolean flag = true;
    Chronometer mChronometer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.capture_audio_activity_list);
        record = (ImageButton) findViewById(R.id.record_button);
        play = (ImageButton) findViewById(R.id.play_button);
        back = (Button) findViewById(R.id.back);
        mChronometer = (Chronometer) findViewById(R.id.chronometer);
        Intent myIntent = getIntent();
        type = myIntent.getStringExtra("IntentType");
        record.setOnClickListener(this);
        play.setVisibility(View.GONE);
        play.setEnabled(false);
        play.setOnClickListener(this);
        back.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.record_button:
                if (flag) {
                    flag = false;
                    record.setBackgroundResource(R.drawable.super_voice_recorder);
                    outputFile = getFilenameAud();
                    myAudioRecorder = new MediaRecorder();
                    myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
                    myAudioRecorder.setOutputFile(outputFile);
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.start();

                    try {
                        myAudioRecorder.prepare();
                        myAudioRecorder.start();
                    } catch (IllegalStateException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    Toast.makeText(getApplicationContext(), R.string.aud_rec_start, Toast.LENGTH_LONG).show();

                }else{
                    mChronometer.stop();
                    myAudioRecorder.stop();
                    myAudioRecorder.release();
                    myAudioRecorder  = null;
                    flag = true;
                    record.setBackgroundResource(R.drawable.easy_voice_recorder);
                    play.setEnabled(true);
                    Toast.makeText(AudioCapture.this, R.string.aud_rec_success, Toast.LENGTH_SHORT).show();
                }

                break;
            case R.id.play_button:
                MediaPlayer m = new MediaPlayer();

                try {
                    m.setDataSource(outputFile);
                }

                catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    m.prepare();
                }

                catch (IOException e) {
                    e.printStackTrace();
                }

                m.start();
                break;

            case R.id.back:
                AudioCapture.this.finish();
                break;
        }

    }

    private String getFilenameAud() {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        group = type;
        groupID = "1";
        return (path + "SVS_" +  group + "_" + timeStamp + "_" + ".3gp");
    }
}
