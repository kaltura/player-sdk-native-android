package com.kaltura.playersdk.actionHandlers;

import android.app.Activity;
import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;
import com.kaltura.playersdk.actionHandlers.ShareStrategies.*;

import static java.lang.Class.forName;

/**
 * Created by nissopa on 2/22/15.
 */
public class ShareManager {
    private JSONObject datasource;
    private KPShareStrategy strategy;
    private Activity presentingActivity;

    public ShareManager(JSONObject datasource, Activity activity) {
        this.datasource = datasource;
        this.presentingActivity = activity;
    }

    public void shareWithCompletion(KPShareCompletionBlock completionBlock) {
        try {
            String className = (String)datasource.get("id");
            if (className != null && className.length() > 0) {
                try {
                    strategy = (KPShareStrategy)Class.forName("com.kaltura.playersdk.actionHandlers.ShareStrategies." + className + "Strategy").newInstance();
                    strategy.share(this.datasource, this.presentingActivity,completionBlock);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    public enum KPShareResult {
        Success,
        Failed,
        Cancel
    }

    public interface KPShareCompletionBlock {
        void completion(KPShareResult result);
    }

    public interface KPShareStrategy {
        public void share(JSONObject shareParams, Activity activity,KPShareCompletionBlock completionBlock);
    }


}
