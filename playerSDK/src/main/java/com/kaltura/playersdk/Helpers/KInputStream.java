package com.kaltura.playersdk.Helpers;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by nissimpardo on 25/10/15.
 */
public class KInputStream extends BufferedInputStream {
    private String mUrl;
    private BufferedOutputStream mOutputStream;
    private Context mContext;
    private String mMimeType;
    private String mEncoding;
    private KSQLHelper mSQLHelper;


    public KInputStream(InputStream in) {
        super(in);
    }

    public KInputStream(String url, String mimeType, String encoding, InputStream inputStream, Context context) {
        super(inputStream);
        mUrl = url;
        mContext = context;
        mMimeType = mimeType;
        mEncoding = encoding;
    }

    private BufferedOutputStream getOutputStream() throws FileNotFoundException {
        if (mOutputStream == null) {
            mOutputStream = new BufferedOutputStream(new FileOutputStream(mContext.getFilesDir() + "/" + KStringUtilities.md5(mUrl)));
        }
        return mOutputStream;
    }

    private KSQLHelper getSQLHelper() {
        if (mSQLHelper == null) {
            mSQLHelper = new KSQLHelper(mContext);
        }
        return mSQLHelper;
    }

    @Override
    public void close() throws IOException {
        getSQLHelper().addFile(KStringUtilities.md5(mUrl), mMimeType, mEncoding);
        getOutputStream().flush();
        getOutputStream().close();
        super.close();
    }

    @Override
    public synchronized int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        int bytesRead = super.read(buffer, byteOffset, byteCount);
        getOutputStream().write(buffer, byteOffset, byteCount);
        return bytesRead;
    }
}
