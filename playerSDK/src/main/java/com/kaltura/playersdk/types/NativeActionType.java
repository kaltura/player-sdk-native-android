package com.kaltura.playersdk.types;

public enum NativeActionType {
    OPEN_URL("openURL"),
    SHARE("share"),
    SHARE_NETWORK("shareNetwork");
    private String label = "";

    NativeActionType(String label){
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
