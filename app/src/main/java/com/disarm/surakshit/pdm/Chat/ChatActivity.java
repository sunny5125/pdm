package com.disarm.surakshit.pdm.Chat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.disarm.surakshit.pdm.BuildConfig;
import com.disarm.surakshit.pdm.Chat.Holders.IncomingAudioHolders;
import com.disarm.surakshit.pdm.Chat.Holders.IncomingVideoHolders;
import com.disarm.surakshit.pdm.Chat.Holders.OutgoingAudioHolders;
import com.disarm.surakshit.pdm.Chat.Holders.OutgoingVideoHolders;
import com.disarm.surakshit.pdm.Chat.Utils.ChatUtils;
import com.disarm.surakshit.pdm.CollectMapDataActivity;
import com.disarm.surakshit.pdm.DB.DBEntities.App;
import com.disarm.surakshit.pdm.DB.DBEntities.Receiver;
import com.disarm.surakshit.pdm.DB.DBEntities.Receiver_;
import com.disarm.surakshit.pdm.DB.DBEntities.Sender;
import com.disarm.surakshit.pdm.DB.DBEntities.Sender_;
import com.disarm.surakshit.pdm.Encryption.KeyBasedFileProcessor;
import com.disarm.surakshit.pdm.Encryption.SignedFileProcessor;
import com.disarm.surakshit.pdm.GetLocationActivity;
import com.disarm.surakshit.pdm.R;
import com.disarm.surakshit.pdm.ShowMapDataActivity;
import com.disarm.surakshit.pdm.Util.ContactUtil;
import com.disarm.surakshit.pdm.Util.DiffUtils;
import com.disarm.surakshit.pdm.Util.Params;
import com.disarm.surakshit.pdm.location.MLocation;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.github.javiersantos.materialstyleddialogs.enums.Duration;
import com.github.javiersantos.materialstyleddialogs.enums.Style;
import com.squareup.picasso.Picasso;
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.messages.MessageHolders;
import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.util.GeoPoint;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import info.hoang8f.widget.FButton;
import io.objectbox.Box;

public class ChatActivity extends AppCompatActivity implements MessageHolders.ContentChecker<Message> {
    MessagesList messagesList;
    MessageInput messageInput;
    ImageLoader load;
    String number;
    String unique = "";
    Author me, other;
    MessagesListAdapter<Message> messagesListAdapter;
    ArrayList<Message> allMessages;
    int total_msg_receiver = 0;
    MaterialStyledDialog materialDialog;
    private final byte CONTENT_AUDIO = 1, CONTENT_VIDEO = 2;
    HandlerThread ht;
    Handler h;
    int previous_total = 0;
    String last_file_name = "";
    String pathToFile = "";
    public static GeoPoint currLoc = null;
    Uri uri;
    //audio
    boolean isRecording, isPlaying;
    private MediaRecorder recorder;
    private MediaPlayer mediaPlayer;
    private ImageView recordButton, playButton;
    private Chronometer chronometer;
    private FButton backButton, okayButton;
    private TextView recordText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Location l = MLocation.getLocation(getApplicationContext());
        if (l == null) {
            Intent i = new Intent(this, GetLocationActivity.class);
            startActivityForResult(i, 6666);
        } else {
            currLoc = new GeoPoint(l.getLatitude(), l.getLongitude());
        }
        setContentView(R.layout.activity_chat);
        messagesList = (MessagesList) findViewById(R.id.messagesList);
        messageInput = (MessageInput) findViewById(R.id.input);
        ActionBar ab = getSupportActionBar();
        Drawable d = getResources().getDrawable(R.color.fbutton_color_turquoise);
        if (ab != null) {
            ab.setBackgroundDrawable(d);
        }

        ht = new HandlerThread("newMsg");
        ht.start();
        h = new Handler(ht.getLooper());
        final Handler locHandler = new Handler(ht.getLooper());
        locHandler.post(new Runnable() {
            @Override
            public void run() {
                Location l = MLocation.getLocation(getApplicationContext());
                if (l == null) {
                    locHandler.postDelayed(this, 1000);
                } else {
                    currLoc.setLatitude(l.getLatitude());
                    currLoc.setLongitude(l.getLongitude());
                }
            }
        });
        allMessages = new ArrayList<>();
        number = getIntent().getStringExtra("number");
        String receiversName = ContactUtil.getContactName(getApplicationContext(), number);
        me = new Author(Params.SOURCE_PHONE_NO, "Me");
        other = new Author(number, receiversName);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(receiversName);
        }
        setChatConfig();
        //Define what to do with msg touch events
        setMessagesListAdapterListener();
        //define onClick of attachment in message input
        setMessageInputAttachmentListener();
        //define send button of message input
        setMessageInputSendListener();

        h.post(new Runnable() {
            @Override
            public void run() {
                final Box<Receiver> receiverBox = ((App) getApplication()).getBoxStore().boxFor(Receiver.class);
                List<Receiver> receivers = receiverBox.query().equal(Receiver_.number, number).build().find();
                final Box<Sender> senderBox = ((App) getApplication()).getBoxStore().boxFor(Sender.class);
                List<Sender> senders = senderBox.query().equal(Sender_.number, number).build().find();
                //cal populateChat if there are some messages sent or received
                if (receivers.size() != 0 || senders.size() != 0) {
                    populateChat();
                }
                h.postDelayed(this, 1500);
            }
        });

    }

    @Override
    public boolean hasContentFor(Message message, byte type) {
        if (type == CONTENT_AUDIO) {
            return message.isAudio();
        }
        if (type == CONTENT_VIDEO) {
            return message.isVideo();
        }
        return false;
    }

    private void populateChat() {

        final Box<Sender> senderBox = ((App) getApplication()).getBoxStore().boxFor(Sender.class);
        final Box<Receiver> receiverBox = ((App) getApplication()).getBoxStore().boxFor(Receiver.class);

        List<Sender> senders = senderBox.query().equal(Sender_.number, number).build().find();
        List<Receiver> receivers = receiverBox.query().equal(Receiver_.number, number).build().find();

        KmlDocument senderKml = new KmlDocument();
        KmlDocument receiversKml = new KmlDocument();

        if (senders.size() != 0) {
            String sendersKml = senders.get(0).getKml();
            InputStream sendersStream = new ByteArrayInputStream(sendersKml.getBytes(StandardCharsets.UTF_8));
            //get sender kml file
            senderKml.parseKMLStream(sendersStream, null);
        }
        if (receivers.size() != 0) {
            Receiver receiver = receivers.get(0);
            receiver.setUnread(0);
            receiverBox.put(receiver);
            String receiverKml = receivers.get(0).getKml();
            InputStream receiversStream = new ByteArrayInputStream(receiverKml.getBytes(StandardCharsets.UTF_8));
            //get receiver kml file
            receiversKml.parseKMLStream(receiversStream, null);
        }
        extractMessageFromKML(senderKml, receiversKml);
        senderBox.closeThreadResources();
        receiverBox.closeThreadResources();
    }

    private void extractMessageFromKML(final KmlDocument sender, final KmlDocument receiver) {
        allMessages.clear();
        String nextKey = "source";
        String msg;
        while (sender.mKmlRoot.mExtendedData != null && sender.mKmlRoot.mExtendedData.containsKey(nextKey)) {
            msg = sender.mKmlRoot.getExtendedData(nextKey);
            nextKey = getTimeStampFromMsg(msg);
            allMessages.add(ChatUtils.getMessageObject(msg, me));
        }
        nextKey = "source";
        while (receiver.mKmlRoot.mExtendedData != null && receiver.mKmlRoot.mExtendedData.containsKey(nextKey)) {
            msg = receiver.mKmlRoot.getExtendedData(nextKey);
            nextKey = getTimeStampFromMsg(msg);
            allMessages.add(ChatUtils.getMessageObject(msg, other));
            //not needed
            total_msg_receiver++;
        }
        if (previous_total < allMessages.size()) {
            sortAllMessage();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    messagesListAdapter.clear();
                    messagesListAdapter.notifyDataSetChanged();
                    messagesListAdapter.addToEnd(allMessages, false);
                    previous_total = allMessages.size();
                }
            });

        }

    }

    private void sortAllMessage() {
        Collections.sort(allMessages, new Comparator<Message>() {
            @Override
            public int compare(Message o1, Message o2) {
                if (o1.getCreatedAt().after(o2.getCreatedAt())) {
                    return -1;
                } else if (o1.getCreatedAt().before(o2.getCreatedAt())) {
                    return 1;
                } else return 0;
            }
        });
    }

    private String getTimeStampFromMsg(String msg) {
        Pattern p = Pattern.compile("-");
        String[] s = p.split(msg, 4);
        return s[0];
    }

    private void setChatConfig() {
        //load images in the chat list called by the adapter
        load = new ImageLoader() {
            @Override
            public void loadImage(ImageView imageView, String url) {
                File im = Environment.getExternalStoragePublicDirectory(url);

                if (im.exists()) {
                    Picasso.get().load(im).resize(800, 1000).centerCrop().into(imageView);
                } else {
                    String name = FilenameUtils.getName(url);
                    File img = Environment.getExternalStoragePublicDirectory("DMS/tempMedia/" + name);
                    Picasso.get().load(img).resize(800, 1000).centerCrop().into(imageView);
                }
            }
        };
        MessageHolders holders = new MessageHolders();
        holders.registerContentType(CONTENT_AUDIO,
                IncomingAudioHolders.class, R.layout.chat_incoming_audio,
                OutgoingAudioHolders.class, R.layout.chat_outgoing_audio,
                this);
        holders.registerContentType(CONTENT_VIDEO,
                IncomingVideoHolders.class, R.layout.chat_incoming_video,
                OutgoingVideoHolders.class, R.layout.chat_outgoing_video,
                this);

        messagesListAdapter = new MessagesListAdapter<Message>(Params.SOURCE_PHONE_NO, holders, load);
    }

    private void setMessagesListAdapterListener() {
        messagesListAdapter.setOnMessageClickListener(new MessagesListAdapter.OnMessageClickListener<Message>() {
            @Override
            public void onMessageClick(Message message) {

                //Define what to do with msg touch events
                //Video touch and Audio touch are already defined in the holders

                if (message.isImage()) {
                    Intent i = new Intent(ChatActivity.this, ImageViewActivity.class);
                    i.putExtra("url", message.getImageUrl());
                    startActivity(i);
                } else if (message.isMap()) {
                    Intent i = new Intent(ChatActivity.this, ShowMapDataActivity.class);
                    if (message.getUser().getId().equals(Params.SOURCE_PHONE_NO)) {
                        i.putExtra("who", "me");
                    } else {
                        i.putExtra("who", "other");
                    }
                    i.putExtra("number", number);
                    i.putExtra("uniqueId", message.getMapObjectID());
                    startActivity(i);
                }
            }
        });
        messagesList.setAdapter(messagesListAdapter);
    }

    private void setMessageInputAttachmentListener() {
        messageInput.setAttachmentsListener(new MessageInput.AttachmentsListener() {
            @Override
            public void onAddAttachments() {
                File keyDir = Environment.getExternalStoragePublicDirectory("DMS/Working/pgpKey");
                boolean isKey = false;
                for (File file : keyDir.listFiles()) {
                    if (file.getName().contains(number)) {
                        isKey = true;
                        break;
                    }
                }
                if (!isKey) {
                    Toast.makeText(ChatActivity.this, "No security key found!!! It will be sent after getting encryption key", Toast.LENGTH_LONG).show();
                }
                View view = getLayoutInflater().inflate(R.layout.dialog_attachment, null);
                materialDialog = new MaterialStyledDialog.Builder(ChatActivity.this)
                        .setTitle(R.string.attachment)
                        .setCustomView(view, 10, 20, 10, 20)
                        .withDialogAnimation(true, Duration.FAST)
                        .setCancelable(true)
                        .setStyle(Style.HEADER_WITH_TITLE)
                        .withDarkerOverlay(true)
                        .build();

                Window window = materialDialog.getWindow();
                WindowManager.LayoutParams wlp = null;
                if (window != null) {
                    wlp = window.getAttributes();
                }
                assert wlp != null;
                wlp.gravity = Gravity.BOTTOM;
                wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                window.setAttributes(wlp);
                ImageButton camera = view.findViewById(R.id.attach_camera);
                final boolean finalIsKey = isKey;
                camera.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startCamera(finalIsKey);
                    }
                });
                ImageButton map = view.findViewById(R.id.attach_map);
                map.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startMap();
                    }
                });
                materialDialog.show();
                ImageButton video = view.findViewById(R.id.attach_video);
                video.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startVideo(finalIsKey);
                    }
                });
                ImageButton audio = view.findViewById(R.id.attach_audio);
                audio.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startAudio();
                    }
                });
            }
        });
    }

    private void setMessageInputSendListener() {
        messageInput.setInputListener(new MessageInput.InputListener() {
            @Override
            public boolean onSubmit(CharSequence input) {
                //check if number key is available or not
                File keyDir = Environment.getExternalStoragePublicDirectory("DMS/Working/pgpKey");
                boolean isKey = false;
                for (File file : keyDir.listFiles()) {
                    if (file.getName().contains(number)) {
                        isKey = true;
                        break;
                    }
                }
                String destinationPath = "";
                String sourcePath = "";
                if (!isKey) {
                    Toast.makeText(ChatActivity.this, "No security key found!!! It will be sent after getting the encryption key", Toast.LENGTH_LONG).show();
                    destinationPath = "DMS/temp/";
                    sourcePath = "DMS/temp/";
                } else {
                    destinationPath = "DMS/KML/Source/LatestKml/";
                    sourcePath = "DMS/KML/Source/SourceKml/";
                }

                final Box<Sender> senderBox = ((App) getApplication()).getBoxStore().boxFor(Sender.class);
                List<Sender> senders = senderBox.query().contains(Sender_.number, number).build().find();
                //sending message for first time
                if (messagesListAdapter.getItemCount() == 0 || senders.size() == 0) {
                    KmlDocument kml = new KmlDocument();
                    String extendedDataFormat = ChatUtils.getExtendedDataFormatName(input.toString(), "text", "none");
                    kml.mKmlRoot.setExtendedData("source", extendedDataFormat);
                    kml.mKmlRoot.setExtendedData("total", "1");
                    File file = getNewFileObject(sourcePath);
                    //save file in sourceKml
                    kml.saveAsKML(file);
                    if (isKey) {
                        File dest = Environment.getExternalStoragePublicDirectory(destinationPath + FilenameUtils.getBaseName(file.getName()) + ".kml");
                        try {
                            //if key is available then make another copy in latestKml
                            FileUtils.copyFile(file, dest);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    //update dB
                    Sender sender = new Sender();
                    sender.setNumber(number);
                    sender.setLastMessage(extendedDataFormat);
                    sender.setLastUpdated(true);
                    String kmlString = "";
                    try {
                        kmlString = FileUtils.readFileToString(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    sender.setKml(kmlString);
                    senderBox.put(sender);
                    //update Chat
                    populateChat();
                    if (isKey)
                        encryptIt(file);
                } else {
                    String destination = "";
                    if (!isKey) {
                        destination = "DMS/temp";
                    } else {
                        destination = "DMS/KML/Source/LatestKml";
                    }
                    KmlDocument kml = new KmlDocument();
                    File latestSourceDir = Environment.getExternalStoragePublicDirectory(destination);
                    File kmlFile = null;
                    for (File file : latestSourceDir.listFiles()) {
                        if (file.getName().contains(number)) {
                            kml.parseKMLFile(file);
                            kmlFile = file;
                            break;
                        }
                    }
                    String nextKey = "source";
                    String msg = "";
                    while (kml.mKmlRoot.mExtendedData.containsKey(nextKey)) {
                        msg = kml.mKmlRoot.getExtendedData(nextKey);
                        nextKey = getTimeStampFromMsg(msg);
                    }
                    String extendedDataFormat = ChatUtils.getExtendedDataFormatName(input.toString(), "text", "none");
                    kml.mKmlRoot.setExtendedData(nextKey, extendedDataFormat);
                    int total = Integer.parseInt(kml.mKmlRoot.getExtendedData("total"));
                    total++;
                    kml.mKmlRoot.setExtendedData("total", total + "");
                    kml.saveAsKML(kmlFile);
                    List<Sender> senderList = senderBox.query().contains(Sender_.number, number).build().find();
                    Sender s = senderList.get(0);
                    s.setLastUpdated(true);
                    s.setLastMessage(extendedDataFormat);
                    String kmlString = "";
                    try {
                        assert kmlFile != null;
                        kmlString = FileUtils.readFileToString(kmlFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    s.setKml(kmlString);
                    senderBox.put(s);
                    populateChat();
                    if (isKey) {
                        generateDiff(kmlFile);
                        Log.d("DIff", "Generating diff");
                    }
                }
                senderBox.closeThreadResources();
                return true;
            }
        });
    }

    //Generates a 16-bit unique random string
    private String generateRandomString() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] token = new byte[16];
        secureRandom.nextBytes(token);
        return new BigInteger(1, token).toString(16);
    }

    private String generateRandomString(int size) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] token = new byte[size];
        secureRandom.nextBytes(token);
        return new BigInteger(1, token).toString(16);
    }


    private File getNewFileObject(String path) {
        String fileName = generateRandomString() + "_" + Params.SOURCE_PHONE_NO + "_" + number + "_" + "50";
        return Environment.getExternalStoragePublicDirectory(path + fileName + ".kml");
    }

    private void encryptIt(File file) {

        String inputPath = file.getAbsolutePath();
        String publicKeyPath = Environment.getExternalStoragePublicDirectory("DMS/Working/pgpKey/pub_" + number + ".bgp").getAbsolutePath();
        String outputFilePath = Environment.getExternalStoragePublicDirectory("DMS/Working/SurakshitKml/"
                + FilenameUtils.getBaseName(file.getName()) + ".bgp")
                .getAbsolutePath();
        try {
            KeyBasedFileProcessor.encrypt(inputPath, publicKeyPath, outputFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void signAndEncrypt(File file, String broadcastTo) throws IOException {
        String inputPath = file.getAbsolutePath();
        String secretKeyPath;
        String outputPath = Environment.getExternalStoragePublicDirectory("DMS/temp/" + FilenameUtils.getBaseName(file.getName()) + ".asc").getAbsolutePath();
        if (broadcastTo.equals("user") || broadcastTo.equals("volunteer")) {
            secretKeyPath = Environment.getExternalStoragePublicDirectory("DMS/Working/pgpKey/pri_" + broadcastTo + ".bgp").getAbsolutePath();
        } else {
            secretKeyPath = Environment.getExternalStoragePublicDirectory("DMS/pgpPrivate/pri_" + number + ".bgp").getAbsolutePath();
        }
        SignedFileProcessor signedFileProcessor = new SignedFileProcessor();
        String pass;
        if (broadcastTo.equals("volunteer")) {
            pass = "volunteer@disarm321";
        } else {
            pass = Params.PASS_PHRASE;
        }
        signedFileProcessor.signFile(inputPath, outputPath, secretKeyPath, pass);
        String publicKeyPath = Environment.getExternalStoragePublicDirectory("DMS/Working/pgpKey/pub_" + number + ".bgp").getAbsolutePath();
        String outputFilePath = Environment.getExternalStoragePublicDirectory("DMS/Working/SurakshitKml/"
                + FilenameUtils.getBaseName(file.getName()) + ".bgp")
                .getAbsolutePath();
        try {
            KeyBasedFileProcessor.encrypt(outputPath, publicKeyPath, outputFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        FileUtils.forceDelete(Environment.getExternalStoragePublicDirectory("DMS/temp/temp.asc"));
    }

    private void generateDiff(final File dest) {
        final HandlerThread ht = new HandlerThread("Diff");
        ht.start();
        Handler diffHandler = new Handler(ht.getLooper());
        diffHandler.post(new Runnable() {
            @Override
            public void run() {
                File source = null;
                File sourceDir = Environment.getExternalStoragePublicDirectory("DMS/KML/Source/SourceKml");
                for (File file : sourceDir.listFiles()) {
                    if (file.getName().contains(number)) {
                        source = file;
                        break;
                    }
                }
                try {
                    Log.d("Diff", "Diff start");
                    DiffUtils.createDiff(source, dest, getApplication(), ChatActivity.this);
                    Log.d("Diff", "diff end");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ht.quit();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ht.quit();
    }

    private void startCamera(Boolean isKey) {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, "262144");
        //get unique value if i have already sent a message to the number
        if (unique.equals("")) {
            File sourceDir = Environment.getExternalStoragePublicDirectory("DMS/KML/Source/SourceKml");
            for (File file : sourceDir.listFiles()) {
                if (file.getName().contains(number)) {
                    String name = file.getName();
                    unique = name.split("_")[0];
                    break;
                }
            }
            File tempDir = Environment.getExternalStoragePublicDirectory("DMS/temp");
            for (File file : tempDir.listFiles()) {
                if (file.getName().contains(number)) {
                    String name = file.getName();
                    unique = name.split("_")[0];
                    break;
                }
            }
        }

        if (unique.equals("")) {
            unique = generateRandomString();
        }
        String fileName = unique + "_" + Params.SOURCE_PHONE_NO + "_" + number + "_50_" + generateRandomString(8) + ".jpeg";
        String path;
        if (isKey) {
            path = "DMS/Working/SurakshitImages/";
        } else {
            path = "DMS/tempMedia/";
        }
        //save image to a particular directory
        File image = Environment.getExternalStoragePublicDirectory(path + fileName);
        pathToFile = path + fileName;
        uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", image);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //get file name in last_file_name i.e.-- x.jpeg
        last_file_name = image.getName();
        startActivityForResult(cameraIntent, 1000);
    }

    private void startVideo(Boolean isKey) {
        Intent cameraIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (unique.equals("")) {
            File sourceDir = Environment.getExternalStoragePublicDirectory("DMS/KML/Source/SourceKml");
            for (File file : sourceDir.listFiles()) {
                if (file.getName().contains(number)) {
                    String name = file.getName();
                    unique = name.split("_")[0];
                    break;
                }
            }
            File tempDir = Environment.getExternalStoragePublicDirectory("DMS/temp");
            for (File file : tempDir.listFiles()) {
                if (file.getName().contains(number)) {
                    String name = file.getName();
                    unique = name.split("_")[0];
                    break;
                }
            }
        }
        if (unique.equals("")) {
            unique = generateRandomString();
        }
        String fileName = unique + "_" + Params.SOURCE_PHONE_NO + "_" + number + "_50_" + generateRandomString(8) + ".mp4";
        String path;
        if (isKey) {
            path = "DMS/Working/SurakshitVideos/";
        } else {
            path = "DMS/tempMedia/";
        }

        File image = Environment.getExternalStoragePublicDirectory(path + fileName);
        Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", image);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10);
        last_file_name = image.getName();
        startActivityForResult(cameraIntent, 1001);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == 1000 || requestCode == 1001) && resultCode == RESULT_OK) {
            final Box<Sender> senderBox = ((App) getApplication()).getBoxStore().boxFor(Sender.class);
            List<Sender> senders = senderBox.query().contains(Sender_.number, number).build().find();
            String type = "";
            if (requestCode == 1000) {

                type = "image";
                Bitmap bitmap = null;

                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Bitmap resized = Bitmap.createScaledBitmap(bitmap, 450, 640, true);
                FileOutputStream out = null;
                File f = Environment.getExternalStoragePublicDirectory(pathToFile);
                try {
                    out = new FileOutputStream(f.getAbsoluteFile());
                    resized.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
                    // PNG is a lossless format, the compression factor (100) is ignored
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                type = "video";
            }
            File keyDir = Environment.getExternalStoragePublicDirectory("DMS/Working/pgpKey");
            boolean isKey = false;
            for (File file : keyDir.listFiles()) {
                if (file.getName().contains(number)) {
                    isKey = true;
                    break;
                }
            }
            String destinationPath = "";
            if (!isKey) {
                Toast.makeText(ChatActivity.this, "No security key found!!! It will be sent after getting the encryption key", Toast.LENGTH_LONG).show();
                destinationPath = "DMS/temp/";
            } else {
                destinationPath = "DMS/KML/Source/LatestKml/";
            }
            //saving image or video for the first time
            if (messagesListAdapter.getItemCount() == 0 || senders.size() == 0) {
                KmlDocument kml = new KmlDocument();
                String extendedDataFormat = ChatUtils.getExtendedDataFormatName(last_file_name, type, "none");
                kml.mKmlRoot.setExtendedData("source", extendedDataFormat);
                kml.mKmlRoot.setExtendedData("total", "1");
                String fileName = last_file_name.split("_")[0] + "_" + Params.SOURCE_PHONE_NO + "_" + number + "_50.kml";
                File file = null;
                if (isKey)
                    file = Environment.getExternalStoragePublicDirectory("DMS/KML/Source/SourceKml/" + fileName);
                else
                    file = Environment.getExternalStoragePublicDirectory(destinationPath + fileName);
                //save file in SourceKml Directory or in Temps Folder
                kml.saveAsKML(file);
                if (isKey) {
                    File dest = Environment.getExternalStoragePublicDirectory(destinationPath + FilenameUtils.getBaseName(file.getName()) + ".kml");
                    try {
                        //save the same file in LatestKml directory
                        FileUtils.copyFile(file, dest);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //update dB
                Sender sender = new Sender();
                sender.setNumber(number);
                sender.setLastMessage(extendedDataFormat);
                sender.setLastUpdated(true);
                String kmlString = "";
                try {
                    kmlString = FileUtils.readFileToString(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sender.setKml(kmlString);
                senderBox.put(sender);
                //show message in chat
                populateChat();
                if (isKey) {
                    if (!(number.contains("user") || number.contains("volunteer")))
                        //encrypt for normal number contacts
                        encryptIt(file);
                    else {
                        try {
                            //otherwise sign and encrypt
                            signAndEncrypt(file, number);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            //saving image or video if message have been received or sent earlier
            else {
                String destination;
                if (isKey) {
                    destination = "DMS/KML/Source/LatestKml";
                } else {
                    destination = "DMS/temp";
                }
                KmlDocument kml = new KmlDocument();
                File latestSourceDir = Environment.getExternalStoragePublicDirectory(destination);
                File kmlFile = null;
                //get the desired kml file from the destination path
                for (File file : latestSourceDir.listFiles()) {
                    if (file.getName().contains(number)) {
                        kml.parseKMLFile(file);
                        kmlFile = file;
                        break;
                    }
                }
                String nextKey = "source";
                String msg = "";
                //get to the last message and get the timeStamp as nextKey
                while (kml.mKmlRoot.mExtendedData.containsKey(nextKey)) {
                    msg = kml.mKmlRoot.getExtendedData(nextKey);
                    nextKey = getTimeStampFromMsg(msg);
                }
                String extendedDataFormat = ChatUtils.getExtendedDataFormatName(last_file_name, type, "none");
                //update kml data
                kml.mKmlRoot.setExtendedData(nextKey, extendedDataFormat);
                //update total message in kml
                int total = Integer.parseInt(kml.mKmlRoot.getExtendedData("total"));
                total++;
                kml.mKmlRoot.setExtendedData("total", total + "");
                kml.saveAsKML(kmlFile);
                //update the dB
                List<Sender> senderList = senderBox.query().contains(Sender_.number, number).build().find();
                Sender s = senderList.get(0);
                s.setLastUpdated(true);
                s.setLastMessage(extendedDataFormat);
                String kmlString = "";
                try {
                    assert kmlFile != null;
                    kmlString = FileUtils.readFileToString(kmlFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                s.setKml(kmlString);
                senderBox.put(s);
                //update chat
                populateChat();
                //create diff
                if (isKey)
                    generateDiff(kmlFile);
            }
            senderBox.closeThreadResources();
            materialDialog.dismiss();
        }
        if (requestCode == 777 && resultCode == -1) {
            populateChat();
            materialDialog.dismiss();
        }

        if (requestCode == 6666) {
            if (resultCode == 1) {
                currLoc = new GeoPoint(data.getDoubleExtra("lat", 0.0), data.getDoubleExtra("lon", 0.0));
            } else {
                Toast.makeText(this, "Something went wrong!!! You can't proceed further", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void startMap() {
        Intent ii = new Intent(this, CollectMapDataActivity.class);
        ii.putExtra("number", number);
        File latestKmlDir = Environment.getExternalStoragePublicDirectory("DMS/KML/Source/SourceKml/");
        File[] files = latestKmlDir.listFiles();
        File latestKmlFile = null;
        boolean isKey = false;
        String fileName;
        for (File file : files) {
            if (file.getName().contains(number)) {
                latestKmlFile = file;
                isKey = true;
                break;
            }
        }
        if (!isKey) {
            File tempDir = Environment.getExternalStoragePublicDirectory("DMS/temp");
            for (File file : tempDir.listFiles()) {
                if (file.getName().contains(number)) {
                    latestKmlFile = file;
                    break;
                }
            }
        }
        if (latestKmlFile == null) {
            fileName = generateRandomString() + "_" + Params.SOURCE_PHONE_NO + "_" + number + "_50.kml";
        } else {
            fileName = latestKmlFile.getName();
        }
        ii.putExtra("kml", fileName);
        ii.putExtra("key", isKey);
        startActivityForResult(ii, 777);
    }

    public void startAudio() {
        View view = getLayoutInflater().inflate(R.layout.dialog_audio_record, null);
        final MaterialStyledDialog materialStyledDialog = new MaterialStyledDialog.Builder(ChatActivity.this)
                .setTitle(R.string.attachment)
                .setCustomView(view, 10, 20, 10, 20)
                .withDialogAnimation(true, Duration.FAST)
                .setCancelable(false)
                .setStyle(Style.HEADER_WITH_TITLE)
                .withDarkerOverlay(true)
                .build();
        recordButton = view.findViewById(R.id.record_button_dialog);
        chronometer = view.findViewById(R.id.chronometer);
        backButton = view.findViewById(R.id.record_dialog_back_button);
        okayButton = view.findViewById(R.id.record_dialog_ok_button);
        recordText = view.findViewById(R.id.record_button_text);
        playButton = view.findViewById(R.id.play_button_dialog);

        playButton.setEnabled(false);
        isRecording = false;
        isPlaying = false;
        recorder = null;

        //file name for the file
        if (unique.equals("")) {
            File sourceDir = Environment.getExternalStoragePublicDirectory("DMS/KML/Source/SourceKml");
            for (File file : sourceDir.listFiles()) {
                if (file.getName().contains(number)) {
                    String name = file.getName();
                    unique = name.split("_")[0];
                    break;
                }
            }
            File tempDir = Environment.getExternalStoragePublicDirectory("DMS/temp");
            for (File file : tempDir.listFiles()) {
                if (file.getName().contains(number)) {
                    String name = file.getName();
                    unique = name.split("_")[0];
                    break;
                }
            }
        }

        if (unique.equals("")) {
            unique = generateRandomString();
        }

        final String fileName = unique + "_" + Params.SOURCE_PHONE_NO + "_" + number + "_50_" + generateRandomString(8) + ".3gp";
        String path;

        //find if key is present
        File keyDir = Environment.getExternalStoragePublicDirectory("DMS/Working/pgpKey");
        boolean isKey = false;
        for (File file : keyDir.listFiles()) {
            if (file.getName().contains(number)) {
                isKey = true;
                break;
            }
        }

        if (isKey) {
            path = "DMS/Working/SurakshitAudio/";
        } else {
            path = "DMS/tempMedia/";
        }

        final String finalFilePath = Environment.getExternalStoragePublicDirectory(path + fileName).getAbsolutePath();

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                materialStyledDialog.dismiss();
                stopPlaying();
                File file = new File(finalFilePath);
                try {
                    if (file.exists())
                        FileUtils.forceDelete(file);
                    Log.d("AUDIO_CHAT", "file deleted");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        final boolean finalIsKey = isKey;
        okayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                materialStyledDialog.dismiss();
                stopPlaying();
                saveAudioFileHelper(finalIsKey, fileName);
                Log.d("AUDIO_CHAT", "fileNAme:" + fileName);
            }
        });

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    startRecording(finalFilePath);
                    backButton.setEnabled(false);
                    okayButton.setEnabled(false);
                    playButton.setEnabled(false);
                    playButton.setImageResource(R.drawable.ic_play_audio_gray_40dp);
                    recordText.setText(getString(R.string.recording));
                    recordButton.setImageResource(R.drawable.ic_record_audio_80dp);
                    isRecording = true;
                } else {
                    stopRecording();
                    backButton.setEnabled(true);
                    okayButton.setEnabled(true);
                    playButton.setEnabled(true);
                    playButton.setImageResource(R.drawable.ic_play_audio_40dp);
                    recordText.setText(getString(R.string.record));
                    recordButton.setImageResource(R.drawable.ic_recording_audio_80dp);
                    isRecording = false;
                }
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPlaying) {
                    startPlaying(finalFilePath);
                    recordButton.setImageResource(R.drawable.ic_record_audio_off_40dp);
                    recordButton.setEnabled(false);
                    playButton.setImageResource(R.drawable.ic_pause_audio_40dp);
                } else {
                    stopPlaying();
                    recordButton.setImageResource(R.drawable.ic_recording_audio_80dp);
                    recordButton.setEnabled(true);
                    playButton.setImageResource(R.drawable.ic_play_audio_40dp);
                }
            }
        });

        materialStyledDialog.show();
        materialDialog.dismiss();
    }

    private void saveAudioFileHelper(Boolean isKey, String filePath) {
        String type = "audio";
        final Box<Sender> senderBox = ((App) getApplication()).getBoxStore().boxFor(Sender.class);
        List<Sender> senders = senderBox.query().contains(Sender_.number, number).build().find();

        String destinationPath = "";
        if (!isKey) {
            Toast.makeText(ChatActivity.this, "No security key found!!! It will be sent after getting the encryption key", Toast.LENGTH_LONG).show();
            destinationPath = "DMS/temp/";
        } else {
            destinationPath = "DMS/KML/Source/LatestKml/";
        }
        //saving audio for the first time
        if (messagesListAdapter.getItemCount() == 0 || senders.size() == 0) {
            KmlDocument kml = new KmlDocument();
            String extendedDataFormat = ChatUtils.getExtendedDataFormatName(filePath, type, "none");
            kml.mKmlRoot.setExtendedData("source", extendedDataFormat);
            kml.mKmlRoot.setExtendedData("total", "1");
            //saving the kml file
            String fileName = filePath.split("_")[0] + "_" + Params.SOURCE_PHONE_NO + "_" + number + "_50.kml";
            File file = null;
            if (isKey)
                file = Environment.getExternalStoragePublicDirectory("DMS/KML/Source/SourceKml/" + fileName);
            else
                file = Environment.getExternalStoragePublicDirectory(destinationPath + fileName);
            //save file in SourceKml Directory or in Temps Folder
            kml.saveAsKML(file);
            if (isKey) {
                File dest = Environment.getExternalStoragePublicDirectory(destinationPath + FilenameUtils.getBaseName(file.getName()) + ".kml");
                try {
                    //save the same file in LatestKml directory
                    FileUtils.copyFile(file, dest);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //update dB
            Sender sender = new Sender();
            sender.setNumber(number);
            sender.setLastMessage(extendedDataFormat);
            sender.setLastUpdated(true);
            String kmlString = "";
            try {
                kmlString = FileUtils.readFileToString(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            sender.setKml(kmlString);
            senderBox.put(sender);
            //show message in chat
            populateChat();
            if (isKey) {
                if (!(number.contains("user") || number.contains("volunteer")))
                    //encrypt for normal number contacts
                    encryptIt(file);
                else {
                    try {
                        //otherwise sign and encrypt
                        signAndEncrypt(file, number);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            String destination;
            if (isKey) {
                destination = "DMS/KML/Source/LatestKml";
            } else {
                destination = "DMS/temp";
            }
            KmlDocument kml = new KmlDocument();
            File latestSourceDir = Environment.getExternalStoragePublicDirectory(destination);
            File kmlFile = null;
            //get the desired kml file from the destination path
            for (File file : latestSourceDir.listFiles()) {
                if (file.getName().contains(number)) {
                    kml.parseKMLFile(file);
                    kmlFile = file;
                    break;
                }
            }
            String nextKey = "source";
            String msg = "";
            //get to the last message and get the timeStamp as nextKey
            while (kml.mKmlRoot.mExtendedData.containsKey(nextKey)) {
                msg = kml.mKmlRoot.getExtendedData(nextKey);
                nextKey = getTimeStampFromMsg(msg);
            }
            String extendedDataFormat = ChatUtils.getExtendedDataFormatName(filePath, type, "none");
            //update kml data
            kml.mKmlRoot.setExtendedData(nextKey, extendedDataFormat);
            //update total message in kml
            int total = Integer.parseInt(kml.mKmlRoot.getExtendedData("total"));
            total++;
            kml.mKmlRoot.setExtendedData("total", total + "");
            kml.saveAsKML(kmlFile);
            //update the dB
            List<Sender> senderList = senderBox.query().contains(Sender_.number, number).build().find();
            Sender s = senderList.get(0);
            s.setLastUpdated(true);
            s.setLastMessage(extendedDataFormat);
            String kmlString = "";
            try {
                assert kmlFile != null;
                kmlString = FileUtils.readFileToString(kmlFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            s.setKml(kmlString);
            senderBox.put(s);
            //update chat
            populateChat();
            //create diff
            if (isKey)
                generateDiff(kmlFile);
        }
        senderBox.closeThreadResources();
    }

    private void startRecording(String filePath) {

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(filePath);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e("AUDIO_CHAT", "prepare() failed");
        }

        recorder.start();
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
    }

    private void stopRecording() {
        recorder.stop();
        chronometer.stop();
        recorder.release();
        recorder = null;
    }

    private void startPlaying(String filePath) {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();
            mediaPlayer.start();
            isPlaying = true;
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopPlaying();
                    recordButton.setImageResource(R.drawable.ic_recording_audio_80dp);
                    recordButton.setEnabled(true);
                    playButton.setImageResource(R.drawable.ic_play_audio_40dp);
                }
            });
        } catch (IOException e) {
            Log.e("AUDIO_CHAT", "prepare() failed");
        }

    }

    private void stopPlaying() {
        if (mediaPlayer != null)
            mediaPlayer.release();
        isPlaying = false;
        mediaPlayer = null;
        chronometer.stop();
    }
}
