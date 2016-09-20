package com.disarm.sanna.pdm.Capture;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.disarm.sanna.pdm.R;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by disarm on 9/7/16.
 */
public class SmsCaptrue extends DialogFragment implements View.OnClickListener{

    private static String type;
    private EditText edittextMsg;
    private Button save,back;
    private String outputFile;
    private String msgInput;
    static String root = Environment.getExternalStorageDirectory().toString();
    static String path =root + "/" + "DMS" + "/" + "tmp" + "/",group,groupID;

    public static SmsCaptrue newInstance(String s){
        type = s;
        return null;
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_sms, container,false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        save = (Button)view.findViewById(R.id.save);
        back = (Button)view.findViewById(R.id.back);
        edittextMsg = (EditText)view.findViewById(R.id.customMsg);
        save.setOnClickListener(this);
        back.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.save:
                msgInput = edittextMsg.getText().toString();
                if (!msgInput.isEmpty()) {
                    outputFile = getFilename();
                    File file = new File(outputFile);
                    // If file does not exists, then create it
                    if (!file.exists()) {
                        try {
                            file.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        FileWriter fw = new FileWriter(file.getAbsoluteFile());
                        BufferedWriter bw = new BufferedWriter(fw);
                        Log.v("File", "Writing " + msgInput);
                        bw.write(msgInput);
                        bw.flush();
                        bw.close();
                        Log.v("File", "written");

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(getActivity(), R.string.file_success, Toast.LENGTH_SHORT).show();
                    getDialog().dismiss();
                }else {
                    edittextMsg.setError(String.valueOf(R.string.sms_edittext_error));
                }

                break;
            case R.id.back:
                getDialog().dismiss();
                break;
        }
    }

    private String getFilename() {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        group = type;
        groupID = "1";
        return (path + "SMS_" +  group + "_" + timeStamp + "_" + ".txt");
    }
}
