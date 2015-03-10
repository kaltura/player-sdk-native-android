package com.kaltura.playersdk.events;


public abstract class OnErrorListener extends Listener{

    @Override
    final protected void setEventType() {
        mEventType = EventType.ERROR_LISTENER_TYPE;
    }

    @Override
    final  protected boolean executeInternalCallback(InputObject inputObject){
        ErrorInputObject input = (ErrorInputObject)inputObject;
        onError(input.errorCode, input.errorMessage);
        return true;
    }

    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof ErrorInputObject;
    }



    //TODO: change to enums....
	 public static final int ERROR_UNKNOWN = -100;

    /** 
     * Invalid media was given
     */
    public static final int MEDIA_ERROR_NOT_VALID = -101;

    /** File or network related operation errors. */
    public static final int MEDIA_ERROR_IO = -102;
    
    /** Bitstream is not conforming to the related coding standard or file spec. */
    public static final int MEDIA_ERROR_MALFORMED = -103;
    
    /** Bitstream is conforming to the related coding standard or file spec, but
     * the media framework does not support the feature. */
    public static final int MEDIA_ERROR_UNSUPPORTED = -104;
    
    /** Some operation takes too long to complete, usually more than 3-5 seconds. */
    public static final int MEDIA_ERROR_TIMED_OUT = -105;
    
    /** profile is incompatible to hardware */
    public static final int MEDIA_INCOMPATIBLE_PROFILE = -106;

    abstract public void onError(int errorCode, String errorMessage);

    public static class ErrorInputObject extends InputObject{
        public int errorCode;
        public String errorMessage;
    }
}
