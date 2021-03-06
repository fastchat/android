package com.fastchat.fastchat.ui;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fastchat.fastchat.MainActivity;
import com.fastchat.fastchat.R;
import com.fastchat.fastchat.Utils;
import com.fastchat.fastchat.networking.NetworkManager;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.fastchat.fastchat.vendor.ChipsAdapter;
import com.fastchat.fastchat.vendor.ChipsItem;
import com.fastchat.fastchat.vendor.ChipsMultiAutoCompleteTextview;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class NewGroupFragment extends Fragment implements OnClickListener {

    private View rootView;

    private static final String TAG=NewGroupFragment.class.getName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        MainActivity.activity.getActionBar().setTitle("New Group");
        rootView = inflater.inflate(R.layout.new_group, container,
                false);
        ArrayList<ChipsItem> arrUsernames = new ArrayList<ChipsItem>();
        ChipsMultiAutoCompleteTextview ch = (ChipsMultiAutoCompleteTextview) rootView.findViewById(R.id.invite_users);
        ChipsAdapter chipsAdapter = new ChipsAdapter(MainActivity.activity.getApplicationContext(), arrUsernames);
        ch.setAdapter(chipsAdapter);
        Button button = (Button) rootView.findViewById(R.id.button_send_invite);
        button.setOnClickListener(this);
        return rootView;
    }

    public void onStart(){
        Tracker t = MainActivity.tracker;
        t.setScreenName("New Group View");

        // Send a screen view.
        t.send(new HitBuilders.AppViewBuilder().build());
        super.onStart();
    }

    @Override
    public void onClick(View arg0) {
        EditText groupName = (EditText) rootView.findViewById(R.id.group_name);
        ChipsMultiAutoCompleteTextview ch = (ChipsMultiAutoCompleteTextview) rootView.findViewById(R.id.invite_users);
        EditText messageBox = (EditText) rootView.findViewById(R.id.invite_message);
        List<String> users = Arrays.asList(ch.getText().toString().split(" "));
        if(groupName.length()==0 || messageBox.length()==0 || users.size()==0){
            Utils.makeToast("Please fill out all of the information");
            return;
        }

        NetworkManager.postCreateGroup(users, groupName.getText().toString(), messageBox.getText().toString());
    }
}
