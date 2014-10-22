package com.fastchat.fastchat.networking;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Vibrator;
import android.util.Log;

import java.util.HashSet;
import java.util.Timer;

import com.fastchat.fastchat.MainActivity;
import com.fastchat.fastchat.Utils;
import com.fastchat.fastchat.models.User;
import com.fastchat.fastchat.ui.FastChatTextWatcher;
import com.fastchat.fastchat.ui.GroupsFragment;
import com.fastchat.fastchat.ui.MessageFragment;
import com.fastchat.fastchat.models.Group;
import com.fastchat.fastchat.models.Message;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.socketio.Acknowledge;
import com.koushikdutta.async.http.socketio.ConnectCallback;
import com.koushikdutta.async.http.socketio.DisconnectCallback;
import com.koushikdutta.async.http.socketio.EventCallback;
import com.koushikdutta.async.http.socketio.JSONCallback;
import com.koushikdutta.async.http.socketio.ReconnectCallback;
import com.koushikdutta.async.http.socketio.SocketIOClient;
import com.koushikdutta.async.http.socketio.SocketIORequest;
import com.koushikdutta.async.http.socketio.StringCallback;

public class SocketIoController {

    private static SocketIOClient client;

    private static Future<SocketIOClient> clientFuture;

    private static final String TAG=SocketIoController.class.getName();

    private static Long lastTypingEventTime  = null;
    private static Thread typingThread;

    private static final long RESET_TIME = 2000L;

    public static Future<SocketIOClient> connect(){
        String newURL = NetworkManager.getURL();
        Log.d(TAG,"Socket.io connect:"+newURL+"token:"+NetworkManager.getToken());
        SocketIORequest request = new SocketIORequest(newURL,null,"token="+NetworkManager.getToken());

        //request.setHeader("token", NetworkManager.getToken());
        if(clientFuture !=null){
            if(!clientFuture.isDone()){
                return clientFuture;
            }
        }
        if(getClient()!=null){
            if(getClient().isConnected())
            {
                getClient().disconnect();
            }
        }
        clientFuture = SocketIOClient.connect(AsyncHttpClient.getDefaultInstance(), request, new ConnectCallback() {
            @Override
            public void onConnectCompleted(Exception ex, SocketIOClient client) {
                if (ex != null) {
                    ex.printStackTrace();
                    Utils.makeToast(ex);
                    return;
                }



                Log.d(TAG, "EVENT: " + client);

                client.setStringCallback(new StringCallback() {

                    @Override
                    public void onString(String string, Acknowledge acknowledge) {
                        Log.d(TAG,"onString:"+string);

                    }
                });
                client.on("message", new EventCallback() {

                    @Override
                    public void onEvent(JSONArray argument,
                                        Acknowledge acknowledge) {
                        Log.d(TAG,"onEvent message:"+argument);
                        try {
                            JSONObject messageObject = argument.getJSONObject(0);
                            Message message = new Message(messageObject);
                            Group currGroup = NetworkManager.getCurrentGroup();
                            if(currGroup==null || !currGroup.getId().equals(message.getGroupId())){
                                Group tempGroup = NetworkManager.getAllGroups().get(message.getGroupId());
                                tempGroup.addMessage(message);
                                tempGroup.addOneToUnreadCount();
                                GroupsFragment.updateUi();
                                Vibrator v = (Vibrator) MainActivity.activity.getSystemService(Context.VIBRATOR_SERVICE);
                                v.vibrate(200);
                            }
                            else{
                                MessageFragment.addMessage(message);
                            }
                        } catch (JSONException e) {
                            Utils.makeToast(e);
                            e.printStackTrace();
                        }

                    }
                });
                client.on("typing",new EventCallback(){
                    @Override
                    public void onEvent(JSONArray argument,
                                        Acknowledge acknowledge) {

                        JSONObject typingObject;
                        try {
                            typingObject = argument.getJSONObject(0);
                            Log.d(TAG,"new onEvent typing:"+typingObject.getBoolean("typing"));
                            String userId = typingObject.getString("from");
                            boolean isTyping = typingObject.getBoolean("typing");
                            String groupId = typingObject.getString("group");
                            final String fUserId = userId;
                            Group currGroup = NetworkManager.getCurrentGroup();
                            Group isTypingGroup = NetworkManager.getAllGroups().get(groupId);
                            if(isTyping){
                                isTypingGroup.addTypingUser(NetworkManager.getUsernameFromId(userId));

                                if( lastTypingEventTime == null ) {
                                    lastTypingEventTime = System.currentTimeMillis();
                                    currGroup = NetworkManager.getCurrentGroup();
                                    //sendStartTyping();
                                    isTypingGroup.addTypingUser(NetworkManager.getUsernameFromId(userId));
                                typingThread = new Thread(new Runnable(){

                                    public void run(){
                                        while(true){
                                            Long timeDifference = System.currentTimeMillis()-lastTypingEventTime;
                                            if(timeDifference>=RESET_TIME){//If the user has stopped typing for 1 seconds. Send stop typing.
                                                //SocketIoController.sendStopTyping(NetworkManager.getCurrentGroup(), fUserId);
                                                SocketIoController.removeTyper(NetworkManager.getCurrentGroup(), fUserId);
                                                lastTypingEventTime = null;
                                                Log.d(TAG, "HERE");
                                                break;
                                            }
                                            try {
                                                Thread.sleep(RESET_TIME-timeDifference+5);
                                            } catch (InterruptedException e) {
                                                //SocketIoController.sendStopTyping(currGroup);
                                                SocketIoController.removeTyper(NetworkManager.getCurrentGroup(), fUserId);
                                                lastTypingEventTime = null;
                                                e.printStackTrace();
                                                break;
                                            }
                                        }
                                    }
                                    });
                                    typingThread.start();
                                }else{
                                    lastTypingEventTime = System.currentTimeMillis();
                                }
                            }
                            else{
                                isTypingGroup.removeTypingUser(NetworkManager.getUsernameFromId(userId));
                            }
                            if(currGroup!=null && groupId.equals(currGroup.getId())){
                                MessageFragment.typingUpdated();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Utils.makeToast(e);
                        }
                    }
                });
                client.setJSONCallback(new JSONCallback() {

                    @Override
                    public void onJSON(JSONObject json, Acknowledge acknowledge) {
                        Log.d(TAG,"onJSON:"+json);

                    }
                });
                SocketIoController.setClient(client);
                client.setDisconnectCallback(new DisconnectCallback(){
                    @Override
                    public void onDisconnect(Exception e) {
                        //Utils.makeToast("Lost connection with the server");
                        GroupsFragment.setUnliveData();
                    }
                });
                client.setReconnectCallback(new ReconnectCallback(){
                    @Override
                    public void onReconnect() {
                        //Utils.makeToast("Reconnected to the server");
                    }
                });
            }
        });
        return clientFuture;
    }

    public static void disconnect(){

        if(clientFuture!=null){
            clientFuture.cancel();
        }
        if(getClient()!=null){
            getClient().disconnect();
        }
    }

    public static void sendMessage(final Message m){
        if(getClient()==null || !getClient().isConnected()){
            Utils.makeToast("Couldn't send message. Try again later");
            MessageFragment.removeMessage(m);
            SocketIoController.connect();
        }else{
            if(m.hasMedia()){
                NetworkManager.postMultimediaMessage(m);
                return;
            }
            getClient().emit("message",m.getSendFormat());
        }

    }

    public static void sendStartTyping(String userId){
        JSONArray array = new JSONArray();
        JSONObject object = new JSONObject();
        try {
            object.put("typing", true);
            object.put("from", userId);
            object.put("group", NetworkManager.getCurrentGroup().getId());
        } catch (JSONException e) {
            Utils.makeToast(e);
            e.printStackTrace();
        }
        array.put(object);
        if(getClient()!=null){
            getClient().emit("typing",array);
        }
    }

    public static void sendStopTyping(Group g, String userId){
        FastChatTextWatcher.resetTextWatcher();
        JSONArray array = new JSONArray();
        JSONObject object = new JSONObject();
        try {
            object.put("typing", false);
            object.put("from", NetworkManager.getCurrentUser().getId());
            object.put("group", g.getId());
        } catch (JSONException e) {
            Utils.makeToast(e);
            e.printStackTrace();
        }
        array.put(object);
        if(getClient()!=null){
            getClient().emit("typing",array);
        }
    }

    public static void removeTyper(Group g, String userId) {
        g.removeTypingUser(NetworkManager.getUsernameFromId(userId));
        MessageFragment.updateUI();
    }

    public static boolean isConnected() {
        if(getClient()==null){
            return false;
        }
        return getClient().isConnected();
    }

    public static SocketIOClient getClient() {
        return client;
    }

    public static void setClient(SocketIOClient client) {
        SocketIoController.client = client;
    }
}
