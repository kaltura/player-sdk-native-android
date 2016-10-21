package com.kaltura.playersdk;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.kaltura.playersdk.drm.DrmAdapter;
import com.kaltura.playersdk.helpers.CacheManager;
import com.kaltura.playersdk.utils.Utilities;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import static com.kaltura.playersdk.utils.LogUtils.LOGE;
import static com.kaltura.playersdk.utils.LogUtils.LOGI;


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
        void onStatus(String assetPath, long expiryTimeSeconds, long availableTimeSeconds);
    }
    
    public interface AssetRemovalListener {
        void onRemoved(String assetPath);
    }

    public static boolean registerAsset(@NonNull final Context context, @NonNull final KPPlayerConfig entry, @NonNull final String flavor,
                                        @NonNull final String localPath, @Nullable final AssetRegistrationListener listener) {
        
        return registerOrRefreshAsset(context, entry, flavor, localPath, false, listener);
    }
    
    public static boolean refreshAsset(@NonNull final Context context, @NonNull final KPPlayerConfig entry, @NonNull final String flavor,
                                        @NonNull final String localPath, @Nullable final AssetRegistrationListener listener) {


        return registerOrRefreshAsset(context, entry, flavor, localPath, true, listener);

    }
    
    public static boolean unregisterAsset(@NonNull final Context context, @NonNull final KPPlayerConfig entry, 
                                          @NonNull final String localPath, final AssetRemovalListener listener) {

        doInBackground(new Runnable() {
            @Override
            public void run() {
                // Remove cache
                CacheManager cacheManager = null;
                try {
                    cacheManager = getCacheManager(context, entry);
                    cacheManager.removeCachedResponse(Uri.parse(entry.getVideoURL()));

                    DrmAdapter drmAdapter = DrmAdapter.getDrmAdapter(context, localPath);
                    drmAdapter.unregisterAsset(localPath, listener);
                } finally {
                    if (cacheManager != null) {
                        cacheManager.release();
                    }
                }
            }
        });
        return true;
    }

    public static boolean checkAssetStatus(@NonNull final Context context, @NonNull final String localPath,
                                           @Nullable final AssetStatusListener listener) {

        final DrmAdapter drmAdapter = DrmAdapter.getDrmAdapter(context, localPath);

        doInBackground(new Runnable() {
            @Override
            public void run() {
                drmAdapter.checkAssetStatus(localPath, listener);
            }
        });

        return true;
    }

    private static boolean registerOrRefreshAsset(@NonNull final Context context, @NonNull final KPPlayerConfig entry, @NonNull final String flavor,
                                                  @NonNull final String localPath, final boolean refresh, @Nullable final AssetRegistrationListener listener) {

        // Preflight: check that all parameters are valid.
        checkNotNull(entry.getPartnerId(), "entry.partnerId");    // can be an empty string (but not null)
        checkNotEmpty(entry.getServerURL(), "entry.domain");
        checkNotEmpty(entry.getUiConfId(), "entry.uiConfId");
        checkNotEmpty(entry.getEntryId(), "entry.entryId");
        checkNotEmpty(entry.getLocalContentId(), "entry.localContentId");
        checkNotEmpty(localPath, "localPath");


        if (! Utilities.isOnline(context)) {
            LOGI(TAG, "Can't register/refresh when offline");
            return false;
        }

        doInBackground(new Runnable() {
            @Override
            public void run() {

                CacheManager cacheManager = null;
                try {
                    cacheManager = getCacheManager(context, entry);
                    Uri uri = Uri.parse(entry.getVideoURL());
                    if (refresh) {
                        cacheManager.refreshCachedResponse(uri);
                    } else {
                        cacheManager.cacheResponse(uri);
                    }
                    
                    DrmAdapter drmAdapter = DrmAdapter.getDrmAdapter(context, localPath);
                    DrmAdapter.DRMScheme scheme = drmAdapter.getScheme();

                    Uri licenseUri = prepareLicenseUri(entry, flavor, scheme);
                    drmAdapter.registerAsset(localPath, String.valueOf(licenseUri), listener);

                } catch (JSONException | IOException e) {
                    LOGE(TAG, "Error", e);
                    if (listener != null) {
                        listener.onFailed(localPath, e);
                    }
                } finally {
                    if (cacheManager != null) {
                        cacheManager.release();
                    }
                }

            }
        });

        return true;
    }
    
    @NonNull
    private static CacheManager getCacheManager(@NonNull Context context, @NonNull KPPlayerConfig entry) {
        CacheManager cacheManager = new CacheManager(context.getApplicationContext());
        cacheManager.setBaseURL(Utilities.stripLastUriPathSegment(entry.getServerURL()));
        cacheManager.setCacheSize(entry.getCacheSize());
        return cacheManager;
    }


    private static Uri prepareLicenseUri(KPPlayerConfig config, @Nullable String flavor, @NonNull DrmAdapter.DRMScheme drmScheme) throws IOException, JSONException {

        String overrideUrl = config.getConfigValueString("Kaltura.overrideDrmServerURL");
        if (overrideUrl != null) {
            return Uri.parse(overrideUrl);
        }
        
        if (drmScheme == DrmAdapter.DRMScheme.Null) {
            return null;
        }
        
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

    private static Uri prepareGetLicenseDataURL(KPPlayerConfig config, String flavor, DrmAdapter.DRMScheme drmScheme) throws IOException, JSONException {

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

        Uri.Builder builder = serviceURL.buildUpon()
                .appendPath("services.php")
                .encodedQuery(config.getQueryString())
                .appendQueryParameter("service", "getLicenseData")
                .appendQueryParameter("drm", drmName);
        if (flavor != null) {
            builder.appendQueryParameter("flavor_id", flavor);
        }

        return builder.build();
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

    private static void doInBackground(Runnable runnable) {
        new Thread(runnable).start();
    }


}
