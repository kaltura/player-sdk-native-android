package com.kaltura.playersdk.casting;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;

/**
 * Created by nissimpardo on 07/12/15.
 */
public class KCastKalturaChannel implements Cast.MessageReceivedCallback {
    private String mNameSpace;

    public KCastKalturaChannel(String nameSpace) {
        mNameSpace = nameSpace;
    }

    public String getNamespace() {
        return mNameSpace;
    }

    @Override
    public void onMessageReceived(CastDevice castDevice, String s, String s1) {

    }
}
