package com.kaltura.playersdk.types;

import android.renderscript.Script;

/**
 * Created by nissimpardo on 08/02/16.
 */
public class KError {
    private String mDescription;
    private int mCode;
    private String mDomain;

    public KError(String jsonString) {
        
    }

    public String getDescription() {
        return mDescription;
    }

    public int getCode() {
        return mCode;
    }

    public String getDomain() {
        return mDomain;
    }
}
