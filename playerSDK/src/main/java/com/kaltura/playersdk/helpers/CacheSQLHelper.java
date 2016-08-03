package com.kaltura.playersdk.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;
import static com.kaltura.playersdk.utils.LogUtils.LOGE;

/**
 * Created by nissimpardo on 26/10/15.
 */
public class CacheSQLHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "KCache.db";
    private static final String TAG = "CacheSQLHelper";
    private final String TABLE_NAME = "KCacheTable";
    private final String COL_FILEID = "_id";
    public static final String COL_ENCODING = "Encoding";
    public static final String COL_MIMETYPE = "MimeType";
    public static final String COL_LASTUSED = "LastUsed";
    public static final String COL_SIZE = "Size";

    private SQLiteDatabase mDatabase;

    public interface KSQLHelperDeleteListener {
        void fileDeleted(String fileId);
    }

    public CacheSQLHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_PRODUCTS_TABLE = "Create table IF NOT EXISTS " +
                TABLE_NAME + "("
                + COL_FILEID + " TEXT," + COL_ENCODING
                + " TEXT," + COL_MIMETYPE + " TEXT," + COL_LASTUSED + " INTEGER," + COL_SIZE + " INTEGER)";
        db.execSQL(CREATE_PRODUCTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    private SQLiteDatabase db() {
        // Keep db open.
        if (mDatabase == null || !mDatabase.isOpen()) {
            synchronized (this) {
                if (mDatabase == null || !mDatabase.isOpen()) {
                    mDatabase = getWritableDatabase();
                }
            }
        }
        return mDatabase;
    }


    @Override
    protected void finalize() throws Throwable {
        // Finalizers are unpredictable, but Android's CloseGuards use them.
        try {
            if (mDatabase != null) {
                mDatabase.close();
                mDatabase = null;
            }
        } finally {
            super.finalize();
        }
    }

    @Override
    public synchronized void close() {
        if (mDatabase != null) {
            mDatabase.close();
            mDatabase = null;
        }
        super.close();
    }

    public void addFile(String fileId, String mimeType, String encoding) {
        ContentValues values = new ContentValues();
        values.put(COL_FILEID, fileId);
        values.put(COL_MIMETYPE, mimeType);
        values.put(COL_ENCODING, encoding);
        values.put(COL_LASTUSED, 0);
        values.put(COL_SIZE, 0);
        try {
            if (entryExists(fileId)) {
                db().update(TABLE_NAME, values, this.COL_FILEID + "=?", new String[]{fileId});
            } else {
                db().insert(TABLE_NAME, null, values);
            }
        } catch (SQLiteException e) {
            LOGE(TAG, "Error adding file, fileId=" + fileId);
        }
    }

    private boolean entryExists(String fileId) {
        Cursor c = null;
        try {
            c = db().query(TABLE_NAME, null, this.COL_FILEID + "=?", new String[]{fileId}, null, null, null);
            return c.getCount() > 0;
        } finally {
            quietClose(c);
        }
    }

    public boolean removeFile(String fileId) {
        try {
            SQLiteDatabase db = db();
            db.delete(TABLE_NAME, COL_FILEID + "=?", new String[]{fileId});
            db.close();
            return true;
        } catch (SQLiteException e) {
            return false;
        }
    }

    public long cacheSize() {
        String query = "SELECT SUM(Size) FROM KCacheTable";
        Cursor cursor = null; 
        long size = 0;
        try {
            cursor = db().rawQuery(query, null);
            if (cursor.moveToFirst()) {
                size = cursor.getInt(0);
            }
        } catch (Exception e) {
            LOGE(TAG, "Error getting total cache size", e);
        } finally {
            quietClose(cursor);
        }
        
        return size;
    }

    public void updateDate(String fileId) {
        ContentValues data = new ContentValues();
        data.put(COL_LASTUSED, System.currentTimeMillis());
        try {
            db().update(TABLE_NAME, data, COL_FILEID + "=?", new String[]{fileId});
        } catch (SQLiteException e) {
            LOGE(TAG, "Error updating entry date", e);
        }
    }

    public void updateFileSize(String fileId, long fileSize) {
        ContentValues data = new ContentValues();
        data.put(COL_SIZE, fileSize);
        data.put(COL_LASTUSED, System.currentTimeMillis());
        try {
            db().update(TABLE_NAME, data, COL_FILEID + "=?", new String[]{fileId});
        } catch (SQLiteException e) {
            LOGE(TAG, "Error updating entry size", e);
        }
    }

    private void quietClose(Cursor c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (Exception e) {
            LOGE(TAG, "Failed closing " + c);
        }
    }
    
    public long sizeForId(String fileId) {
        Cursor cursor = null;
        long size = 0;
        try {
            cursor = db().query(TABLE_NAME, new String[]{COL_SIZE}, COL_FILEID + "=?", new String[]{fileId}, null, null, null);
            if (cursor.moveToFirst()) {
                size = cursor.getInt(0);
            }
        } finally {
            quietClose(cursor);
        }
        return size;
    }

    public void deleteLessUsedFiles(long sizeToDelete, KSQLHelperDeleteListener listener) {
        
        SQLiteDatabase db = db();

        ArrayList<String> deletedIds = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_NAME, new String[]{COL_FILEID, COL_SIZE}, null, null, null, null, COL_LASTUSED + " DESC");
            while (cursor.moveToNext()) {
                String fileId = cursor.getString(0);
                int size = cursor.getInt(1);
                deletedIds.add(fileId);
                sizeToDelete -= size;
                if (sizeToDelete <= 0) {
                    break;
                }
            }
        } catch (SQLiteException e) {
            LOGE(TAG, "Error getting list of files to delete");
        } finally {
            quietClose(cursor);
        }
        
        
        for (String fileId : deletedIds) {
            try {
                db.delete(TABLE_NAME, COL_FILEID + "=?", new String[]{fileId});
                listener.fileDeleted(fileId);
            } catch (SQLiteException e) {
                LOGE(TAG, "Error deleting entry (lessUsed) " + fileId);
            }
        }
    }

    public HashMap<String, Object> fetchParamsForFile(String fileId) {
        SQLiteDatabase db = db();
        Cursor cursor = null;

        HashMap<String, Object> params = null;
        try {
            cursor = db.query(TABLE_NAME, new String[]{COL_FILEID, COL_ENCODING, COL_MIMETYPE}, COL_FILEID + "=?", new String[]{fileId}, null, null, null);
            if (cursor.moveToFirst()) {
                params = new HashMap<>();
                params.put(COL_ENCODING, cursor.getString(1));
                params.put(COL_MIMETYPE, cursor.getString(2));
            }
        } catch (SQLiteException e) {
            LOGE(TAG, "Error fetching params for " + fileId);
        } finally {
            quietClose(cursor);
        }
        return params;
    }
}
