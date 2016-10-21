package com.kaltura.playersdk.helpers;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.webkit.WebResourceResponse;

import com.kaltura.playersdk.utils.Utilities;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.kaltura.playersdk.helpers.KStringUtilities.md5;
import static com.kaltura.playersdk.utils.LogUtils.LOGD;
import static com.kaltura.playersdk.utils.LogUtils.LOGE;


/**
 * Created by nissimpardo on 25/10/15.
 */
public class CacheManager {
    private static final String TAG = "CacheManager";
    public static final String CACHED_STRINGS_JSON = "CachedStrings.json";
    private JSONObject mCacheConditions;
    private CacheSQLHelper mSQLHelper;
    private String mBaseURL;
    private float mCacheSize = 0;
    private String mCachePath;
    private File mFilesDir;
    private Context mAppContext;
    private List<Pattern> mIncludePatterns;


    private void logCacheHit(Uri url, String fileId) {
        LOGD(TAG, "CACHE HIT: " + fileId + " : " + url);
    }

    private void logCacheMiss(Uri url, String fileId) {
        LOGD(TAG, "CACHE MISS: " + fileId + " : " + url);
    }
    
    private void logCacheIgnored(Uri url, String method) {
        LOGD(TAG, "CACHE IGNORE: " + method + " " + url);
    }

    private void logCacheSaved(Uri url, String fileId) {
        LOGD(TAG, "CACHE SAVED: " + fileId + " : " + url);
    }

    private void logCacheDeleted(String fileId) {
        LOGD(TAG, "CACHE DELETE: " + fileId);
    }

    public CacheManager(Context context) {
        mAppContext = context.getApplicationContext();
        mFilesDir = mAppContext.getFilesDir();
        mCachePath = mFilesDir + "/kaltura/";
        File cacheDir = new File(mCachePath);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        mSQLHelper = new CacheSQLHelper(context);
        String string = Utilities.readAssetToString(mAppContext, CACHED_STRINGS_JSON);
        if (string != null) {
            try {
                mCacheConditions = new JSONObject(string);
            } catch (JSONException e) {
                LOGE(TAG, "Invalid json", e);
            }
        }
    }

    public void setBaseURL(String baseURL) {
        mBaseURL = baseURL;
    }

    public void setCacheSize(float cacheSize) {
        mCacheSize = cacheSize;
    }

    private boolean shouldStore(Uri uri, Map<String, String> headers, String method) {
        
        if (! method.equalsIgnoreCase("GET")) {
            return false;   // only cache GETs
        }
        
        if (! (uri.getScheme().equals("http") || uri.getScheme().equals("https"))) {
            return false;   // only cache http(s)
        }
        
        // Allow app-specific inclusion.
        String uriString = uri.toString();
        for (Pattern pattern : mIncludePatterns) {
            if (pattern.matcher(uriString).matches()) {
                return true;
            }
        }

        if (! uriString.startsWith(mBaseURL)) {
            return false;   // not our server
        }

        // Special case: do not cache the embedFrame page, UNLESS localContentId is set.
        if (uri.getPath().contains("/mwEmbedFrame.php") || uri.getPath().contains("/embedIframeJs/")) {
            if (TextUtils.isEmpty(getLocalContentId(uri))) {
                return false;
            }
        }
        
        return true;
    }

    private void deleteLessUsedFiles(long newCacheSize) {
        long freeBytesInternal = new File(mFilesDir.getAbsoluteFile().toString()).getFreeSpace();
        long cahceSize = (long)(mCacheSize * 1024 * 1024);
        long actualCacheSize = Math.min(cahceSize, freeBytesInternal);
        long currentCacheSize = mSQLHelper.cacheSize();
        boolean shouldDeleteLessUsedFiles = currentCacheSize + newCacheSize > actualCacheSize;
        if (shouldDeleteLessUsedFiles) {
            mSQLHelper.deleteLessUsedFiles(currentCacheSize + newCacheSize - actualCacheSize, new CacheSQLHelper.KSQLHelperDeleteListener() {
                @Override
                public void fileDeleted(String fileId) {
                    File deletedFile = new File(mCachePath, fileId);
                    if (deletedFile.exists()) {
                        deletedFile.delete();
                        logCacheDeleted(fileId);
                    }
                }
            });
        }
    }

    private void setRequestParams(HttpURLConnection connection, Map<String, String> headers, String method) {
        try {
            connection.setRequestMethod(method);
        } catch (ProtocolException e) {
            LOGE(TAG, "Invalid method " + method, e);
            // This can't really happen. But if it did, and we're on a debug build, the app should crash.
            throw new IllegalArgumentException(e);
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
    }
    
    public boolean removeCachedResponse(Uri requestUrl) {
        String fileId = getCacheFileId(requestUrl);

        if (!mSQLHelper.removeFile(fileId)) {
            LOGE(TAG, "Failed to remove cache entry for request: " + requestUrl);
            return false;
        } else {
            File file = new File(mCachePath, fileId);

            if (!file.delete()) {
                LOGE(TAG, "Failed to delete file for request: " + requestUrl);
                return false;
            }
        }
        logCacheDeleted(fileId);

        return true;
    }
    
    public boolean refreshCachedResponse(Uri url) throws IOException {
        boolean remove = removeCachedResponse(url);
        if (!remove) {
            return false;
        }
        
        cacheResponse(url);
        
        return true;
    }
    
    public void cacheResponse(Uri requestUrl) throws IOException {

        // Explicitly load and save the URL - don't even check db.
        String fileName = getCacheFileId(requestUrl);
        File targetFile = new File(mCachePath, fileName);
        WebResourceResponse resp = getResponseFromNetwork(requestUrl, Collections.<String, String>emptyMap(), "GET", fileName, targetFile);
        
        InputStream inputStream = resp.getData();

        // Must fully read the input stream so that it gets cached. But we don't need the data now.
        try {
            byte[] buffer = new byte[1024];
            //noinspection StatementWithEmptyBody
            while (inputStream.read(buffer, 0, buffer.length) >= 0);
        } finally {
            inputStream.close();
        }
    }
    
    public WebResourceResponse getResponse(final Uri requestUrl, Map<String, String> headers, String method) throws IOException {
        if (!shouldStore(requestUrl, headers, method)) {
            logCacheIgnored(requestUrl, method);
            return null;
        }
        boolean online = Utilities.isOnline(mAppContext);
        if (!online && requestUrl.toString().contains("playManifest")) {
            return webResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream("Empty".getBytes()));
        }
        InputStream inputStream;
        String fileName = getCacheFileId(requestUrl);
        File targetFile = new File(mCachePath, fileName);
        String contentType;
        String encoding;
        HashMap<String, Object> fileParams = mSQLHelper.fetchParamsForFile(fileName);

        if (mSQLHelper.sizeForId(fileName) > 0 && fileParams != null) {
            logCacheHit(requestUrl, fileName);
            
            FileInputStream fileInputStream = new FileInputStream(targetFile);
            inputStream = new BufferedInputStream(fileInputStream);
            contentType = (String)fileParams.get(CacheSQLHelper.COL_MIMETYPE);
            encoding = (String)fileParams.get(CacheSQLHelper.COL_ENCODING);
            mSQLHelper.updateDate(fileName);
            WebResourceResponse response = webResourceResponse(contentType, encoding, inputStream);
            return response;

        } else {
            logCacheMiss(requestUrl, fileName);
            
            if (!online) {
                LOGE(TAG, "Error: device is offline and response is not cached.");
            }
            
            return getResponseFromNetwork(requestUrl, headers, method, fileName, targetFile);
        }
    }

    @NonNull
    private WebResourceResponse getResponseFromNetwork(final Uri requestUrl, Map<String, String> headers, String method, String fileName, File targetFile) throws IOException {
        String contentType;
        InputStream inputStream = null;
        URL url = new URL(requestUrl.toString());
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            setRequestParams(connection, headers, method);
            connection.connect();
            contentType = connection.getContentType();

            Map<String, List<String>> headerFields = connection.getHeaderFields();

            if (contentType == null) {
                contentType = "";
            }
            String[] contentTypeParts = TextUtils.split(contentType, ";");
            String encoding = null;
            if (contentTypeParts.length >= 2) {
                contentType = contentTypeParts[0].trim();
                encoding = contentTypeParts[1].trim();
            }
            mSQLHelper.addFile(fileName, contentType, encoding);
            inputStream = new CachingInputStream(targetFile.getAbsolutePath(), connection.getInputStream(), new CachingInputStream.KInputStreamListener() {
                @Override
                public void streamClosed(long fileSize, String filePath) {
                    int trimLength = mCachePath.length();
                    String fileId = filePath.substring(trimLength);
                    mSQLHelper.updateFileSize(fileId, fileSize);
                    logCacheSaved(requestUrl, fileId);
                    deleteLessUsedFiles(fileSize);
                    connection.disconnect();
                }
            });
            return webResourceResponse(contentType, encoding, inputStream);
        } finally {
            // if inputStream wasn't created, streamClosed() will not get called and the connection may leak. 
            if (inputStream == null) {
                connection.disconnect();
            }
        }

    }

    @NonNull
    private static WebResourceResponse webResourceResponse(String contentType, String encoding, InputStream inputStream) {
        return new WebResourceResponse(contentType, encoding, inputStream);
    }

    @NonNull
    private String getCacheFileId(Uri requestUrl) {
        if (requestUrl.getFragment() != null) {
            String localContentId = getLocalContentId(requestUrl);
            if (!TextUtils.isEmpty(localContentId)) {
                return md5("contentId:" + localContentId);
            }
        }
        return md5(requestUrl.toString());
    }

    private String getLocalContentId(Uri requestUrl) {
        return KStringUtilities.extractFragmentParam(requestUrl, "localContentId");
    }

    public void release() {
        if (mSQLHelper != null) {
            mSQLHelper.close();
            mSQLHelper = null;
        }
    }

    public void setIncludePatterns(List<Pattern> includePatterns) {
        mIncludePatterns = new ArrayList<>(includePatterns);    // make a safe copy.
    }
}
