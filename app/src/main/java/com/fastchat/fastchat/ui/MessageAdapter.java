package com.fastchat.fastchat.ui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import com.fastchat.fastchat.MainActivity;
import com.fastchat.fastchat.R;
import com.fastchat.fastchat.Utils;
import com.fastchat.fastchat.models.Message;
import com.fastchat.fastchat.models.MultiMedia;
import com.fastchat.fastchat.networking.NetworkManager;
import com.koushikdutta.async.future.Future;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.json.JSONObject;

import static android.support.v4.app.ActivityCompat.startActivity;

public class MessageAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<Message> mMessages;
    private static final int MARGINS = 30;

    private static final String TAG=MessageAdapter.class.getName();

    private static final OnClickListener ocl = new OnClickListener(){

        @Override
        public void onClick(View arg0) {
            Message message = (Message) arg0.getTag();
//            MultiMedia mms = message.getMedia();
//            if(mms==null){
//                ((Button) arg0).setText("Downloading...");
//                ((Button) arg0).setEnabled(false);
//                NetworkManager.getMessageMedia(message);
//                return;
//            }
//
//            Intent intent = new Intent();
//            intent.setAction(android.content.Intent.ACTION_VIEW);
//            intent.setDataAndType(Uri.fromFile(mms.getData()),mms.getMimeType());
//            try{
//                MainActivity.activity.startActivityForResult(intent, 10);
//            }catch(ActivityNotFoundException e){
//                Log.d(TAG,"No Activity found to handle intent");
//            }
        }

    };


    public MessageAdapter(Context context, ArrayList<Message> messages) {
        super();
        this.mContext = context;
        this.mMessages = messages;
    }

    @Override
    public int getCount() {
      return mMessages.size();
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType (int position) {
        Message message = (Message) this.getItem(position);
        return message.isMine() ? 0 : 1;
    }

    @Override
    public Object getItem(int position) {
//        if(position > mMessages.size()){
//            return null;
//        }
        return mMessages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Message message = (Message) this.getItem(position);

        ViewHolder holder;

        if( convertView == null ) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(message.isMine() ? R.layout.message_row_me : R.layout.message_row_other, parent, false);
            holder.message = (TextView) convertView.findViewById(R.id.message_text);
            holder.image = (ImageView) convertView.findViewById(R.id.message_media);

            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        View cell = convertView;
        if (cell == null) {
            cell = LayoutInflater.from(mContext).inflate(message.isMine() ? R.layout.message_row_me : R.layout.message_row_other, parent, false);
        }

        holder.multiMedia = (ImageView) convertView.findViewById(R.id.message_media);
        holder.multiMedia.setImageDrawable(null);
        holder.multiMedia.setVisibility(View.GONE);

        ((TextView)cell.findViewById(R.id.message_text)).setText(message.getText());
        ((TextView)cell.findViewById(R.id.message_time_stamp)).setText(message.getDateString());
        ((TextView)cell.findViewById(R.id.user_name)).setText(message.getFrom().getUsername());

        Bitmap avatar = message.getFrom().getAvatarBitmap();
        if (avatar != null && !avatar.isRecycled()) {
            ((ImageView)cell.findViewById(R.id.avatar)).setImageBitmap(avatar);
        } else {
            ((ImageView)cell.findViewById(R.id.avatar)).setImageBitmap(ProfileFragment.getDefaultBitmap());
        }

        if (message.hasMedia()) {
            //Log.d(TAG, "This message has media! " + message.getMedia());

            MultiMedia mms = message.getMedia();
            Log.d(TAG, "mms: " + mms);
            if( mms != null && mms.isImage() ) {
//                Log.d(TAG, "Setting image view " + message.getMedia().getBitmap(50));
                holder.multiMedia.setVisibility(View.VISIBLE);
                holder.message.setText("TEXT TO MAKE VIEW MAXIMUM LENGTH");
                convertView.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                int width = convertView.getMeasuredWidth();
                holder.message.setText("");
                holder.multiMedia.setImageBitmap(mms.getBitmap(width));

                ImageView v = (ImageView) holder.multiMedia;
                Bitmap bm=((BitmapDrawable)v.getDrawable()).getBitmap();
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                if( byteStream != null ) {
                   // bm.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
                   // File f = Utils.saveToInternalStorage(byteStream.toByteArray());
                   // Uri contentUri = Uri.fromFile(f);

                   // holder.multiMedia.setOnClickListener(new ImageViewerOnClickListener(contentUri, this.mContext));
                    Log.d(TAG, "byteStream " + byteStream);
                }
//
//                    @Override
//                    public void onClick(View arg0) {
////                        Intent intent = new Intent();
////                        intent.setAction(android.content.Intent.ACTION_VIEW);
////
////                        intent.setDataAndType(Uri.parse());
//
//
//                        //mediaScanIntent.setData(contentUri);
//
////                        Intent intent = new Intent();
////                        intent.setAction(Intent.ACTION_VIEW);
////                        intent.setDataAndType(Uri.parse(uri), "image/*");
////                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                        // Activity.getApplicationContenxt().startActivity(intent);
//
////                        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
////                        ImageView v = (ImageView) arg0;
////                        Bitmap bm=((BitmapDrawable)v.getDrawable()).getBitmap();
////                        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
////                        bm.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
////                        File f = Utils.saveToInternalStorage(byteStream.toByteArray());
////                        Uri contentUri = Uri.fromFile(f);
////                        mediaScanIntent.setData(contentUri);
////                        MainActivity.activity.sendBroadcast(mediaScanIntent);
////                        Log.d(TAG, "Sending image to Gallery: "+contentUri.toString());
////                        Utils.makeToast("Saved Image to Gallery");
//                    }
//                });
            }
            else {

            }
        }

        else {
            ((ImageView)cell.findViewById(R.id.message_media)).setImageBitmap(null);
        }
        return cell;
    }

    private static class ViewHolder
    {
        public ImageView multiMedia;
        public LinearLayout layout;
        public ImageView image;
        TextView message;
    }
}
