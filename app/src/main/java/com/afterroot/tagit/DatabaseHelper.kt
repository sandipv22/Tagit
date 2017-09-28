package com.afterroot.tagit

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, Helper(context).tagItPath + DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private val DATABASE_VERSION = 1
        private val DATABASE_NAME = "TagIt.db"
        private val TEXT_TYPE = " TEXT"
        private val COMMA_SEP = ","
    }

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        val SQL_CREATE_ENTRIES = "CREATE TABLE " + TableColumns.TABLE_NAME_IMAGES + " (" +
                TableColumns.ID + " INTEGER PRIMARY KEY," +
                TableColumns.COLUMN_NAME_FILENAME + TEXT_TYPE + COMMA_SEP +
                TableColumns.COLUMN_NAME_PATH + TEXT_TYPE + COMMA_SEP +
                TableColumns.COLUMN_NAME_TAG + TEXT_TYPE + " )"
        val SQL_CREATE_TAGS_TABLE = "CREATE TABLE " + TableColumns.TABLE_NAME_TAGS + " (" +
                TableColumns.COLUMN_NAME_TAG + TEXT_TYPE + " )"

        sqLiteDatabase.execSQL(SQL_CREATE_ENTRIES)
        sqLiteDatabase.execSQL(SQL_CREATE_TAGS_TABLE)
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, i: Int, i1: Int) {
        val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + TableColumns.TABLE_NAME_IMAGES
        val SQL_DELETE_TAGS_ENTRIES = "DROP TABLE IF EXISTS " + TableColumns.TABLE_NAME_TAGS

        sqLiteDatabase.execSQL(SQL_DELETE_ENTRIES)
        sqLiteDatabase.execSQL(SQL_DELETE_TAGS_ENTRIES)
        onCreate(sqLiteDatabase)
    }

    class TableColumns {
        companion object {
            val TABLE_NAME_IMAGES = "images"
            val TABLE_NAME_TAGS = "tagsTable"
            val COLUMN_NAME_FILENAME = "fileName"
            val COLUMN_NAME_PATH = "path"
            val COLUMN_NAME_TAG = "tag"
            val ID = BaseColumns._ID
        }
    }
}
