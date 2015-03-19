package com.kaltura.playersdk.actionHandlers.ShareStrategies;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.kaltura.playersdk.actionHandlers.ShareManager;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nissopa on 3/3/15.
 */
public class SmsShareStrategy implements ShareManager.KPShareStrategy {
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
        intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        Uri data = Uri.parse("sms:?body=" + videoName + "\n" + videoLink);
        intent.setData(data);
        activity.startActivity(intent);
    }
}
