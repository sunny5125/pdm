package com.disarm.surakshit.pdm;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.provider.Settings;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;

import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.disarm.surakshit.pdm.Chat.ChatActivity;
import com.disarm.surakshit.pdm.DB.DBEntities.App;
import com.disarm.surakshit.pdm.DB.DBEntities.Receiver;
import com.disarm.surakshit.pdm.DB.DBEntities.Receiver_;
import com.disarm.surakshit.pdm.DB.DBEntities.Sender;
import com.disarm.surakshit.pdm.DB.DBEntities.Sender_;
import com.disarm.surakshit.pdm.DisarmConnect.DCService;
import com.disarm.surakshit.pdm.Encryption.KeyBasedFileProcessor;
import com.disarm.surakshit.pdm.Fragments.FragmentAdapter;
import com.disarm.surakshit.pdm.Service.SyncService;
import com.disarm.surakshit.pdm.Util.DiffUtils;
import com.disarm.surakshit.pdm.Util.Params;
import com.disarm.surakshit.pdm.location.LocationState;
import com.disarm.surakshit.pdm.location.MLocation;
import com.onegravity.contactpicker.contact.Contact;
import com.onegravity.contactpicker.core.ContactPickerActivity;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.osmdroid.bonuspack.kml.KmlDocument;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import io.objectbox.Box;

public class MainActivity extends AppCompatActivity {

    HandlerThread ht;
    Handler h, h_diff;
    static int total_kml = 0;
    static HashSet<String> kmlFilesList;
    private boolean gpsService = false;
    public static SyncService syncService;
    public static DCService myService;
    public static boolean syncServiceBound = false;
    public static boolean myServiceBound = false;
    LocationManager lm;
    LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportFragmentManager().getFragments().size() == 2) {
            for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                Log.d("Fragment", "remove");
            }
        }
        FragmentAdapter fragmentAdapter = new FragmentAdapter(getSupportFragmentManager());
        ViewPager mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(fragmentAdapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
        View root = tabLayout.getChildAt(0);
        if (root instanceof LinearLayout) {
            ((LinearLayout) root).setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(getResources().getColor(R.color.white));
            drawable.setSize(2, 1);
            ((LinearLayout) root).setDividerPadding(10);
            ((LinearLayout) root).setDividerDrawable(drawable);
        }
        crashLog();
        startService();
        kmlFilesList = new HashSet<>();
        File kmlDir = Environment.getExternalStoragePublicDirectory("DMS/KML/Dest/SourceKml");
        for (File file : kmlDir.listFiles()) {
            //file.getName() returns pathname from which file name without the
            //extension and path is extracted using FilenameUtils
            kmlFilesList.add(FilenameUtils.getBaseName(file.getName()));
            total_kml++;
        }
        //Data mule setting
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        //ht is a thread with a looper
        ht = new HandlerThread("FileWatcher");
        ht.start();
        h = new Handler(ht.getLooper());
        //decrypt
        h.post(new Runnable() {
            @Override
            public void run() {
                File kmlDir = Environment.getExternalStoragePublicDirectory("DMS/Working/SurakshitKml");
                File[] kmlfiles = kmlDir.listFiles();
                if (kmlfiles.length > total_kml) {
                    for (File file : kmlfiles) {
                        try {
                            if (!file.getName().contains(Params.SOURCE_PHONE_NO)) {
                                continue;
                            }
                        } catch (Exception e) {
                            continue;
                        }
                        if (!kmlFilesList.contains(FilenameUtils.getBaseName(file.getName()))) {
                            boolean done = true;
                            String keyPath = Environment.getExternalStoragePublicDirectory("DMS/pgpPrivate/pri_" + Params.SOURCE_PHONE_NO + ".bgp").getAbsolutePath();
                            try {
                                KeyBasedFileProcessor.decrypt(file.getAbsolutePath(), keyPath, Params.PASS_PHRASE);
                            } catch (Exception e) {
                                done = false;
                                e.printStackTrace();
                            }
                            if (done) {
                                total_kml++;
                                kmlFilesList.add(FilenameUtils.getBaseName(file.getName()));
                                try {
                                    updateDB(FilenameUtils.getBaseName(file.getName()));
                                } catch (IOException | ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                h.postDelayed(this, 1500);
            }
        });
        //find diff of receiver and apply
        h_diff = new Handler(ht.getLooper());
        h_diff.post(new Runnable() {
            @Override
            public void run() {
                File diffDir = Environment.getExternalStoragePublicDirectory("DMS/Working/SurakshitDiff");
                File latestDestDir = Environment.getExternalStoragePublicDirectory("DMS/KML/Dest/LatestKml");
                File sourceDestDir = Environment.getExternalStoragePublicDirectory("DMS/KML/Dest/SourceKml");
                File[] diffFiles = diffDir.listFiles();
                HashMap<String, File> myDiffFiles = new HashMap<>();
                HashMap<String, Integer> latestVersion = new HashMap<>();

                for (File file : diffFiles) {
                    String[] name = FilenameUtils.getBaseName(file.getName()).split("_");
                    if (!latestVersion.containsKey(name[0])) {
                        latestVersion.put(name[0], Integer.parseInt(name[4]));
                    }
                    if (name[2].equals(Params.SOURCE_PHONE_NO) && Integer.parseInt(name[4]) >= latestVersion.get(name[0])) {
                        myDiffFiles.put(name[0], file);
                        latestVersion.put(name[0], Integer.parseInt(name[4]));
                    }
                }

                HashMap<String, File> sourceFiles = new HashMap<>();
                for (File file : sourceDestDir.listFiles()) {
                    String[] name = file.getName().split("_");
                    sourceFiles.put(name[0], file);
                }
                //always true
                if (latestDestDir.listFiles().length > 0) {
                    for (File file : latestDestDir.listFiles()) {
                        String fileName = FilenameUtils.getBaseName(file.getName());
                        String split[] = fileName.split("_");
                        if (myDiffFiles.containsKey(split[0])) {
                            //I have the latest version of the file
                            File diff = myDiffFiles.get(split[0]);
                            int version = Integer.parseInt(FilenameUtils.getBaseName(diff.getName()).split("_")[4]);
                            int curr_version = Integer.parseInt(split[4]);
                            if (curr_version < version) {
                                //if my version is less than the version i got
                                File source = sourceFiles.get(split[0]);
                                if (DiffUtils.applyPatch(source, diff, getApplication(), MainActivity.this)) {
                                    try {
                                        FileUtils.forceDelete(file);
                                        final Box<Receiver> receiverBox = ((App) getApplication()).getBoxStore().boxFor(Receiver.class);
                                        final Box<Sender> senderBox = ((App) getApplication()).getBoxStore().boxFor(Sender.class);
                                        String number = split[1];

                                        List<Receiver> receivers = receiverBox.query().equal(Receiver_.number, number).build().find();
                                        List<Sender> senders = senderBox.query().equal(Sender_.number, number).build().find();

                                        Receiver receiver = receivers.get(0);
                                        File latestkml = Environment.getExternalStoragePublicDirectory("DMS/KML/Dest/LatestKml");
                                        for (File kml : latestkml.listFiles()) {
                                            if (kml.getName().contains(split[0])) {
                                                KmlDocument latestkmldocument = new KmlDocument();
                                                latestkmldocument.parseKMLFile(kml);
                                                int total = Integer.parseInt(latestkmldocument.mKmlRoot.getExtendedData("total"));
                                                receiver.setUnread(total - receiver.getTotalMsg());
                                                receiver.setTotalMsg(total);
                                                String lastMsgReceiver = getLastMessage(latestkmldocument);
                                                receiver.setLastMessage(lastMsgReceiver);
                                                receiver.setKml(FileUtils.readFileToString(kml));
                                                if (senders.size() != 0) {
                                                    Sender sender = senders.get(0);
                                                    String lastMsg = sender.getLastMessage();
                                                    String sender_time = lastMsg.split("-")[0];
                                                    DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);
                                                    Date sender_d = df.parse(sender_time);
                                                    Date receiver_d = df.parse(lastMsgReceiver.split("-")[0]);
                                                    if (sender_d.before(receiver_d)) {
                                                        receiver.setLastUpdated(true);
                                                        sender.setLastUpdated(false);
                                                    } else {
                                                        receiver.setLastUpdated(false);
                                                        sender.setLastUpdated(true);
                                                    }
                                                    senderBox.put(sender);
                                                } else {
                                                    receiver.setLastUpdated(true);
                                                }
                                                receiverBox.put(receiver);
                                            }
                                        }
                                        senderBox.closeThreadResources();
                                        receiverBox.closeThreadResources();
                                    } catch (IOException | ParseException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
                //will not execute
                else {
                    for (File diff : diffFiles) {
                        String split[] = diff.getName().split("_");
                        if (split[1].equals(Params.SOURCE_PHONE_NO)) {
                            continue;
                        }
                        String identifier = split[0];
                        File source = sourceFiles.get(identifier);
                        if (DiffUtils.applyPatch(source, diff, getApplication(), MainActivity.this)) {
                            final Box<Receiver> receiverBox = ((App) getApplication()).getBoxStore().boxFor(Receiver.class);
                            final Box<Sender> senderBox = ((App) getApplication()).getBoxStore().boxFor(Sender.class);
                            String number = split[1];
                            List<Receiver> receivers = receiverBox.query().equal(Receiver_.number, number).build().find();
                            List<Sender> senders = senderBox.query().equal(Sender_.number, number).build().find();
                            Receiver receiver = receivers.get(0);
                            File latestkml = Environment.getExternalStoragePublicDirectory("DMS/KML/Dest/LatestKml");
                            for (File kml : latestkml.listFiles()) {
                                if (kml.getName().contains(identifier)) {
                                    KmlDocument latestkmldocument = new KmlDocument();
                                    latestkmldocument.parseKMLFile(kml);
                                    int total = Integer.parseInt(latestkmldocument.mKmlRoot.getExtendedData("total"));
                                    receiver.setUnread(total - receiver.getTotalMsg());
                                    receiver.setTotalMsg(total);
                                    String lastMsgReceiver = getLastMessage(latestkmldocument);
                                    receiver.setLastMessage(lastMsgReceiver);
                                    try {
                                        receiver.setKml(FileUtils.readFileToString(kml));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    if (senders.size() != 0) {
                                        Sender sender = senders.get(0);
                                        String lastMsg = sender.getLastMessage();
                                        String sender_time = lastMsg.split("-")[0];
                                        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);
                                        Date sender_d = null;
                                        try {
                                            sender_d = df.parse(sender_time);
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }
                                        Date receiver_d = null;
                                        try {
                                            receiver_d = df.parse(lastMsgReceiver.split("-")[0]);
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }
                                        assert sender_d != null;
                                        if (sender_d.before(receiver_d)) {
                                            receiver.setLastUpdated(true);
                                            sender.setLastUpdated(false);
                                        } else {
                                            receiver.setLastUpdated(false);
                                            sender.setLastUpdated(true);
                                        }
                                        senderBox.put(sender);
                                    } else {
                                        receiver.setLastUpdated(true);
                                    }
                                    receiverBox.put(receiver);
                                }
                            }
                            senderBox.closeThreadResources();
                            receiverBox.closeThreadResources();
                        }
                    }
                }
                h_diff.postDelayed(this, 1500);
            }
        });
        //move files from temp to working directory
        final Handler h_tempKey = new Handler(ht.getLooper());
        h_tempKey.post(new Runnable() {
            @Override
            public void run() {
                File tempDir = Environment.getExternalStoragePublicDirectory("DMS/temp");
                File pgpKeyDir = Environment.getExternalStoragePublicDirectory("DMS/Working/pgpKey");
                HashMap<String, File> keyFile = new HashMap<>();
                for (File file : pgpKeyDir.listFiles()) {
                    String number = FilenameUtils.getBaseName(file.getName()).split("_")[1];
                    keyFile.put(number, file);
                }

                for (File file : tempDir.listFiles()) {
                    String fileName[] = FilenameUtils.getBaseName(file.getName()).split("_");
                    String fileNumber = fileName[2];
                    String unique = fileName[0];
                    if (keyFile.containsKey(fileNumber)) {
                        String inputPath = file.getAbsolutePath();
                        String publicKeyPath = Environment.getExternalStoragePublicDirectory("DMS/Working/pgpKey/pub_" + fileNumber + ".bgp").getAbsolutePath();
                        String outputFilePath = Environment.getExternalStoragePublicDirectory("DMS/Working/SurakshitKml/"
                                + FilenameUtils.getBaseName(file.getName()) + ".bgp")
                                .getAbsolutePath();
                        try {
                            KeyBasedFileProcessor.encrypt(inputPath, publicKeyPath, outputFilePath);
                            String outputPath = "DMS/KML/Source/LatestKml/";
                            String outputPath2 = "DMS/KML/Source/SourceKml/";
                            File file1 = Environment.getExternalStoragePublicDirectory(outputPath + file.getName());
                            File file2 = Environment.getExternalStoragePublicDirectory(outputPath2 + file.getName());
                            FileUtils.copyFile(file, file1);
                            FileUtils.copyFile(file, file2);
                            FileUtils.forceDelete(file);
                            File tempMedia = Environment.getExternalStoragePublicDirectory("DMS/tempMedia");
                            for (File f : tempMedia.listFiles()) {
                                String type = "";
                                if (f.getName().contains(unique)) {
                                    switch (FilenameUtils.getExtension(f.getName())) {
                                        case "jpeg":
                                            type = "Images";
                                            break;
                                        case "mp4":
                                            type = "Videos";
                                            break;
                                        case "png":
                                            type = "Map";
                                            break;
                                    }
                                    String outputFolder = "DMS/Working/Surakshit" + type + "/";
                                    File outputFile = Environment.getExternalStoragePublicDirectory(outputFolder + f.getName());
                                    FileUtils.moveFile(f, outputFile);
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                h_tempKey.postDelayed(this, 1000);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            AlertDialog ad = new AlertDialog.Builder(this)
                    .setTitle("WARNING")
                    .setMessage("Changing any settings will lead you to restart of the app")
                    .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent ii = new Intent(MainActivity.this, SettingActivity.class);
                            startActivity(ii);
                            dialogInterface.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create();
            ad.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ht.quit();
        unbindAllService();
    }

    private void updateDB(String fileName) throws IOException, ParseException {
        File destKmlDir = Environment.getExternalStoragePublicDirectory("DMS/KML/Dest/SourceKml");
        for (File file : destKmlDir.listFiles()) {
            if (file.getName().contains(FilenameUtils.getBaseName(fileName))) {
                final Box<Receiver> receiverBox = ((App) getApplication()).getBoxStore().boxFor(Receiver.class);
                final Box<Sender> senderBox = ((App) getApplication()).getBoxStore().boxFor(Sender.class);
                Receiver receiver = new Receiver();
                String fileSplit[] = fileName.split("_");
                receiver.setNumber(fileSplit[1]);
                KmlDocument kmlDocument = new KmlDocument();
                kmlDocument.parseKMLFile(file);
                int total = Integer.parseInt(kmlDocument.mKmlRoot.getExtendedData("total"));
                receiver.setTotalMsg(total);
                String kml = FileUtils.readFileToString(file);
                receiver.setKml(kml);
                receiver.setUnread(0);
                String nextKey = "source";
                String msg = "";
                while (kmlDocument.mKmlRoot.mExtendedData.containsKey(nextKey)) {
                    msg = kmlDocument.mKmlRoot.getExtendedData(nextKey);
                    nextKey = getTimeStampFromMsg(msg);
                }
                receiver.setLastMessage(msg);
                List<Sender> senderList = senderBox.query().contains(Sender_.number, fileSplit[1]).build().find();
                if (senderList.size() != 0) {
                    Sender sender = senderList.get(0);
                    String message = sender.getLastMessage();
                    String split[] = message.split("-");
                    String time = split[0];
                    DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);
                    Date d = df.parse(time);
                    String split2[] = msg.split("-");
                    String time2 = split2[0];
                    Date d2 = df.parse(time2);
                    if (d.after(d2)) {
                        receiver.setLastUpdated(false);
                    } else {
                        receiver.setLastUpdated(true);
                    }
                } else {
                    receiver.setLastUpdated(true);
                }
                receiverBox.put(receiver);
                receiverBox.closeThreadResources();
                senderBox.closeThreadResources();
            }
        }
    }

    private String getTimeStampFromMsg(String msg) {
        Pattern p = Pattern.compile("-");
        String[] s = p.split(msg, 4);
        return s[0];
    }

    private String getLastMessage(KmlDocument kml) {
        String nextKey = "source";
        String msg = "";
        while (kml.mKmlRoot.mExtendedData.containsKey(nextKey)) {
            msg = kml.mKmlRoot.getExtendedData(nextKey);
            nextKey = getTimeStampFromMsg(msg);
        }
        return msg;
    }

    private void crashLog() {
        // draw crash logs in a file every time the application crashes
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {

                Calendar cal = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                File crashLogFile = new File(SplashActivity.DMS_PATH + "PDM_CrashLog");
                if (!crashLogFile.exists()) {
                    crashLogFile.mkdir();
                }
                String filename = crashLogFile + "/" + sdf.format(cal.getTime()) + ".txt";

                PrintStream writer;
                try {
                    writer = new PrintStream(filename, "UTF-8");
                    writer.println(e.getClass() + ": " + e.getMessage());
                    for (int i = 0; i < e.getStackTrace().length; i++) {
                        writer.println(e.getStackTrace()[i].toString());
                    }
                    System.exit(1);
                } catch (FileNotFoundException | UnsupportedEncodingException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    private void startService() {
        final HandlerThread htd = new HandlerThread("Sync");
        htd.start();
        final Handler h = new Handler(htd.getLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                if (Params.SOURCE_PHONE_NO == null) {
                    SharedPreferences sp = getSharedPreferences("Surakshit", MODE_PRIVATE);
                    Params.SOURCE_PHONE_NO = sp.getString("phone_no", null);
                    h.postDelayed(this, 1000);
                } else {
                    final Intent syncServiceIntent = new Intent(getApplicationContext(), SyncService.class);
                    bindService(syncServiceIntent, syncServiceConnection, Context.BIND_AUTO_CREATE);
                    startService(syncServiceIntent);
                    htd.quit();
                }
            }
        });

//            final Intent dcServiceIntent = new Intent(getApplicationContext(),DCService.class);
//            bindService(dcServiceIntent,myServiceConnection,Context.BIND_AUTO_CREATE);
//            startService(dcServiceIntent);


        if (!LocationState.with(MainActivity.this).locationServicesEnabled()) {
            enableGPS();
        }
        MLocation.subscribe(MainActivity.this);
    }

    public void unbindSyncService() {
        final Intent syncServiceIntent = new Intent(getApplicationContext(), SyncService.class);
        if (syncServiceBound) {
            unbindService(syncServiceConnection);
        }
        syncServiceBound = false;
        stopService(syncServiceIntent);
    }

    private void unbindAllService() {
        unbindSyncService();

        final Intent myServiceIntent = new Intent(getApplicationContext(), DCService.class);
        if (myServiceBound) {
            unbindService(myServiceConnection);
        }
        myServiceBound = false;
        stopService(myServiceIntent);

        if (gpsService) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            lm.removeUpdates(locationListener);
            gpsService = false;
        }
    }

    public static ServiceConnection syncServiceConnection = new ServiceConnection() {

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

    public static ServiceConnection myServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            DCService.MyServiceBinder binder = (DCService.MyServiceBinder) service;
            myService = binder.getService();
            myServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            myServiceBound = false;
        }
    };

    public void enableGPS() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder
                .setMessage(R.string.gps_msg)
                .setCancelable(false)
                .setTitle("Turn on Location")
                .setPositiveButton(R.string.enable_gps,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                Intent callGPSSettingIntent = new Intent(
                                        Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivityForResult(callGPSSettingIntent, 5);
                            }
                        });
        alertDialogBuilder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        int CONTACT_REQUEST = 100;
        if (requestCode == 5 && resultCode == 0) {
            String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            if (provider != null) {
                switch (provider.length()) {
                    case 0:
                        //GPS still not enabled..
                        Toast.makeText(MainActivity.this, "Please enable GPS!!!", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        MLocation.subscribe(MainActivity.this);
                        Toast.makeText(this, R.string.enabled_gps, Toast.LENGTH_LONG).show();
                        break;
                }
            }
        } else if (requestCode == CONTACT_REQUEST && resultCode == Activity.RESULT_OK &&
                data != null && data.hasExtra(ContactPickerActivity.RESULT_CONTACT_DATA)) {
            List<Contact> contacts = (List<Contact>) data.getSerializableExtra(ContactPickerActivity.RESULT_CONTACT_DATA);
            for (Contact contact : contacts) {
                // process the contacts...
                Intent intent = new Intent(this, ChatActivity.class);
                String s = contact.getPhone(0);
                String num = "";
                int count = 0;
                for (int i = s.length() - 1; i >= 0; i--) {
                    if (s.charAt(i) >= '0' && s.charAt(i) <= '9') {
                        num = num + s.charAt(i);
                        count++;
                    }
                    if (count == 10)
                        break;
                }
                StringBuilder sb = new StringBuilder(num);
                sb = sb.reverse();
                intent.putExtra("number", sb.toString());
                startActivity(intent);
            }
        } else if (requestCode == 5) {
            //the user did not enable his GPS
            enableGPS();
        }
    }


}
