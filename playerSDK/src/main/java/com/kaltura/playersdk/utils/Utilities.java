package com.kaltura.playersdk.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static com.kaltura.playersdk.utils.LogUtils.LOGE;

/**
 * Created by itayi on 3/5/15.
 */
public class Utilities {

    private static final String TAG = "Utilities";

    public static boolean doesPackageExist(String targetPackage, Context context){
        List<ApplicationInfo> packages;
        PackageManager pm;
        pm = context.getPackageManager();
        packages = pm.getInstalledApplications(0);

        for (ApplicationInfo packageInfo : packages) {
            if(packageInfo.packageName.equals(targetPackage)){
                return true;
            }
        }

        return false;
    }

    public static String readAssetToString(Context context, String asset) {
        try {
            InputStream assetStream = context.getAssets().open(asset);
            return fullyReadInputStream(assetStream, 1024*1024).toString();
        } catch (IOException e) {
            LOGE(TAG, "Failed reading asset " + asset, e);
            return null;
        }
    }

    @NonNull
    public static ByteArrayOutputStream fullyReadInputStream(InputStream inputStream, int byteLimit) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte data[] = new byte[1024];
        int count;
        
        while ((count = inputStream.read(data)) != -1) {
            int maxCount = byteLimit - bos.size();
            if (count > maxCount) {
                bos.write(data, 0, maxCount);
                break;
            } else {
                bos.write(data, 0, count);
            }
        }
        bos.flush();
        bos.close();
        inputStream.close();
        return bos;
    }

    public static Uri stripLastUriPathSegment(Uri uri) {
        String path = uri.getPath();
        if (TextUtils.isEmpty(path)) {
            return uri;
        }
        path = stripLastPathSegment(path);
        return uri.buildUpon().path(path).clearQuery().fragment(null).build();
    }

    public static String stripLastUriPathSegment(String uri) {
        return stripLastUriPathSegment(Uri.parse(uri)).toString();
    }

    @NonNull
    public static String stripLastPathSegment(String path) {
        path = path.substring(0, path.lastIndexOf('/', path.length() - 2));
        return path;
    }

    public static String loadStringFromURL(Uri url, int byteLimit) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url.toString()).openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
        InputStream is = conn.getInputStream();
        return fullyReadInputStream(is, byteLimit).toString();
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMgr.getActiveNetworkInfo();
        return !(netInfo == null || !netInfo.isConnected() || !netInfo.isAvailable());
    }

    public static void quietClose(Closeable... closeables) {
        for (Closeable c : closeables) {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (Exception e) {
                LOGE(TAG, "Failed closing " + c);
            }
        }
    }

    public static void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte data[] = new byte[1024];
        int count;

        while ((count = inputStream.read(data)) != -1) {
            if (count > 0) {
                outputStream.write(data, 0, count);
            }
        }
    }
    
    public static String optString(JSONObject jsonObject, String key) {
        return jsonObject.isNull(key) ? null : jsonObject.optString(key);
    }
}
