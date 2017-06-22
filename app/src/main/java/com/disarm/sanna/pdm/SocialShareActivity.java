package com.disarm.sanna.pdm;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.disarm.sanna.pdm.Adapters.SocialShareChatlistAdapter;
import com.disarm.sanna.pdm.Util.DividerItemDecoration;
import com.disarm.sanna.pdm.Util.PathFileObserver;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by arka on 14/9/16.
 * Offline Social Share Activity
 */
public class SocialShareActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String WORKING_DIRECTORY = "/DMS/Working/";

    RecyclerView chatList;
    ArrayList<File> allFiles;
    ArrayList<String> senderList;
    ArrayList<String> senderListNames;
    HashMap<String, Integer> numberToSenderMap;
    ArrayList<Senders> senders;
    Senders myself;

    PathFileObserver pathFileObserver;


    SocialShareChatlistAdapter chatlistAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_social_share);

        chatList = (RecyclerView) findViewById(R.id.list_social_share_chats);

        allFiles = new ArrayList<>();

        senderList = new ArrayList<>();
        senderListNames = new ArrayList<>();
        numberToSenderMap = new HashMap<>();
        senders = new ArrayList<>();

        setTitle("Recent");

        myself = new Senders(identifySelf(), "Me");
        populateChatList();

        pathFileObserver = new PathFileObserver(this,
                Environment.getExternalStorageDirectory().toString() + WORKING_DIRECTORY);
        pathFileObserver.startWatching();

        FloatingActionButton addChat = (FloatingActionButton) findViewById(R.id.b_social_share_add);
        addChat.setOnClickListener(this);
    }

    /**
     * Identify device's phone number
     */
    private String identifySelf() {
        return SelectCategoryActivity.SOURCE_PHONE_NO;
    }

    /**
     * Find all files in working dirctory
     */
    private void findFiles() {

        File workingDirectory = new File(Environment.getExternalStorageDirectory().toString() +
                WORKING_DIRECTORY);

        File[] files = workingDirectory.listFiles();
        if(files==null) {
            Toast.makeText(this, "Working Directory Not Found", Toast.LENGTH_LONG).show();
            Log.d("ERROR", "Working Directory Not Found");
            return;
        }

        for(File file:files) {
            if( !file.isDirectory() && !file.isHidden() ) {
                allFiles.add(file);
            }
        }
    }

    /**
     * Populate chat list with the phone numbers of senders
     */
    private void populateChatList() {

        findFiles();
        for(File file:allFiles) {
            String name = file.getName();

            findNodeOfFile(name, file, false);
        }

        addSentFilesToSenderNodes();

        chatlistAdapter = new SocialShareChatlistAdapter(senderList, senderListNames);
        chatList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        chatList.setItemAnimator(new DefaultItemAnimator());
        chatList.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        chatList.setAdapter(chatlistAdapter);
        chatList.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(),
                chatList, new SurakshitActivity.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                launchShareActivity(position);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
        chatlistAdapter.notifyDataSetChanged();
    }

    /**
     * Handle event triggered by File Observer
     * @param fileName
     * @param file
     */
    public void refreshList(final String fileName, final File file) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findNodeOfFile(fileName, file, true);
                addSentFilesToSenderNodes();
            }
        });
    }

    /**
     * Assign file to its corresponding node
     * @param fileName
     * @param file
     * @param updateList
     */
    public void findNodeOfFile(String fileName, File file, boolean updateList) {

        if(fileName.contains("MapDisarm")) { // ignore GPS trails for now
            return;
        }

        String number = fileName.split("_")[3];

        if(number.equals(myself.number)) {
            addFileToNode(myself, file, fileName);
            return;
        }

        if(senderList.contains(number) == false) {
            senderList.add(number);
            String nameFromContact = findContactNameByNumber(fileName.split("_")[3]);
            Senders sender = new Senders(number, nameFromContact);

            addFileToNode(sender, file, fileName);
            senderListNames.add(nameFromContact);
            senders.add(sender);
            numberToSenderMap.put(number, senders.size()-1);

        } else if(number.indexOf(".") == -1){ // hack to avoid unwanted files
            Senders sender = senders.get(numberToSenderMap.get(number));
            addFileToNode(sender, file, fileName);
        }

        if(updateList) {
            chatlistAdapter.notifyDataSetChanged();
            Log.d("MOVED", fileName);
        }
    }


    /**
     * Add files to their corresponding node
     * @param node
     * @param file : the file belonging to the node
     */
    private void addFileToNode(Senders node, File file, String fileName) {

        if(node.allFiles.contains(file) || fileName.contains("MapDisarm")) {
            return;
        }

        node.addFile(file);
        if(fileName.startsWith("IMG")) {
            node.addImage(file);
        } else if(fileName.startsWith("VID")) {
            node.addVideo(file);
        } else  if(fileName.startsWith("TXT")) {
            node.addText(file);
        } else if(fileName.startsWith("SVS")) {
            node.addRecording(file);
        } else if(fileName.startsWith("SMS")) {
            node.addSms(file);
        }
    }

    /**
     * Add sent files to corresponding senders
     */
    private void addSentFilesToSenderNodes() {
        for(File file:myself.getAllFiles()) {

            String fileName = file.getName();
            if(fileName.contains("MapDisarm")) {
                continue;
            }

            String sentNodeNumber = fileName.split("_")[4];

            if(numberToSenderMap.get(sentNodeNumber) != null) {
                Senders sender = senders.get(numberToSenderMap.get(sentNodeNumber));
                if (sender != null) {
                    addFileToNode(sender, file, fileName);
                }
            } else {
                senderList.add(sentNodeNumber);
                String nameFromContact = findContactNameByNumber(fileName.split("_")[3]);
                Senders sender = new Senders(sentNodeNumber, nameFromContact);

                addFileToNode(sender, file, fileName);
                senderListNames.add(nameFromContact);
                senders.add(sender);
                numberToSenderMap.put(sentNodeNumber, senders.size()-1);
            }
        }
    }

    /**
     * Find the name of contact from phone book
     * @param number : The contact number
     * @return : Corresponding Contact name
     */
    private String findContactNameByNumber(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
        String name = "No Name";

        ContentResolver contentResolver = getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup
                        .getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return name;
    }

    /**
     * Launch Chat and Share Activity based on the position
     * @param position
     */
    private void launchShareActivity(int position) {
        Intent shareActivityIntent = new Intent(this, ShareActivity.class);
        Senders sender = senders.get(position);
        shareActivityIntent.putExtra("SENDER_DATA", sender);
        startActivity(shareActivityIntent);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.b_social_share_add:
                final AlertDialog.Builder addNewChat = new AlertDialog.Builder(this);
                addNewChat.setTitle("Add New");

                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                addNewChat.setView(input);

                addNewChat.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String inputNumber = input.getText().toString();
                        if(inputNumber.length() == 10) {
                            addNewSender(inputNumber);
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    "Not a valid Number", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                addNewChat.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });

                addNewChat.show();
                break;
        }
    }

    private void addNewSender(String inputNumber) {
        if(senderList.contains(inputNumber)) {
            return;
        }

        String nameFromContact = findContactNameByNumber(inputNumber);
        Senders sender = new Senders(inputNumber, nameFromContact);

        senderList.add(inputNumber);
        senderListNames.add(nameFromContact);
        senders.add(sender);
        numberToSenderMap.put(inputNumber, senders.size()-1);
        chatlistAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pathFileObserver.stopWatching();
    }
/*
    @Override
    public void onBackPressed() {
        if(doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.press_back_to_exit, Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }*/
}
