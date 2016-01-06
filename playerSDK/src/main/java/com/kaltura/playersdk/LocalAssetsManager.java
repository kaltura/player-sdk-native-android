package com.kaltura.playersdk;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.kaltura.playersdk.widevine.WidevineDrmClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


/**
 * Created by noamt on 04/01/2016.
 */
public class LocalAssetsManager {

    private static final String TAG = "LocalAssetsManager";
    private static final int JSON_BYTE_LIMIT = 1024 * 1024;

    public static boolean registerAsset(@NonNull Context context, @NonNull KPPlayerConfig entry, @Nullable String flavor, @NonNull String localPath) throws IOException {

        // NOTE: this method currently only supports (and assumes) Widevine Classic.

        Uri licenseUri;
        try {
            licenseUri = prepareLicenseUri(entry, flavor, DRMScheme.WidevineClassic);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error", e);
            // delegate to an IOException, because they are the same in this case -- we couldn't reach the server.
            throw new IOException(e);
        }

        WidevineDrmClient widevineDrmClient = new WidevineDrmClient(context);
        widevineDrmClient.acquireLocalAssetRights(localPath, licenseUri.toString());
        return true;
    }
    
    public static boolean checkAssetRights(@NonNull Context context, @NonNull String localPath) {
        return false; // TODO
    }

    private static Uri prepareLicenseUri(@NonNull KPPlayerConfig entry, @Nullable String flavor, @NonNull DRMScheme drmScheme) throws IOException, JSONException {

        // load license data
        Uri getLicenseDataURL = prepareGetLicenseDataURL(entry, drmScheme);
        String licenseData = Utilities.loadStringFromURL(getLicenseDataURL, JSON_BYTE_LIMIT);
        
        // parse license data
        JSONObject licenseUris = new JSONObject(licenseData).getJSONObject("licenseUri");

        if (flavor == null) {
            // select any flavor
            flavor = licenseUris.keys().next();
        }
        
        return Uri.parse(licenseUris.getString(flavor));
    }

    private static Uri prepareGetLicenseDataURL(KPPlayerConfig entry, DRMScheme drmScheme) throws IOException {
        Uri serviceURL = Uri.parse(entry.getDomain());

        // URL may either point to the root of the server or to mwEmbedFrame.php. Resolve this.
        if (serviceURL.getPath().endsWith("/mwEmbedFrame.php")) {
            serviceURL = Utilities.stripLastPathSegment(serviceURL);
        } else {
            serviceURL = resolvePlayerRootURL(serviceURL, entry.getPartnerId(), entry.getUiConfId(), entry.getKS());
        }

        // Now serviceURL is something like "http://cdnapi.kaltura.com/html5/html5lib/v2.38.3".
        
        
        String drmName = null;
        switch (drmScheme) {
            case WidevineCENC:
                drmName = "wvcenc";
                break;
            case WidevineClassic:
                drmName = "wvclassic";
                break;
        }

        // Build service URL
        serviceURL = serviceURL.buildUpon()
                .appendPath("services.php")
                .appendQueryParameter("service", "getLicenseData")
                .appendQueryParameter("ks", entry.getKS())
                .appendQueryParameter("wid", entry.getPartnerId())
                .appendQueryParameter("entry_id", entry.getEntryId())
                .appendQueryParameter("uiconf_id", entry.getUiConfId())
                .appendQueryParameter("drm", drmName)
                .build();
        
        return serviceURL;
    }

    private static Uri resolvePlayerRootURL(Uri serverURL, String partnerId, String uiConfId, String ks) throws IOException {
        // serverURL is something like "http://cdnapi.kaltura.com"; 
        // we need to get to "http://cdnapi.kaltura.com/html5/html5lib/v2.38.3".
        // This is done by loading UIConf data, and looking at "html5Url" property.

        String jsonString = loadUIConf(serverURL, partnerId, uiConfId, ks);
        String embedLoaderUrl;
        try {
            embedLoaderUrl = new JSONObject(jsonString).getString("html5Url");
        } catch (JSONException e) {
            throw new IOException(e);
        }
        Uri serviceUri;
        if (embedLoaderUrl.startsWith("/")) {
            serviceUri = serverURL.buildUpon()
                    .appendEncodedPath(embedLoaderUrl)
                    .build();
        } else {
            serviceUri = Uri.parse(embedLoaderUrl);
        }

        return Utilities.stripLastPathSegment(serviceUri);
    }

    private static String loadUIConf(Uri serverURL, String partnerId, String uiConfId, String ks) throws IOException {
        
        Uri.Builder uriBuilder = serverURL.buildUpon()
                .appendEncodedPath("api_v3/index.php")
                .appendQueryParameter("service", "uiconf")
                .appendQueryParameter("action", "get")
                .appendQueryParameter("format", "1")
                .appendQueryParameter("p", partnerId)
                .appendQueryParameter("id", uiConfId);
        
        if (ks != null) {
            uriBuilder.appendQueryParameter("ks", ks);
        }
        
        return Utilities.loadStringFromURL(uriBuilder.build(), JSON_BYTE_LIMIT);
    }

    enum DRMScheme {
        WidevineClassic, WidevineCENC
    }
}
