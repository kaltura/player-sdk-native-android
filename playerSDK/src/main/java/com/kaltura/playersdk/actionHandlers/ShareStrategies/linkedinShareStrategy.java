package com.kaltura.playersdk.actionHandlers.ShareStrategies;

import android.app.Activity;
import android.content.Intent;

import com.kaltura.playersdk.Utilities;

import org.json.JSONObject;

/**
 * Created by nissopa on 3/3/15.
 */
public class LinkedinShareStrategy extends WebShareStrategy {
    @Override
    public void share(JSONObject shareParams, Activity activity) {
        if(Utilities.doesPackageExist("com.linkedin.android", activity))
        {
            String shareText = "";//TODO: parse shareParams
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setClassName("com.linkedin.android",
                    "com.linkedin.android.home.UpdateStatusActivity");
            shareIntent.setType("text/*");
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareText);
            activity.startActivity(shareIntent);
        }else{
           super.share(shareParams, activity);
        }

    }
}
