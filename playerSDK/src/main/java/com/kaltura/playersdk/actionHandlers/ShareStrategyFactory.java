package com.kaltura.playersdk.actionHandlers;

import android.util.Log;

import com.kaltura.playersdk.actionHandlers.ShareStrategies.EmailShareStrategy;
import com.kaltura.playersdk.actionHandlers.ShareStrategies.FacebookShareStrategy;
import com.kaltura.playersdk.actionHandlers.ShareStrategies.GooglePlusShareStrategy;
import com.kaltura.playersdk.actionHandlers.ShareStrategies.LinkedinShareStrategy;
import com.kaltura.playersdk.actionHandlers.ShareStrategies.SmsShareStrategy;
import com.kaltura.playersdk.actionHandlers.ShareStrategies.TwitterShareStrategy;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by itayi on 3/5/15.
 */
public class ShareStrategyFactory {

    public static final String TAG = ShareStrategyFactory.class.getSimpleName();


    public static ShareManager.KPShareStrategy getStrategy(JSONObject dataSource){
        String strategyName;
        try {
            strategyName = dataSource.getString("id");
        } catch (JSONException e) {
            Log.w(TAG, "Error parsing Json object");
            return null;
        }

        switch(strategyName){
            case "facebook":
                return new FacebookShareStrategy();

            case "email":
                return new EmailShareStrategy();

            case "twitter":
                return new TwitterShareStrategy();

            case "googleplus":
                return new GooglePlusShareStrategy();

            case "linkedin":
                return new LinkedinShareStrategy();

            case "sms":
                return new SmsShareStrategy();
        }

        Log.w(TAG, "couldn't find a strategy for " + strategyName);
        return null;
    }
}
