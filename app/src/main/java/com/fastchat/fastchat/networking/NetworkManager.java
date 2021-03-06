package com.fastchat.fastchat.networking;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.util.Log;

import com.fastchat.fastchat.CacheManager;
import com.fastchat.fastchat.MainActivity;
import com.fastchat.fastchat.Utils;
import com.fastchat.fastchat.ui.GroupsFragment;
import com.fastchat.fastchat.ui.LoginFragment;
import com.fastchat.fastchat.ui.MessageFragment;
import com.fastchat.fastchat.ui.ProfileFragment;
import com.fastchat.fastchat.models.Group;
import com.fastchat.fastchat.models.Message;
import com.fastchat.fastchat.models.MultiMedia;
import com.fastchat.fastchat.models.User;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpClient.DownloadCallback;
import com.koushikdutta.async.http.AsyncHttpClient.FileCallback;
import com.koushikdutta.async.http.AsyncHttpClient.JSONArrayCallback;
import com.koushikdutta.async.http.AsyncHttpClient.JSONObjectCallback;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.body.FilePart;
import com.koushikdutta.async.http.body.JSONObjectBody;
import com.koushikdutta.async.http.body.MultipartFormDataBody;

public class NetworkManager {

    private static String url ="http://powerful-cliffs-9562.herokuapp.com:80";
    public static void setUrl(String url) {
        NetworkManager.url = url;
    }



    //private static final String url ="http://129.21.118.49:3000";
    //protected static String url = "http://localhost:3000";
    private static String currentUserId = "0";
    private static Group currentGroup;
    // HashMap <groupId, Groups>
    private static HashMap<String,Group> groups = new HashMap<String,Group>();
    private static HashMap<String, User> users  = new HashMap<String,User>();
    private static User fastChatUser = new User(null,"FastChat",null);

    private static final String TAG=NetworkManager.class.getSimpleName();


    private static final JSONObjectCallback loginCallback = new AsyncHttpClient.JSONObjectCallback() {
        // Callback is invoked with any exceptions/errors, and the result, if available.
        public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject result) {
            int responseCode = handleResponse(e,response,result);
            Utils.makeToast("" + responseCode);
            Utils.makeToast(result.toString());
            Log.d(TAG,"I got a JSONObject: " + result);
            if(responseCode<200 || responseCode>299){
                return;
            }
            try {
                getCurrentUser().setToken(result.getString("session-token"));
            } catch (JSONException e1) {
                e1.printStackTrace();
                Utils.makeToast(e1);
                return;
            }
        }
    };

    public static Future<JSONObject> postLogin(String username, String password){
        User u = new User(null, username, null);
        setCurrentUser(u);
        AsyncHttpPost post = new AsyncHttpPost(url+"/login");
        JSONObject object = new JSONObject();
        try {
            object.put("password", password);
            object.put("username", username);
        } catch (JSONException e) {
            Utils.makeToast(e);
            e.printStackTrace();
            return null;
        }
        JSONObjectBody body = new JSONObjectBody(object);
        post.setBody(body);
        return AsyncHttpClient.getDefaultInstance().executeJSONObject(post, loginCallback);
    }


    public static Future<JSONArray> getGroups(){

        AsyncHttpGet get = new AsyncHttpGet(url+"/group");
        get.setHeader("session-token", getCurrentUser().getSessionToken());
        return AsyncHttpClient.getDefaultInstance().executeJSONArray(get,new AsyncHttpClient.JSONArrayCallback() {
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONArray result) {
                int responseCode = handleResponse(e,response);
                if(responseCode>=200 && responseCode<300){
                    Log.d(TAG,"GET groups: "+result);
                    GroupsFragment.addGroups(result);
                }else{
                    MainActivity.restartFragments(new LoginFragment());
                }
            }
        });

    }


    public static Future<JSONObject> postDeviceId(String reg_id){
        if(reg_id==null || reg_id.equals("")){
            return null;
        }
        if(getCurrentUser()==null || getCurrentUser().getSessionToken()==null){
            return null;
        }
        Log.d(TAG,"Posting device registration token"+getCurrentUser().getSessionToken());
        Log.d(TAG,"REGID: "+reg_id);
        AsyncHttpPost post = new AsyncHttpPost(url+"/user/device");
        post.setHeader("session-token", getCurrentUser().getSessionToken());
        JSONObject object = new JSONObject();
        try {
            object.put("token", reg_id);
            object.put("type", "android");
        } catch (JSONException e) {
            Utils.makeToast(e);
            e.printStackTrace();
            return null;
        }
        JSONObjectBody body = new JSONObjectBody(object);
        post.setBody(body);
        return AsyncHttpClient.getDefaultInstance().executeJSONObject(post, new AsyncHttpClient.JSONObjectCallback() {
            // Callback is invoked with any exceptions/errors, and the result, if available.
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject result) {
                int responseCode = handleResponse(e,response,result);
                Log.d(TAG,"code:"+responseCode+" result:"+result);
            }
        });
    }



    private static final JSONArrayCallback groupMessagesCallback = new AsyncHttpClient.JSONArrayCallback() {
        // Callback is invoked with any exceptions/errors, and the result, if available.
        public void onCompleted(Exception e, AsyncHttpResponse response, JSONArray result) {
            int responseCode =handleResponse(e,response);
            if(responseCode<200 || responseCode>299){
                Utils.makeToast("Unable to retrieve groups");
                return;
            }
            String requestUrl = response.getRequest().getUri().toString();
            String[] urlSplit = requestUrl.split("/");
            String groupId = urlSplit[urlSplit.length-2];
            MessageFragment.removeAllMessages(groupId);
            for(int i=0;i<result.length();i++){
                int j = result.length()-i-1;
                try {
                    JSONObject messageObject = result.getJSONObject(j);

                    MessageFragment.addMessage(new Message(messageObject));
                } catch (JSONException e1) {
                    e1.printStackTrace();
                    Utils.makeToast(e1);
                }

            }
            Log.d(TAG,"Group Message:"+result);
        }
    };


    public static Future<JSONArray> getCurrentGroupMessages()
    {
        if(currentGroup==null){
            return null;
        }
        String groupId = currentGroup.getId();
        AsyncHttpGet get = new AsyncHttpGet(url+"/group/"+groupId+"/message");
        get.setHeader("session-token", getCurrentUser().getSessionToken());
        return AsyncHttpClient.getDefaultInstance().executeJSONArray(get,groupMessagesCallback);
    }


    public static Future<JSONObject> postLogout(){
        AsyncHttpRequest http = new AsyncHttpRequest(Uri.parse(url+"/logout"),"DELETE");
        http.setHeader("session-token", getCurrentUser().getSessionToken());
        return AsyncHttpClient.getDefaultInstance().executeJSONObject(http, new AsyncHttpClient.JSONObjectCallback() {
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject result) {
                handleResponse(e,response,result,"Successfully logged out");
            };
        });
    }


    public static Future<JSONObject> postLogoutAll(){
        AsyncHttpRequest http = new AsyncHttpRequest(Uri.parse(url+"/logout?all=true"),"DELETE");
        http.setHeader("session-token", getCurrentUser().getSessionToken());
        return AsyncHttpClient.getDefaultInstance().executeJSONObject(http, new AsyncHttpClient.JSONObjectCallback() {
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject result) {
                handleResponse(e,response,result,"Successfully logged out every device");
            };
        });
    }


    private static final JSONObjectCallback profileCallback = new AsyncHttpClient.JSONObjectCallback() {
        public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject result) {
            handleResponse(e,response,result);
            JSONObject profileObject;
            try {
                profileObject = result.getJSONObject("profile");
                User tempUser = new User(profileObject);
                tempUser.setToken(getToken());
                Log.d(TAG,"currentUser: "+tempUser.getId()+":"+tempUser.getUsername()+":"+tempUser.getSessionToken());
                NetworkManager.setCurrentUser(tempUser);
                MainActivity.saveLoginCredentials(tempUser);
            } catch (JSONException e1) {
                e1.printStackTrace();
                Utils.makeToast(e1);
            }

            Log.d(TAG,"Profile:"+result);

        };
    };

    public static Future<JSONObject> getProfile(){
        AsyncHttpGet get = new AsyncHttpGet(url+"/user");
        get.setHeader("session-token", getCurrentUser().getSessionToken());
        return AsyncHttpClient.getDefaultInstance().executeJSONObject(get,profileCallback);
    }

    public static Future<JSONObject> putLeaveGroup(Group g){
        AsyncHttpRequest http = new AsyncHttpRequest(Uri.parse(url+"/group/"+g.getId()+"/leave"),"PUT");
        http.setHeader("session-token", getCurrentUser().getSessionToken());
        groups.remove(g);
        return AsyncHttpClient.getDefaultInstance().executeJSONObject(http, new AsyncHttpClient.JSONObjectCallback() {
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject result) {
                handleResponse(e,response,result,"Successfully left the group");
            };
        });
    }


    public static Future<JSONObject> postCreateGroup(List<String> userNames,String groupName, String message){
        AsyncHttpPost post = new AsyncHttpPost(url+"/group");
        post.setHeader("session-token", getCurrentUser().getSessionToken());
        JSONObject object = new JSONObject();
        try {
            object.put("members", new JSONArray(userNames));
            object.put("name", groupName);
            object.put("text", message);
        } catch (JSONException e) {
            Utils.makeToast(e);
            e.printStackTrace();
            return null;
        }
        JSONObjectBody body = new JSONObjectBody(object);
        post.setBody(body);
        return AsyncHttpClient.getDefaultInstance().executeJSONObject(post, new AsyncHttpClient.JSONObjectCallback() {
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject result) {
                handleResponse(e,response,result);
                MainActivity.switchView(new GroupsFragment());
                NetworkManager.getGroups();
            };
        });
    }



    public static Future<JSONObject> putInviteUser(String username, Group g){
        AsyncHttpRequest http = new AsyncHttpRequest(Uri.parse(url+"/group/"+g.getId()+"/add"),"PUT");
        http.setHeader("session-token", getCurrentUser().getSessionToken());
        JSONObject object = new JSONObject();
        try {
            object.put("invitees", new JSONArray(Arrays.asList(username)));
        } catch (JSONException e) {
            Utils.makeToast(e);
            e.printStackTrace();
            return null;
        }
        JSONObjectBody body = new JSONObjectBody(object);
        http.setBody(body);

        return AsyncHttpClient.getDefaultInstance().executeJSONObject(http, new AsyncHttpClient.JSONObjectCallback() {
            // Callback is invoked with any exceptions/errors, and the result, if available.
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject result) {
                handleResponse(e,response,result);
                MainActivity.switchView(new GroupsFragment());
                NetworkManager.getGroups();
            };
        });
    }

    public static Future<JSONObject> postRegisterUser(String username, String password){
        AsyncHttpPost post = new AsyncHttpPost(url+"/user");
        JSONObject object = new JSONObject();
        try {
            object.put("username", username);
            object.put("password", password);
        } catch (JSONException e) {
            Utils.makeToast(e);
            e.printStackTrace();
            return null;
        }
        JSONObjectBody body = new JSONObjectBody(object);
        post.setBody(body);
        return AsyncHttpClient.getDefaultInstance().executeJSONObject(post, new AsyncHttpClient.JSONObjectCallback() {
            // Callback is invoked with any exceptions/errors, and the result, if available.
            public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject result) {
                handleResponse(e,response,result,"Registration Successful! Login to continue");
            }
        });
    }

    private static final DownloadCallback dataCallback = new AsyncHttpClient.DownloadCallback() {

        @Override
        public void onCompleted(Exception e, AsyncHttpResponse source,
                                ByteBufferList result) {
            int responseCode = handleResponse(e,source);
            if(responseCode<200 || responseCode>299){
                return;
            }
            String requestUrl = source.getRequest().getUri().toString();
            String[] urlSplit = requestUrl.split("/");
            String userId = urlSplit[urlSplit.length-2];

            byte[] data = result.getAllByteArray();
			/*try {
				CacheManager.cacheData(MainActivity.activity, data, userId+".jpeg");
			} catch (IOException e1) {
				e1.printStackTrace();
			}*/
            Log.d(TAG,"Avatar UserID: "+userId+ "Length: "+data.length);
            Bitmap avatar = BitmapFactory.decodeByteArray(data, 0, data.length);

            if(avatar==null){
                Log.d(TAG,"Avatar null for user:"+userId+"");
                return;
            }
            avatar = ProfileFragment.getRoundedCornerBitmap(avatar);
            NetworkManager.getUsernameFromId(userId).setAvatarBitmap(avatar);
        }

    };

    public static synchronized Future<ByteBufferList> getAvatar(String id) {
		/*File file = null;
		try {
			file = CacheManager.retrieveData(MainActivity.activity, id+".jpeg");
		} catch (IOException e) {
		}*/

        AsyncHttpGet get = new AsyncHttpGet(url+"/user/"+id+"/avatar");
		/*if(file!=null){
			long lastModified = file.lastModified();
			Date date = new Date(lastModified);
			SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
			String modifiedHeader= format.format(date);
			get.setHeader("If-Modified-Since", modifiedHeader);
			Log.d(TAG,"lastModied:" + modifiedHeader);
		}*/
        get.setHeader("session-token", getCurrentUser().getSessionToken());
        return AsyncHttpClient.getDefaultInstance().executeByteBufferList(get,dataCallback);
    }

    public static Future<String> postAvatar(Bitmap bitmap){
        Log.d(TAG,"POSTING user Avatar: "+url+"/user/"+getCurrentUser().getId()+"/avatar");
        AsyncHttpPost post = new AsyncHttpPost(url+"/user/"+getCurrentUser().getId()+"/avatar");
        post.setHeader("session-token", getCurrentUser().getSessionToken());
        MultipartFormDataBody body = new MultipartFormDataBody();
        File file;
        try {
            file = CacheManager.cacheData(MainActivity.activity, bitmap, getCurrentUser().getId()+".jpeg");
        } catch (IOException e1) {
            e1.printStackTrace();
            return null;
        }
        //String fileDirectory = saveToInternalSorage(bitmap)+"/avatar.jpeg";
        FilePart fp = new FilePart("avatar",file);
        fp.setContentType("image/jpeg");
        body.addPart(fp);
        post.setBody(body);
        return AsyncHttpClient.getDefaultInstance().executeString(post, new AsyncHttpClient.StringCallback() {

            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response,
                                    String result) {
                NetworkManager.handleResponse(e, response,null,"Successfully saved avatar");
                Log.d(TAG,"Avatar result"+result);
            }
        });
    }

    public static Future<String> postMultimediaMessage(Message m){
        Log.d(TAG,"POSTING multimedia Message: "+url+"/group/"+m.getGroupId()+"/message");
        AsyncHttpPost post = new AsyncHttpPost(url+"/group/"+m.getGroupId()+"/message");
        post.setHeader("session-token", getCurrentUser().getSessionToken());
        MultipartFormDataBody body = new MultipartFormDataBody();

        FilePart fp = new FilePart("media",m.getMedia().getData());
        fp.setContentType(m.getMedia().getMimeType());
        body.addPart(fp);
        post.setBody(body);
        body.addStringPart("text", m.getText());

        return AsyncHttpClient.getDefaultInstance().executeString(post, new AsyncHttpClient.StringCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response,
                                    String result) {
                int responseCode = NetworkManager.handleResponse(e, response,null,"Successfully sent multimedia message");
                if(responseCode<200 || responseCode>300){
                    Log.e(TAG,responseCode+": "+result);
                }
            }
        });
    }

    private static final FileCallback mediaCallback2 = new AsyncHttpClient.FileCallback() {

        @Override
        public void onCompleted(Exception e, AsyncHttpResponse source, File result) {
            int responseCode = handleResponse(e,source);
            if(responseCode<200 || responseCode>299){
                return;
            }
            Log.d(TAG,"Finished Downloading File");
            String requestUrl = source.getRequest().getUri().toString();
            String[] urlSplit = requestUrl.split("/");
            String messageId = urlSplit[urlSplit.length-2];
            String groupId = urlSplit[urlSplit.length-4];

            Log.d(TAG,"Media MessageID: "+messageId+ " Group Id: "+groupId+" Length: "+result.length());

            String content_type = source.getHeaders().getHeaders().get("Content-type");
            MultiMedia mms = new MultiMedia("test.tmp",content_type,result);
            ArrayList<Message> messagesList = groups.get(groupId).getMessages();
            for(Message m : messagesList){
                if(m.getId().equals(messageId)){
                    Log.d(TAG, "Setting an image for message " + m.getText());
                    m.setMedia(mms);
                    MessageFragment.updateUI();
                    break;
                }
            }
        }

        public void onProgress(AsyncHttpResponse response, long downloaded, long total){

            String requestUrl = response.getRequest().getUri().toString();
            String[] urlSplit = requestUrl.split("/");
            String messageId = urlSplit[urlSplit.length-2];
            String groupId = urlSplit[urlSplit.length-4];
            //Log.d(TAG,"GroupId: "+groupId+" Downloaded:"+downloaded+" Total: "+total);
            if(!groupId.equals(currentGroup.getId())){
                return;
            }
            ArrayList<Message> messagesList = groups.get(groupId).getMessages();
            int position = 0;
            for(Message m : messagesList){
                if(m.getId().equals(messageId)){
                    break;
                }
                position+=1;
            }
            MessageFragment.getIndividiualView(position,downloaded, total);

        }

    };

    public static Future<File> getMessageMedia(Message m) {
        AsyncHttpGet get = new AsyncHttpGet(url+"/group/"+m.getGroupId()+"/message/"+m.getId()+"/media");
        Log.d(TAG,"URL: "+url+"/group/"+m.getGroupId()+"/message/"+m.getId()+"/media");
        get.setHeader("session-token", getCurrentUser().getSessionToken());
        return AsyncHttpClient.getDefaultInstance().executeFile(get,m.getFullFilePath(),mediaCallback2);
    }



    private static int handleResponse(Exception e,AsyncHttpResponse response){
        return handleResponse(e,response,null,null);
    }
    private static int handleResponse(Exception e,AsyncHttpResponse response,JSONObject result){
        return handleResponse(e,response,result,null);
    }

    private static int handleResponse(Exception e,AsyncHttpResponse response,JSONObject result, String correctResponseText){
        if (e != null) {
            e.printStackTrace();
            Utils.makeToast(e);
            return 500;
        }

        int responseCode = response.getHeaders().getHeaders().getResponseCode();
        Log.d(TAG,responseCode+":"+response.getRequest().getMethod()+" "+response.getRequest().getUri().toString());
        if(responseCode>=200 && responseCode<300){
            if(correctResponseText!=null){
                Utils.makeToast(correctResponseText);
            }
        }else{
            String errorMessage = "";
            if(result!=null){
                try {
                    errorMessage = result.getString("error");

                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
            }
            //Utils.makeToast(responseCode+": "+errorMessage);
            Log.e(TAG,responseCode+": "+errorMessage);
        }
        return responseCode;

    }


    public static String getURL(){
        return url;
    }

    public static String getToken() {
        return getCurrentUser().getSessionToken();
    }

    public static Group getCurrentGroup(){
        return currentGroup;
    }

    public static HashMap<String,Group> getAllGroups(){
        return groups;
    }

    public static void setGroups(HashMap<String,Group> g){
        groups = g;
    }

    public static void setCurrentRoom(Group group){
        currentGroup = group;
    }

    public static User getCurrentUser(){
        return  users.get(currentUserId);
    }

    public static User getUsernameFromId(String id){
        return users.get(id);
    }

    public static void setCurrentUser(User user){
        if(users.containsKey("0")){ //Default value for a user is 0.
            users.remove("0");
        }

        if(!users.containsKey(user.getId())){
            users.put(user.getId(), user);
        }else{
            User tempUser = users.get(user.getId());
            tempUser.setToken(user.getSessionToken());
            tempUser.setUsername(user.getUsername());
        }
        currentUserId=user.getId();
    }

    public static HashMap<String,User> getUsersMap(){
        return users;
    }
    public static User getFastChatUser(){
        return fastChatUser;
    }
}
