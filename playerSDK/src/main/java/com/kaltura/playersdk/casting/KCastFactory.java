package com.kaltura.playersdk.casting;

import android.content.Context;

import com.kaltura.playersdk.interfaces.KCastProvider;

/**
 * Created by nissimpardo on 06/07/16.
 */
public class KCastFactory {
    public static KCastProvider createCastProvider(Context context, String castAppId, String logoUrl) {
        return new KCastProviderV3Impl(context, castAppId, logoUrl);
    }
}
