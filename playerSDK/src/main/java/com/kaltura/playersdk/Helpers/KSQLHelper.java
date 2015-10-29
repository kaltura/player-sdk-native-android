package com.kaltura.playersdk.Helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.android.exoplayer.util.SystemClock;

import java.util.Date;
import java.util.HashMap;

/**
 * Created by nissimpardo on 26/10/15.
 */
public class KSQLHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "KCache.db";
    private final String TABLE_NAME = "KCacheTable";
    private final String id = "_id";
    public static final String Encoding = "Encoding";
    public static final String MimeType = "MimeType";
    public static final String LastUsed = "LastUsed";
    public static final String Size = "Size";

    public KSQLHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_PRODUCTS_TABLE = "Create table IF NOT EXISTS " +
                TABLE_NAME + "("
                + id + " TEXT," + Encoding
                + " TEXT," + MimeType + " TEXT," + LastUsed + "INTEGER," + Size + "INTEGER)";
        db.execSQL(CREATE_PRODUCTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }



    public void addFile(String fileName, String mimeType, String encoding) {
        ContentValues values = new ContentValues();
        values.put(id, fileName);
        values.put(MimeType, mimeType);
        values.put(Encoding, encoding);
        SQLiteDatabase db = getWritableDatabase();
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public void removeFile(String fileId) {

    }

    public long cacheSize() {
        String query = "SELECT SUM(Size) FROM KCacheTable";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            return cursor.getInt(0);
        }
        return 0;
    }

    public void updateDate(String fileId) {
        ContentValues data = new ContentValues();
        data.put(LastUsed, System.currentTimeMillis());
        SQLiteDatabase db = getWritableDatabase();
        db.update(DATABASE_NAME, data, id + "=" + fileId, null);
    }

    public void updateFileSize(String fileId, long fileSize) {
        ContentValues data = new ContentValues();
        data.put(Size, fileSize);
        SQLiteDatabase db = getWritableDatabase();
        db.update(DATABASE_NAME, data, id + "=" + fileId, null);
    }

    public long sizeForId(String fileId) {
        String query = "Select Size FROM " + TABLE_NAME + " WHERE " + id + " =  \"" + fileId + "\"";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            int size = cursor.getInt(0);
            cursor.close();
            return size;
        }
        return 0;
    }

    public void deleteLessUsedFiles(long sizeToDelete) {
        String query = "SELECT * FROM KCacheTable ORDER BY LastUsed";
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        boolean stop = false;
        while (!stop) {
            if (cursor.moveToFirst()) {
                int size = cursor.getInt(4);
                String fileId = cursor.getString(0);
                cursor.close();
                if (db.delete(TABLE_NAME, id + "=" + fileId, null) > 0) {
                    sizeToDelete -= size;
                    stop = sizeToDelete <= 0;
                }
            }
        }
    }

    public HashMap<String, Object> fetchParamsForFile(String fileName) {
        String query = "Select * FROM " + TABLE_NAME + " WHERE " + id + " =  \"" + fileName + "\"";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            HashMap<String , Object> params = new HashMap<>();
            params.put(Encoding, cursor.getString(1));
            params.put(MimeType, cursor.getString(2));
            return params;
        }
        return null;
    }
}
