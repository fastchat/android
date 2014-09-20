package com.fastchat.fastchat.ui;

import java.util.ArrayList;

import com.fastchat.fastchat.R;
import com.fastchat.fastchat.models.Message;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.BaseAdapter;
import android.widget.TextView;

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
        if(position > mMessages.size()){
            return null;
        }
        return mMessages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Message message = (Message) this.getItem(position);

        View cell = convertView;
        if (cell == null) {
            cell = LayoutInflater.from(mContext).inflate(message.isMine() ? R.layout.message_row_me : R.layout.message_row_other, parent, false);
        }
        ((TextView)cell.findViewById(R.id.message_text)).setText(message.getText());
        ((TextView)cell.findViewById(R.id.message_time_stamp)).setText(message.getDateString());
        ((TextView)cell.findViewById(R.id.user_name)).setText(message.getFrom().getUsername());

        Bitmap avatar = message.getFrom().getAvatarBitmap();
        if (avatar != null && !avatar.isRecycled()) {
            ((ImageView)cell.findViewById(R.id.avatar)).setImageBitmap(avatar);
        } else {
            ((ImageView)cell.findViewById(R.id.avatar)).setImageBitmap(ProfileFragment.getDefaultBitmap());
        }

        return cell;
    }
}
