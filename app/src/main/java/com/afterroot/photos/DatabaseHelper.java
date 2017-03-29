package com.afterroot.photos;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Created by Sandip on 22-03-2017.
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "TagIt.db";
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private String SQL_CREATE_ENTRIES = "CREATE TABLE " + TableColumns.TABLE_NAME_IMAGES + " (" +
            TableColumns._ID + " INTEGER PRIMARY KEY," +
            TableColumns.COLUMN_NAME_FILENAME + TEXT_TYPE + COMMA_SEP +
            TableColumns.COLUMN_NAME_PATH + TEXT_TYPE + COMMA_SEP +
            TableColumns.COLUMN_NAME_TAG + TEXT_TYPE + " )";

    private String SQL_CREATE_TAGS_TABLE = "CREATE TABLE " + TableColumns.TABLE_NAME_TAGS + " (" +
            TableColumns.COLUMN_NAME_TAG + TEXT_TYPE + " )";

    private String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + TableColumns.TABLE_NAME_IMAGES;
    private String SQL_DELETE_TAGS_ENTRIES = "DROP TABLE IF EXISTS " + TableColumns.TABLE_NAME_TAGS;

    public DatabaseHelper(Context context){
        super(context, new Helper(context).getTagitPath() + DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_ENTRIES);
        sqLiteDatabase.execSQL(SQL_CREATE_TAGS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL(SQL_DELETE_ENTRIES);
        sqLiteDatabase.execSQL(SQL_DELETE_TAGS_ENTRIES);
        onCreate(sqLiteDatabase);
    }

    public static abstract class TableColumns implements BaseColumns {
        public static final String TABLE_NAME_IMAGES = "images";
        public static final String TABLE_NAME_TAGS = "tagsTable";
        public static final String COLUMN_NAME_FILENAME = "fileName";
        public static final String COLUMN_NAME_PATH = "path";
        public static final String COLUMN_NAME_TAG = "tag";
    }
}
