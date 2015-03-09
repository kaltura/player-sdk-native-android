package com.kaltura.playersdk.actionHandlers;

import android.app.Activity;

import org.json.JSONObject;

/**
 * Created by nissopa on 2/22/15.
 */
public class ShareManager {

    public static final String TAG = ShareManager.class.getSimpleName();

    public static void share(JSONObject dataSource, Activity activity) {
        KPShareStrategy strategy = ShareStrategyFactory.getStrategy(dataSource);
        if (strategy != null)
        {
            strategy.share(dataSource, activity);
        }

    }

    public interface KPShareStrategy {
        public void share(JSONObject dataSource, Activity activity);
    }

    public static enum SharingType{
        SHARE_FACEBOOK("Facebook"),
        SHARE_TWITTER("Twitter"),
        SHARE_LINKEDIN("Linkedin"),
        SHARE_EMAIL("Email"),
        SHARE_SMS("Sms"),
        SHARE_GOOGLE_PLUS("Googleplus");

        private String label;

        private SharingType(String str) {
            this.label = str;
        }

        public String toString() {
            return this.label;
        }

        public static SharingType fromString(String label) {
            if (label != null) {
                for (SharingType sharingKey : SharingType.values()) {
                    if (label.equalsIgnoreCase(sharingKey.label)) {
                        return sharingKey;
                    }
                }
            }

            return null;
        }

    }

}
