package com.kaltura.playersdk.casting;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;

/**
 * Created by nissimpardo on 07/12/15.
 */
public class KCastKalturaChannel implements Cast.MessageReceivedCallback {
    private static final String TAG = "KCastKalturaChannel";
    private String mNameSpace;
    private KCastKalturaChannelListener mListener;

    public interface KCastKalturaChannelListener {
        void readyForMedia(String[] castParams);
        void ccOnSenderConnected(int numOfSendersConnected);
        void ccOnSenderDisconnected(int numOfSendersConnected);
        void ccUpdateAdDuration(int adDuration);
        void ccUserInitiatedPlay();
        void ccReceiverAdOpen();
        void ccReceiverAdComplete();
        void textTeacksRecived(HashMap<String,Integer> textTrackHash);
        void videoTracksReceived(List<Integer> videoTracksList);
        void onCastReceiverError(String errorMsg, int errorCode);
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
        LOGD(TAG, "onMessageReceived <" +  s + "> <" + s1 + ">");
        if (s1.startsWith("readyForMedia")) {
            String[] params = s1.split("\\|");
            if (params.length == 3) {
                mListener.readyForMedia(Arrays.copyOfRange(params, 1, 3));
            }
        }
        else if (s1.contains("userInitiatedPlay")){
            LOGD(TAG, "onMessageReceived userInitiatedPlay");
            //mListener.ccUserInitiatedPlay();
        }
        else if (s1.startsWith("chromecastReceiverAdDuration")) {
            String[] params = s1.split("\\|");
            if (params.length == 2) {
                if (params[1] != null) {
                    LOGD(TAG, "onMessageReceived chromecastReceiverAdDuration = " + params[1]);
                    //mListener.ccUpdateAdDuration(Integer.valueOf(params[1]));
                }
            }
        }
        else if (s1.contains("chromecastReceiverAdOpen")){
            LOGD(TAG, "onMessageReceived chromecastReceiverAdOpen");
            mListener.ccReceiverAdOpen();
        }
        else if (s1.contains("chromecastReceiverAdComplete")){
            LOGD(TAG, "onMessageReceived chromecastReceiverAdComplete");
            mListener.ccReceiverAdComplete();
        }
        else if (s1.startsWith("chromecastReceiverOnSenderConnected")) {
            String[] params = s1.split("\\|");
            if (params.length == 2) {
                if (params[1] != null) {
                    LOGD(TAG, "onMessageReceived chromecastReceiverOnSenderConnected = " + params[1]);
                    mListener.ccOnSenderConnected(Integer.valueOf(params[1]));
                }
            }
        }
        else if (s1.startsWith("chromecastReceiverOnSenderDisconnected")) {
            String[] params = s1.split("\\|");
            if (params.length == 2) {
                if (params[1] != null) {
                    LOGD(TAG, "onMessageReceived chromecastReceiverOnSenderDisconnected = " + params[1]);
                    mListener.ccOnSenderDisconnected(Integer.valueOf(params[1]));
                }
            }
        }
        else if (s1.contains("\"Data Loaded\"")){
            //mListener.dataLoaded();
        }
        else if (s1.contains("\"aborted\"")){
            //mListener.dataAborted();
        } else if (s1.toLowerCase().contains("\"error\"")){
            List<String> errMessage = parseErrormessage(s1);
            mListener.onCastReceiverError(errMessage.get(0),Integer.valueOf(errMessage.get(1)));
        } else if (s1.contains("\"captions\"")){
            mListener.textTeacksRecived(parseCaptions(s1));
        } else if (s1.contains("\"video_bitrates\"")) {
            mListener.videoTracksReceived(parseVideoBitrates(s1));
        }
    }

    public HashMap<String,Integer> parseCaptions(String captions) {
        //{"captions":{"1":"eng","2":"fra","3":"spa","4":"ara","5":"nld","6":"jpn","7":"rus"}
        HashMap<String,Integer> captionsHash = new HashMap<String,Integer>();
        captions = captions.replaceAll("\\{", "").replaceAll("\\}", "").replaceAll("\"", "").replaceAll("captions:", "");
        String [] elements = captions.split(",");
        for (String elm : elements) {
            String [] keyVal = elm.split(":");
            captionsHash.put(keyVal[1],Integer.valueOf(keyVal[0]));
        }
        return captionsHash;
    }

    public List<Integer> parseVideoBitrates(String videoBitrates) {
        //{"video_bitrates":[409600,814080,1536000]}
        List<Integer> videoBitratesList = new ArrayList<Integer>();
        videoBitrates = videoBitrates.replaceAll("\\{", "").replaceAll("\\}", "").replaceAll("\"", "").replaceAll("video_bitrates:", "");
        videoBitrates = videoBitrates.replaceAll("\\[", "").replaceAll("\\]", "");
        String [] elements = videoBitrates.split(",");
        for (String elm : elements) {

            videoBitratesList.add(Integer.valueOf(elm));
        }
        return videoBitratesList;
    }

    public List<String> parseErrormessage(String errorMsg) {
        List<String> errorParams = new ArrayList<>();
        //onMessageReceived <urn:x-cast:com.kaltura.cast.player> <mediaHostState: Fatal Error: code = 3>
        errorMsg = errorMsg.replaceAll(" ", "");
        if (errorMsg.contains(":") && errorMsg.contains("code=")) {
            String[] erorrParts = errorMsg.split(":");
            if (erorrParts.length != 2) {
                errorParams.add(erorrParts[0]);
                String[] codeParts = erorrParts[1].split("=");
                errorParams.add(codeParts[1]);
            }
            return errorParams;
        }

        errorParams.add(errorMsg);
        errorParams.add("-1");
        return errorParams;
    }
}
