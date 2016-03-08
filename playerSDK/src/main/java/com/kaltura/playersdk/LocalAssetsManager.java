package com.kaltura.playersdk;

import android.content.Context;
import android.drm.DrmErrorEvent;
import android.drm.DrmEvent;
import android.drm.DrmInfoEvent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.WebResourceResponse;

import com.kaltura.playersdk.helpers.CacheManager;
import com.kaltura.playersdk.widevine.WidevineDrmClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;


/**
 * Created by noamt on 04/01/2016.
 */
public class LocalAssetsManager {

    private static final String TAG = "LocalAssetsManager";
    private static final int JSON_BYTE_LIMIT = 1024 * 1024;
    
    public interface AssetRegistrationListener {
        void onRegistered(String assetPath);
        void onFailed(String assetPath, Exception error);
    }
    
    public interface AssetStatusListener {
        void onStatus(String assetPath, int expiryTimeSeconds, int availableTimeSeconds);
    }
    
    public interface AssetRemovalListener {
        void onRemoved(String assetPath);
    }
    
    private static void doInBackground(Runnable runnable) {
        new Thread(runnable).start();
    }
    
    public static boolean refreshAsset(@NonNull final Context context, @NonNull final KPPlayerConfig entry, @NonNull final String flavor,
                                        @NonNull final String localPath, @Nullable final AssetRegistrationListener listener) {
        
        // TODO: Refresh
        
        // for now, just re-register.
        return registerAsset(context, entry, flavor, localPath, listener);
    }
    
    public static boolean unregisterAsset(@NonNull final Context context, @NonNull final String localPath, final AssetRemovalListener listener) {

        doInBackground(new Runnable() {
            @Override
            public void run() {
                // Remove cache
                // TODO

                // Remove license
                WidevineDrmClient widevineDrmClient = new WidevineDrmClient(context);
                widevineDrmClient.setEventListener(new WidevineDrmClient.EventListener() {
                    @Override
                    public void onError(DrmErrorEvent event) {
                        Log.d(TAG, event.toString());
                    }

                    @Override
                    public void onEvent(DrmEvent event) {
                        Log.d(TAG, event.toString());
                        switch (event.getType()) {
                            case DrmInfoEvent.TYPE_RIGHTS_REMOVED:
                                if (listener != null) {
                                    listener.onRemoved(localPath);
                                }
                                break;
                        }
                    }
                });
                widevineDrmClient.removeRights(localPath);
                widevineDrmClient.release();
            }
        });
        return true;
    }

    public static boolean checkAssetStatus(@NonNull final Context context, @NonNull final String localPath, 
                                           @Nullable final AssetStatusListener listener) {

        doInBackground(new Runnable() {
            @Override
            public void run() {
                WidevineDrmClient widevineDrmClient = new WidevineDrmClient(context);
                WidevineDrmClient.RightsInfo info = widevineDrmClient.getRightsInfo(localPath);
                if (listener != null) {
                    listener.onStatus(localPath, info.expiryTime, info.availableTime);
                }
                widevineDrmClient.release();
            }
        });
        
        return true;
    }


    public static boolean registerAsset(@NonNull final Context context, @NonNull final KPPlayerConfig entry, @NonNull final String flavor,
                                        @NonNull final String localPath, @Nullable final AssetRegistrationListener listener) {

        // NOTE: this method currently only supports (and assumes) Widevine Classic.

        // Preflight: check that all parameters are valid.
        checkNotNull(entry.getPartnerId(), "entry.partnerId");    // can be an empty string (but not null)
        checkNotEmpty(entry.getServerURL(), "entry.domain");
        checkNotEmpty(entry.getUiConfId(), "entry.uiConfId");
        checkNotEmpty(entry.getEntryId(), "entry.entryId");
        checkNotEmpty(flavor, "flavor");
        checkNotEmpty(localPath, "localPath");
        
        
        final DRMScheme drmScheme = DRMScheme.WidevineClassic;

        doInBackground(new Runnable() {
            @Override
            public void run() {
                CacheManager cacheManager = CacheManager.getInstance();
                cacheManager.setContext(context);
                cacheManager.setBaseURL(Utilities.stripLastUriPathSegment(entry.getServerURL()));
                cacheManager.setCacheSize(entry.getCacheSize());
                try {
                    WebResourceResponse resp = cacheManager.getResponse(Uri.parse(entry.getVideoURL()), Collections.<String, String>emptyMap(), "GET");
                    // TODO: avoid this redundant object creation.
                    InputStream inputStream = resp.getData();
                    Utilities.fullyReadInputStream(inputStream, 10*1024*1024);
                    inputStream.close();
                } catch (IOException e) {
                    if (listener != null) {
                        listener.onFailed(localPath, e);
                    }
                    return;
                }
                if (!Uri.parse(localPath).getPath().endsWith(".wvm")) {
                    if (listener != null) {
                        listener.onRegistered(localPath);
                    }
                    return;
                }
                
                try {
                    Uri licenseUri = prepareLicenseUri(entry, flavor, drmScheme);
                    registerWidevineClassicAsset(context, localPath, licenseUri, listener);
                } catch (JSONException | IOException e) {
                    Log.e(TAG, "Error", e);
                    if (listener != null) {
                        listener.onFailed(localPath, e);
                    }
                }
            }
        });

        return true;
    }
    
    private static void registerWidevineClassicAsset(@NonNull Context context, @NonNull final String localPath, Uri licenseUri, @Nullable final AssetRegistrationListener listener) {

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
        widevineDrmClient.acquireLocalAssetRights(localPath, licenseUri.toString());
        widevineDrmClient.release();
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
            serviceURL = Utilities.stripLastUriPathSegment(serviceURL);
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

        return Utilities.stripLastUriPathSegment(serviceUri);
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

    private static void checkArg(boolean invalid, String message) {
        if (invalid) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void checkNotNull(Object obj, String name) {
        checkArg(obj == null, name + " must not be null");
    }

    private static void checkNotEmpty(String obj, String name) {
        checkArg(obj == null || obj.length() == 0, name + " must not be empty");
    }


    enum DRMScheme {
        WidevineClassic, WidevineCENC
    }
}
