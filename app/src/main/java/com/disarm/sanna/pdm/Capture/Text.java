package com.disarm.sanna.pdm.Capture;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.disarm.sanna.pdm.R;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.disarm.sanna.pdm.ActivityList.type;
import static com.disarm.sanna.pdm.Capture.Photo.TMP_FOLDER;

/**
 * Created by Sanna on 30-06-2016.
 */
public class Text extends DialogFragment implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    private Spinner variety,quantity;
    private Button save,back;
    //static String type;
    EditText custom;
    StringBuilder msgString;
    static String group,groupID,h,q;
    private boolean c;
    public String[] trans_health_array ;
    public String[] trans_food_array ;
    public String[] trans_victim_array ;
    public String[] trans_shelter_array ;
    public String[] trans_num_array ;
    private File output = null;

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        trans_health_array = getResources().getStringArray(R.array.trans_health_array);
        trans_food_array = getResources().getStringArray(R.array.trans_food_array);
        trans_victim_array = getResources().getStringArray(R.array.trans_victim_array);
        trans_shelter_array = getResources().getStringArray(R.array.trans_shelter_array);
        trans_num_array = getResources().getStringArray(R.array.trans_num_array);
        variety = (Spinner)view.findViewById(R.id.spinner);
        quantity = (Spinner)view.findViewById(R.id.spinner2);
        save = (Button)view.findViewById(R.id.save);
        back = (Button)view.findViewById(R.id.back);
        custom = (EditText) view.findViewById(R.id.custom);
        custom.setVisibility(view.GONE);
        save.setOnClickListener(this);
        back.setOnClickListener(this);

        switch (type){
            case "Health":
                String[] healthArray = getResources().getStringArray(R.array.health_array);
                List<String> health = new ArrayList<String>(Arrays.asList(healthArray));
                ArrayAdapter<String> healthadapter = new ArrayAdapter<String>
                        (getActivity(), android.R.layout.simple_spinner_item, health);
                healthadapter.setDropDownViewResource
                        (android.R.layout.simple_spinner_dropdown_item);
                variety.setAdapter(healthadapter);
                variety.setOnItemSelectedListener(this);
                break;
            case "Food":
                String[] foodArray = getResources().getStringArray(R.array.food_array);
                List<String> food = new ArrayList<String>(Arrays.asList(foodArray));
                ArrayAdapter<String> foodadapter = new ArrayAdapter<String>
                        (getActivity(), android.R.layout.simple_spinner_item, food);
                foodadapter.setDropDownViewResource
                        (android.R.layout.simple_spinner_dropdown_item);
                variety.setAdapter(foodadapter);
                variety.setOnItemSelectedListener(this);
                break;
            case "Shelter":
                String[] shelterArray = getResources().getStringArray(R.array.shelter_array);
                List<String> shelter = new ArrayList<String>(Arrays.asList(shelterArray));
                ArrayAdapter<String> shelterAdapter = new ArrayAdapter<String>
                        (getActivity(), android.R.layout.simple_spinner_item, shelter);
                shelterAdapter.setDropDownViewResource
                        (android.R.layout.simple_spinner_dropdown_item);
                variety.setAdapter(shelterAdapter);
                variety.setOnItemSelectedListener(this);
                break;
            case "Victim":
                String[] victimArray = getResources().getStringArray(R.array.victim_array);
                List<String> victim = new ArrayList<String>(Arrays.asList(victimArray));
                ArrayAdapter<String> victimAdapter = new ArrayAdapter<String>
                        (getActivity(), android.R.layout.simple_spinner_item, victim);
                victimAdapter.setDropDownViewResource
                        (android.R.layout.simple_spinner_dropdown_item);
                variety.setAdapter(victimAdapter);
                variety.setOnItemSelectedListener(this);
                break;
        }
        String[] numArray = getResources().getStringArray(R.array.num_array);
        List<String> num = new ArrayList<String>(Arrays.asList(numArray));
        ArrayAdapter<String> adapter1 = new ArrayAdapter<String>
                (getActivity(), android.R.layout.simple_spinner_item, num);
        adapter1.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);
        quantity.setAdapter(adapter1);
        quantity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View v, int i, long l) {
                if (i != 0 && i != 6){
                    q = trans_num_array[i].toString();
                }

                if (i == 6){
                    custom.setVisibility(v.getVisibility());
                    custom.setEnabled(true) ;
                    c = true;
                }else {
                    custom.setEnabled(false);
                    c = false;

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                q = null;
            }
        });

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_text, container);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.save:
                if (c && custom.getText().toString().trim().length() != 0) {
                    q = custom.getText().toString();
                }else{
                    custom.setError("It can't be blank");
                }
                if (h != null && q != null){
                    msgString = new StringBuilder(type + ": " + h + q);
                    output=new File(getActivity().getExternalFilesDir(TMP_FOLDER),getFilename());
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
                    Toast.makeText(getActivity(), R.string.file_success, Toast.LENGTH_SHORT).show();
                    getDialog().dismiss();
                }else{
                    Toast.makeText(getActivity(), R.string.text_select_appropriately, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.back:
                getDialog().dismiss();
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        switch (type){
            case "Health":
                if (i != 0) {
                    h = trans_health_array[i].toString() + ": ";
                }
                break;
            case "Food":
                if (i != 0) {
                    h = trans_food_array[i].toString() + ": ";
                }
                break;
            case "Shelter":
                if (i != 0) {
                    h = trans_shelter_array[i].toString() + ": ";
                }
                break;
            case "Victim":
                if (i != 0) {
                    h = trans_victim_array[i].toString() + ": ";
                }
                break;

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        h = null;
    }
    private String getFilename() {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        group = type;
        groupID = "1";
        return ("TXT_" +  group + "_" + timeStamp + "_" + groupID + ".txt");
    }
}
