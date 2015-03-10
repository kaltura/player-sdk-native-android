package com.kaltura.playersdk.events;

import com.kaltura.playersdk.actionHandlers.ShareManager;

/**
 * Created by itayi on 3/9/15.
 */
public abstract class OnShareListener extends Listener{

    @Override
    final protected void setEventType() {
        mEventType = EventType.SHARE_LISTENER_TYPE;
    }

    @Override
    final  protected boolean executeInternalCallback(InputObject inputObject){
        ShareInputObject input = (ShareInputObject) inputObject;
        return onShare(input.videoUrl, input.sharingType, input.videoName);
    }

    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof ShareInputObject;
    }

    abstract public boolean onShare(String videoUrl, ShareManager.SharingType sharingType, String videoName);

    public static class ShareInputObject extends InputObject{
        public String videoUrl;
        public String videoName;
        public ShareManager.SharingType sharingType;
    }
}
