package com.disarm.sanna.pdm;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.disarm.sanna.pdm.Adapters.SocialShareChatlistAdapter;
import com.disarm.sanna.pdm.DisarmConnect.MyService;
import com.disarm.sanna.pdm.Service.SyncService;
import com.disarm.sanna.pdm.Util.DividerItemDecoration;
import com.disarm.sanna.pdm.Util.PathFileObserver;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by arka on 14/9/16.
 * Offline Social Share Activity
 */
public class SocialShareActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String WORKING_DIRECTORY = "/DMS/Working/";

    RecyclerView chatList;
    ArrayList<File> allFiles;
    ArrayList<String> senderList;
    ArrayList<String> senderListNames;
    HashMap<String, Integer> numberToSenderMap;
    ArrayList<Senders> senders;

    PathFileObserver pathFileObserver;

    SyncService syncService;
    private boolean syncServiceBound = false;
    MyService myService;
    private boolean myServiceBound = false;

    //Psync
    private ServiceConnection syncServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            SyncService.SyncServiceBinder binder = (SyncService.SyncServiceBinder) service;
            syncService = binder.getService();
            syncServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            syncServiceBound = false;
        }
    };

    //DisarmConnect
    private ServiceConnection myServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            MyService.MyServiceBinder binder = (MyService.MyServiceBinder) service;
            myService= binder.getService();
            myServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            myServiceBound = false;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_social_share);

        chatList = (RecyclerView)findViewById(R.id.list_social_share_chats);

        allFiles = new ArrayList<>();

        senderList = new ArrayList<>();
        senderListNames = new ArrayList<>();
        numberToSenderMap = new HashMap<>();
        senders = new ArrayList<>();

        setTitle("Recent");

        populateChatList();

        pathFileObserver = new PathFileObserver(
                Environment.getExternalStorageDirectory().toString() + WORKING_DIRECTORY);
        pathFileObserver.startWatching();
        
        startServices();
        Button exit = (Button)findViewById(R.id.b_social_share_exit);
        exit.setOnClickListener(this);
    }

    private void startServices() {
        final Intent syncServiceIntent = new Intent(getBaseContext(), SyncService.class);
        bindService(syncServiceIntent, syncServiceConnection, Context.BIND_AUTO_CREATE);
        startService(syncServiceIntent);
        Toast.makeText(getApplicationContext(), R.string.start_sync, Toast.LENGTH_SHORT).show();

        final Intent myServiceIntent = new Intent(getBaseContext(), MyService.class);
        bindService(myServiceIntent, myServiceConnection, Context.BIND_AUTO_CREATE);
        startService(myServiceIntent);
    }

    private void stopServices() {
        final Intent syncServiceIntent = new Intent(getBaseContext(), SyncService.class);
        if (syncServiceBound) {
            unbindService(syncServiceConnection);
        }
        syncServiceBound = false;
        stopService(syncServiceIntent);

        final Intent myServiceIntent = new Intent(getBaseContext(), MyService.class);
        if (myServiceBound) {
            unbindService(myServiceConnection);
        }
        myServiceBound = false;
        stopService(myServiceIntent);
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

            if(name.startsWith("MapDisarm")) { // ignore GPS trails for now
                continue;
            }

            String number = name.split("_")[3];
            if(senderList.contains(number) == false) {
                senderList.add(number);
                String nameFromContact = findContactNameByNumber(name.split("_")[3]);
                Toast.makeText(this, nameFromContact, Toast.LENGTH_SHORT).show();
                Senders sender = new Senders(number, nameFromContact);
                senderListNames.add(nameFromContact);
                senders.add(sender);
                numberToSenderMap.put(number, senders.size()-1);
            } else if(number.indexOf(".") == -1){ // hack to avoid unwanted files
                Senders sender = senders.get(numberToSenderMap.get(number));

                sender.addFile(file);
                if(name.startsWith("IMG")) {
                    sender.addImage(file);
                } else if(name.startsWith("VID")) {
                    sender.addVideo(file);
                } else  if(name.startsWith("TXT")) {
                    sender.addText(file);
                } else if(name.startsWith("SVS")) {
                    sender.addRecording(file);
                } else if(name.startsWith("SMS")) {
                    sender.addSms(file);
                }
            }
        }

        SocialShareChatlistAdapter chatlistAdapter = new
                SocialShareChatlistAdapter(senderList, senderListNames);
        chatList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        chatList.setItemAnimator(new DefaultItemAnimator());
        chatList.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        chatList.setAdapter(chatlistAdapter);
        chatList.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(),
                chatList, new MainActivity.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                launchShareActivity(position);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
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
                //String contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
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
            case R.id.b_social_share_exit:
                stopServices();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pathFileObserver.stopWatching();
    }
}

