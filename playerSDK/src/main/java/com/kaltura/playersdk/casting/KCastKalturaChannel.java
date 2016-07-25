package com.kaltura.playersdk.casting;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;
import static com.kaltura.playersdk.utils.LogUtils.LOGE;

/**
 * Created by nissimpardo on 07/12/15.
 */
public class KCastKalturaChannel implements Cast.MessageReceivedCallback {
    private String mNameSpace;
    private KCastKalturaChannelListener mListener;

    public interface KCastKalturaChannelListener {
        void readyForMedia();
    }

    public KCastKalturaChannel(String nameSpace, KCastKalturaChannelListener listener) {
        mNameSpace = nameSpace;
        mListener = listener;
    }

    public String getNamespace() {
        return mNameSpace;
    }

    @Override
    public void onMessageReceived(CastDevice castDevice, String s, String s1) {
        LOGD(getClass().getSimpleName(), s + " " + s1);
        if (s1.equals("readyForMedia")) {
            mListener.readyForMedia();
        }
    }
}
