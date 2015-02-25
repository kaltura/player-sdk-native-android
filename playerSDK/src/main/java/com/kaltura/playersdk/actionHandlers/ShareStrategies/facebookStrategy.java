package com.kaltura.playersdk.actionHandlers.ShareStrategies;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.kaltura.playersdk.BrowserActivity;
import com.kaltura.playersdk.actionHandlers.ShareManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by nissopa on 2/24/15.
 */
public class facebookStrategy implements ShareManager.KPShareStrategy {

    @Override
    public void share(JSONObject shareParams, Activity activity, ShareManager.KPShareCompletionBlock completionBlock) {
        Intent fbIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://publish/?text=www.domain.com"));
        activity.startActivity(fbIntent);
//        Intent intent = new Intent(activity, BrowserActivity.class);
//        try {
//            String shareURL = (String)((JSONObject)shareParams.get("shareNetwork")).get("template");
////            ArrayList<String> redirectURIs = (ArrayList<String>)shareParams.get("");
//            intent.putExtra("ShareURL", shareURL);
//            activity.startActivity(intent);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }


    }
}

