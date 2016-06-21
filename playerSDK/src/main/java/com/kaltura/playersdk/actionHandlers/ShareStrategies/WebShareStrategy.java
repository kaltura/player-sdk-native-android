package com.kaltura.playersdk.actionHandlers.ShareStrategies;

import android.app.Activity;
import android.content.Intent;

import com.kaltura.playersdk.R;
import com.kaltura.playersdk.BrowserActivity;
import com.kaltura.playersdk.actionHandlers.ShareManager;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by itayi on 3/5/15.
 */
public class WebShareStrategy implements ShareManager.KPShareStrategy {
    @Override
    public void share(JSONObject shareParams, Activity activity) {
        String sharePrefix = "";
        String videoLink = "";
        String videoName = "";
        String barColor = "";
        try {
            sharePrefix = ((String)((JSONObject)shareParams.get("shareNetwork")).get("template")).replace("{share.shareURL}", "");
            videoLink = (String)shareParams.get("sharedLink");
            videoName = (String)shareParams.get("videoName");
            barColor = (String)((JSONObject)shareParams.get("shareNetwork")).get("barColor");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String shareScheme = sharePrefix + videoLink;
        Intent shareIntent = new Intent(activity, BrowserActivity.class);
        shareIntent.putExtra("ShareLink", shareScheme);
        shareIntent.putExtra("VideoName", videoName);
        shareIntent.putExtra("BarColor", barColor);

        activity.startActivity(shareIntent);
        activity.overridePendingTransition(R.animator.trans_left_in, R.animator.trans_left_out);
    }
}
