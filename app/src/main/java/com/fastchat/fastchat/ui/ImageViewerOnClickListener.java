package com.fastchat.fastchat.ui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;

import android.view.View;

import com.fastchat.fastchat.MainActivity;

import static android.support.v4.app.ActivityCompat.startActivity;

/**
 * Created by Michael on 9/28/2014.
 */
public class ImageViewerOnClickListener implements View.OnClickListener {

    Uri uri;
    Context theContext;
    public ImageViewerOnClickListener(Uri mediaURI, Context context) {
        this.uri = mediaURI;
        this.theContext = context;
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "image/*");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        theContext.getApplicationContext().startActivity(intent);
        //MainActivity.startActionWithIntent(intent);
    }
};
