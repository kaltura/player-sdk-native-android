package com.kaltura.playersdk.actionHandlers.ShareStrategies;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.kaltura.playersdk.actionHandlers.ShareManager;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nissopa on 3/2/15.
 */
public class EmailShareStrategy implements ShareManager.KPShareStrategy {
    @Override
    public void share(JSONObject shareParams, Activity activity) {
        String videoLink = "";
        String videoName = "";
        try {
            videoLink = (String)shareParams.get("sharedLink");
            videoName = (String)shareParams.get("videoName");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        Uri data = Uri.parse("mailto:?subject=" + videoName + "&body=" + videoLink);
        intent.setData(data);
        activity.startActivity(intent);
    }
}
