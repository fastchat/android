package com.fastchat.fastchat.models;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.fastchat.fastchat.Utils;
import com.fastchat.fastchat.networking.NetworkManager;

public class Message {

    private String id;
    private String text;
    private User from;
    private String groupId;
    private String sentTime;
    private boolean hasMedia;
    private MultiMedia media;
    private long media_size;
    private String content_type;
    private static final String TAG=Message.class.getSimpleName();

    public Message(String text,User from){
        this.text=text;
        this.from=from;
    }

    public Message(String text, User from, String groupId, MultiMedia media){
        this.text=text;
        this.from=from;
        this.media=media;
        this.groupId=groupId;
        if(media!=null && media.getData().length()>0){
            this.media_size=media.getData().length();
            this.content_type = media.getMimeType();
            hasMedia=true;
        }
    }

    public Message(JSONObject messageObject){
        try {
            this.text=messageObject.getString("text");
            this.id=messageObject.getString("_id");
            this.groupId = messageObject.getString("group");
            this.sentTime= messageObject.getString("sent");
            this.from= NetworkManager.getUsernameFromId(messageObject.getString("from"));
            this.hasMedia = messageObject.getBoolean("hasMedia");
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if(this.from==null){
            this.from=NetworkManager.getFastChatUser();
        }
        if(this.hasMedia){
            try {
                this.content_type = messageObject.getJSONArray("mediaHeader").getString(0);
                Log.d(TAG,"Content Type: "+this.content_type);
                File f = new File(getFullFilePath()); // Check if file is already saved.
                if(f.exists()){
                    MultiMedia mms = new MultiMedia(getFileName(),content_type,f);
                    setMedia(mms);
                }
                this.media_size = messageObject.getJSONArray("media_size").getLong(0);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }


    public String getDateString(){
        //SimpleDateFormat dfOut = new SimpleDateFormat("MM/dd/yyyy hh:mm a",Locale.US);

        SimpleDateFormat dfOut = new SimpleDateFormat("MMM dd, hh:mm");
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        dfOut.setTimeZone(tz);
        if(this.sentTime==null || this.sentTime.equals("")){
            Date date = new Date();
            return dfOut.format(date);
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date date = df.parse(this.sentTime);

            return dfOut.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            Utils.makeToast(e);
        }
        return "";
    }

    public String getText(){
        return this.text;
    }

    public boolean isMine(){
        return NetworkManager.getCurrentUser().getUsername().equals(this.from.getUsername());
    }

    public User getFrom(){
        return this.from;
    }

    public String getId(){
        return this.id;
    }

    public void setId(String newId){
        this.id=newId;
    }

    public String getGroupId(){
        return this.groupId;
    }

    public boolean hasMedia(){
        return this.hasMedia;
    }

    public MultiMedia getMedia(){
        return this.media;
    }

    public void setMedia(MultiMedia m){
        this.media=m;
    }

    public JSONArray getSendFormat(){
        JSONObject message = new JSONObject();
        JSONArray array = new JSONArray();
        try {
            message.put("text", this.text);
            message.put("group", this.groupId);
            array.put(message);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Utils.makeToast(e);
        }
        Log.i(this.getClass().getName(),"Send Message:"+array);
        return array;
    }

    public long getMedia_size() {
        return media_size;
    }

    public String getFileName(){
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(content_type);
        if(extension==null){
            extension=content_type;
        }
        String fileName = this.id+"."+extension;
        Log.d(TAG,"File Name: "+fileName);
        return fileName;
    }

    public String getFullFilePath(){
        String path = "";
        String type = MultiMedia.getType(content_type);
        if(type.equals("image")){
            path+=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        }else if(type.equals("video")){
            path+=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        }else if(type.equals("audio")){
            path+=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        }else{
            path+=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        }
        path+="/Fast Chat";
        File directory = new File(path);
        if(!directory.exists())//check if file already exists
        {
            directory.mkdirs();//if not, create it
        }
        path+="/"+getFileName();
        Log.d(TAG,"Full File PAth: "+path);
        return path;
    }

    public String getContent_type() {
        return content_type;
    }
}
