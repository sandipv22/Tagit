package com.afterroot.tagit

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Environment
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.view.View
import android.widget.Toast
import java.io.File
import java.util.*

class Helper constructor(private val mContext: Context) {

    val sharedPreferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(mContext)

    private val mEditor: SharedPreferences.Editor
    init {
        mEditor = sharedPreferences.edit()
    }

    val tagItPath: String
        get() = String.format("%s%s", Environment.getExternalStorageDirectory().path, mContext.getString(R.string.photos_path))

    var filterTag: String
        get() = sharedPreferences.getString(mContext.getString(R.string.key_filter_tag), "")
        set(tag) = mEditor.putString(mContext.getString(R.string.key_filter_tag), tag).apply()

    var sortOrder: Int
        get() = sharedPreferences.getInt("sortBy", SORT_BY_NAME)
        set(sortOrder) = mEditor.putInt("sortBy", sortOrder).apply()

    val tags: ArrayList<String>
        get() {
            val proj = arrayOf(DatabaseHelper.TableColumns.COLUMN_NAME_TAG)
            val cursor = getCursor(DatabaseHelper.TableColumns.TABLE_NAME_TAGS, proj)
            val list = ArrayList<String>()
            if (cursor.moveToFirst()) {
                do {
                    val dbTag = cursor.getString(0)
                    list.add(dbTag)
                } while (cursor.moveToNext())
            }
            cursor.close()
            database!!.close()
            Collections.sort(list)
            return list
        }

    fun getImagePaths(tag: String?, sortBy: Int): ArrayList<String> {
        val proj = arrayOf(DatabaseHelper.TableColumns.COLUMN_NAME_PATH, DatabaseHelper.TableColumns.COLUMN_NAME_TAG)
        val cursor = getCursor(DatabaseHelper.TableColumns.TABLE_NAME_IMAGES, proj)
        val list = ArrayList<String>()
        if (cursor.moveToFirst()) {
            do {
                val dbPath = cursor.getString(0)
                val dbTag = cursor.getString(1)
                if (tag != null && dbTag == tag) {
                    list.add(dbPath)
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        database!!.close()
        if (sortBy == SORT_BY_DATE) {
            Collections.sort(list) { lhs, rhs ->
                val rhF = File(rhs)
                val lhF = File(lhs)
                java.lang.Long.valueOf(rhF.lastModified())!!.compareTo(lhF.lastModified())
            }
        } else if (sortBy == SORT_BY_NAME) {
            Collections.sort(list)
        }
        return list
    }

    private var mDatabase: SQLiteDatabase? = null

    private fun setDatabase() {
        mDatabase = DatabaseHelper(mContext).writableDatabase
    }

    val database: SQLiteDatabase?
        get() {
            if (mDatabase == null || !mDatabase!!.isOpen) {
                setDatabase()
            }
            return mDatabase
        }

    fun getCursor(tableName: String, projection: Array<String>): Cursor {
        return database!!.query(tableName, projection, null, null, null, null, null)
    }

    fun showToast(message: String) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show()
    }

    @JvmOverloads
    fun showSnackbar(view: View, message: String, duration: Int = Snackbar.LENGTH_SHORT, actionName: String? = null, listener: View.OnClickListener? = null) {
        Snackbar.make(view, message, duration).setAction(actionName, listener).show()
    }

    companion object {
        var ADD_NOTI_ID = 7865
        var SORT_BY_NAME = 0
        val SORT_BY_DATE = 1
        val REQUEST_CODE_IMAGE_VIEWER = 5464
        var EXTRA_VIEWPAGER_POS = "viewpager_position"
        var EXTRA_GOTO_RECYCLER_POS = "goto_recycler"
    }
}
