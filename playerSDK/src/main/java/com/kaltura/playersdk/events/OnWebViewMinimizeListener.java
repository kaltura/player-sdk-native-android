package com.kaltura.playersdk.events;

public abstract class OnWebViewMinimizeListener extends Listener{
    @Override
    final protected void setEventType() {
        mEventType = EventType.WEB_VIEW_MINIMIZE_LISTENER_TYPE;
    }

    @Override
    final protected boolean executeInternalCallback(InputObject inputObject){
        WebViewMinimizeInputObject input = (WebViewMinimizeInputObject)inputObject;
        setMinimize(input.minimize);
        return true;
    }

    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof WebViewMinimizeInputObject;
    }



    /**
	 * @param minimize: true will minimize webview, false will maximize
	 */
    abstract public void setMinimize( boolean minimize );

    public static class WebViewMinimizeInputObject extends InputObject{
        public boolean minimize;
    }
}
