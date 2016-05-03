package com.kaltura.playersdk;

import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Created by noamt on 19/04/2016.
 */
public class ImpossibleException extends RuntimeException {
    public ImpossibleException(String whyNotPossible) {
        this(whyNotPossible, null);
    }

    public ImpossibleException(@NonNull String whyNotPossible, Throwable throwable) {
        super("Impossible exception: " + whyNotPossible, throwable);
        Log.e("Impossible", whyNotPossible, throwable);
    }
}
