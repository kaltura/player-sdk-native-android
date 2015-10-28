package com.kaltura.playersdk.Helpers;

import android.content.Context;
import android.util.Log;

import com.google.android.exoplayer.upstream.Loader;

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
    private String mFilePath;
    private BufferedOutputStream mOutputStream;

    public KInputStream(InputStream in) {
        super(in);
    }

    public KInputStream(String filePath,InputStream inputStream) {
        super(inputStream);
        mFilePath = filePath;
    }

    private BufferedOutputStream getOutputStream() throws FileNotFoundException {
        if (mOutputStream == null) {
            mOutputStream = new BufferedOutputStream(new FileOutputStream(mFilePath));
        }
        return mOutputStream;
    }

    @Override
    public void close() throws IOException {
        getOutputStream().close();
        super.close();
    }

    @Override
    public synchronized int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        int bytesRead = super.read(buffer, byteOffset, byteCount);
        if (bytesRead > 0) {
            getOutputStream().write(buffer, 0, bytesRead);
        }
        return bytesRead;
    }
}
