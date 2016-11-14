package com.kaltura.playersdk.casting;

import com.kaltura.playersdk.interfaces.KCastProvider;

/**
 * Created by nissimpardo on 06/07/16.
 */
public class KCastFactory {
    public static KCastProvider createCastProvider(String logoURL) {
        return new KCastProviderImpl(logoURL);
    }
}
