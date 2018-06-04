package com.disarm.surakshit.pdm.Chat.Utils;

import com.disarm.surakshit.pdm.Chat.Author;
import com.disarm.surakshit.pdm.Chat.ChatActivity;
import com.disarm.surakshit.pdm.Chat.Message;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Created by naman on 11/3/18.
 */

public class ChatUtils {

    
    public static String getExtendedDataFormatName(String text,String type,String mapID){
        String lat = ChatActivity.currLoc.getLatitude()+"";
        String lon = ChatActivity.currLoc.getLongitude()+"";
        String latlon = lat + "_" + lon;
        return getTimeStamp()+"-"+type+"-"+mapID+"-"+text+"-"+latlon;
    }

    public static String getTimeStamp(){
        return new SimpleDateFormat("yyyyMMddHHmmss",Locale.ENGLISH).format(new Date());
    }

    //Get a Message object from a extended data format string
    public static Message getMessageObject(String extendedData, Author author){
        Message message = null;
        Pattern p = Pattern.compile("-");
        String[] s = p.split(extendedData,5);
        String date = s[0];
        String type = s[1];
        String mapObjectId = s[2];
        String text = s[3];
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);
        try {
            Date d = df.parse(date);
            message = new Message(date, author, type, d);
            switch (type) {
                case "text":
                    message.setText(text);
                    break;
                case "image": {
                    String url = "DMS/Working/SurakshitImages/" + text;
                    message.setImageurl(url);
                    break;
                }
                case "map": {
                    String url = "DMS/Working/SurakshitMap/" + text;
                    message.setImageurl(url);
                    message.setMapObjectID(mapObjectId);
                    break;
                }
                case "audio": {
                    String url = "DMS/Working/SurakshitAudio/" + text;
                    message.setUrl(url);
                    break;
                }
                case "video": {
                    String url = "DMS/Working/SurakshitVideos/" + text;
                    message.setUrl(url);
                    break;
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return message;
    }


}
