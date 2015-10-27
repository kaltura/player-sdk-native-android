package com.kaltura.playersdk.Helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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

    public KSQLHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_PRODUCTS_TABLE = "Create table IF NOT EXISTS " +
                TABLE_NAME + "("
                + id + " TEXT," + Encoding
                + " TEXT," + MimeType + " TEXT)";
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

    public HashMap<String, String> fetchParamsForFile(String fileName) {
        String query = "Select * FROM " + TABLE_NAME + " WHERE " + id + " =  \"" + fileName + "\"";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            HashMap<String , String> params = new HashMap<>();
            params.put(Encoding, cursor.getString(1));
            params.put(MimeType, cursor.getString(2));
            return params;
        }
        return null;
    }
}
