package com.kaltura.playersdk;

import android.content.Context;
import android.drm.DrmErrorEvent;
import android.drm.DrmEvent;
import android.drm.DrmInfoEvent;
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
    
    public interface AssetEventListener {
        void onRegistered(String assetPath);
        void onFailed(String assetPath, Exception error);
        void onStatus(String assetPath, Object status);
    }
    
    private static void checkArg(boolean invalid, String message) {
        if (invalid) {
            throw new IllegalArgumentException(message);
        }
    }
    
    private static Object checkNotNull(Object obj, String name) {
        checkArg(obj == null, name + " must not be null");

        return obj;
    }

    private static String checkNotNullOrEmpty(String obj, String name) {
        checkNotNull(obj, name);
        checkNotEmpty(obj, name);
        
        return obj;
    }

    private static String checkNotEmpty(String obj, String name) {
        checkArg(obj != null && obj.length() == 0, name + " must not be null");

        return obj;
    }

    public static boolean registerAsset(@NonNull final Context context, @NonNull final KPPlayerConfig entry, @Nullable final String flavor,
                                        @NonNull final String localPath, @Nullable final AssetEventListener listener) {

        // NOTE: this method currently only supports (and assumes) Widevine Classic.

        // Preflight: check that all parameters are valid.
        checkNotNullOrEmpty(entry.getServerURL(), "entry.domain");
        checkNotNullOrEmpty(entry.getUiConfId(), "entry.uiConfId");
        checkNotNullOrEmpty(entry.getEntryId(), "entry.entryId");
        checkNotNullOrEmpty(flavor, "flavor");
        checkNotNullOrEmpty(localPath, "localPath");
        checkNotNull(entry.getPartnerId(), "partnerId");    // can be an empty string
        checkNotNull(context, "context");
        
        
        final DRMScheme drmScheme = DRMScheme.WidevineClassic;

        new Thread() {
            @Override
            public void run() {
                Uri licenseUri;
                try {
                    licenseUri = prepareLicenseUri(entry, flavor, drmScheme);
                    registerWidevineClassicAsset(context, localPath, licenseUri, listener);
                } catch (JSONException e) {
                    Log.e(TAG, "JSON error", e);
                    if (listener != null) {
                        listener.onFailed(localPath, e);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "IO error", e);
                    if (listener != null) {
                        listener.onFailed(localPath, e);
                    }
                }
            }
        }.start();
        

        return true;
    }
    
    private static void registerWidevineClassicAsset(@NonNull Context context, @NonNull final String localPath, Uri licenseUri, @Nullable final AssetEventListener listener) {
        WidevineDrmClient widevineDrmClient = new WidevineDrmClient(context);
        widevineDrmClient.setEventListener(new WidevineDrmClient.EventListener() {
            @Override
            public void onError(DrmErrorEvent event) {
                Log.d(TAG, event.toString());

                if (listener != null) {
                    listener.onFailed(localPath, new Exception("License acquisition failed; DRM client error code: " + event.getType()));
                }
            }

            @Override
            public void onEvent(DrmEvent event) {
                Log.d(TAG, event.toString());
                switch (event.getType()) {
                    case DrmInfoEvent.TYPE_RIGHTS_INSTALLED:
                        if (listener != null) {
                            listener.onRegistered(localPath);
                        }
                        break;
                }
            }
        });
        widevineDrmClient.checkRightsStatus(localPath);
        widevineDrmClient.acquireLocalAssetRights(localPath, licenseUri.toString());
        widevineDrmClient.checkRightsStatus(localPath);
    }

    public static boolean checkAssetRights(@NonNull Context context, @NonNull String localPath) {
        return false; // TODO
    }

    private static Uri prepareLicenseUri(KPPlayerConfig config, @Nullable String flavor, @NonNull DRMScheme drmScheme) throws IOException, JSONException {

        // load license data
        Uri getLicenseDataURL = prepareGetLicenseDataURL(config, flavor, drmScheme);
        String licenseData = Utilities.loadStringFromURL(getLicenseDataURL, JSON_BYTE_LIMIT);
        
        // parse license data
        JSONObject licenseDataJSON = new JSONObject(licenseData);
        if (licenseDataJSON.has("error")) {
            throw new IOException("Error getting license data: " + licenseDataJSON.getJSONObject("error").getString("message"));
        }
        
        String licenseUri = licenseDataJSON.getString("licenseUri");
        
        return Uri.parse(licenseUri);
    }

    private static Uri prepareGetLicenseDataURL(KPPlayerConfig config, String flavor, DRMScheme drmScheme) throws IOException, JSONException {

        Uri serviceURL = Uri.parse(config.getServerURL());
        // URL may either point to the root of the server or to mwEmbedFrame.php. Resolve this.
        if (serviceURL.getPath().endsWith("/mwEmbedFrame.php")) {
            serviceURL = Utilities.stripLastPathSegment(serviceURL);
        } else {
            serviceURL = resolvePlayerRootURL(serviceURL, config.getPartnerId(), config.getUiConfId(), config.getKS());
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

        return serviceURL.buildUpon()
                .appendPath("services.php")
                .encodedQuery(config.getQueryString())
                .appendQueryParameter("service", "getLicenseData")
                .appendQueryParameter("drm", drmName)
                .appendQueryParameter("flavor_id", flavor).build();
    }

    private static Uri resolvePlayerRootURL(Uri serverURL, String partnerId, String uiConfId, String ks) throws IOException, JSONException {
        // serverURL is something like "http://cdnapi.kaltura.com"; 
        // we need to get to "http://cdnapi.kaltura.com/html5/html5lib/v2.38.3".
        // This is done by loading UIConf data, and looking at "html5Url" property.

        String jsonString = loadUIConf(serverURL, partnerId, uiConfId, ks);
        String embedLoaderUrl;
        JSONObject uiConfJSON = new JSONObject(jsonString);
        if (uiConfJSON.has("message")) {
            throw new IOException("Error getting UIConf: " + uiConfJSON.getString("message"));
        }
        embedLoaderUrl = uiConfJSON.getString("html5Url");
        
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
