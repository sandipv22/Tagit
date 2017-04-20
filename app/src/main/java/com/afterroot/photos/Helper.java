package com.afterroot.photos;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Sandip on 22-03-2017.
 */

public class Helper {

    private Context mContext;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    @SuppressLint("CommitPrefEdits")
    public Helper(Context context) {
        mContext = context;
        mEditor = getSharedPreferences().edit();
    }

    public static int ADD_NOTI_ID = 7865;

    public String getTagitPath(){
        return String.format("%s%s", Environment.getExternalStorageDirectory().getPath(), mContext.getString(R.string.photos_path));
    }

    public String getFilterTag(){
        return mSharedPreferences.getString(mContext.getString(R.string.key_filter_tag), "");
    }

    public void setFilterTag(String tag){
        mEditor.putString(mContext.getString(R.string.key_filter_tag), tag).apply();
    }

    public SharedPreferences getSharedPreferences(){
        if (mSharedPreferences == null){
            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        }
        return mSharedPreferences;
    }

    public int getSortOrder(){
        return mSharedPreferences.getInt("sortBy", SORT_BY_NAME);
    }

    public void setSortOrder(int sortOrder){
        mEditor.putInt("sortBy", sortOrder).apply();
    }

    public ArrayList<String> getTags(){
        String[] proj = {DatabaseHelper.TableColumns.COLUMN_NAME_TAG};
        Cursor cursor = getCursor(DatabaseHelper.TableColumns.TABLE_NAME_TAGS, proj);
        ArrayList<String> list = new ArrayList<>();
        if (cursor.moveToFirst()){
            do {
                String dbTag = cursor.getString(0);
                list.add(dbTag);
            } while (cursor.moveToNext());
        }
        cursor.close();
        getDatabase().close();
        Collections.sort(list);
        return list;
    }

    static final int SORT_BY_NAME = 1;
    static final int SORT_BY_DATE = 2;
    public ArrayList<String> getImagePaths(String tag, int sortBy){
        String[] proj = {DatabaseHelper.TableColumns.COLUMN_NAME_PATH, DatabaseHelper.TableColumns.COLUMN_NAME_TAG};
        Cursor cursor = getCursor(DatabaseHelper.TableColumns.TABLE_NAME_IMAGES, proj);
        ArrayList<String> list = new ArrayList<>();
        if (cursor.moveToFirst()){
            do {
                String dbPath = cursor.getString(0);
                String dbTag = cursor.getString(1);
                if (tag != null && dbTag.equals(tag)){
                    list.add(dbPath);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        getDatabase().close();
        if (sortBy == SORT_BY_DATE){
            Collections.sort(list, (lhs, rhs) -> {
                File rhF = new File(rhs);
                File lhF = new File(lhs);
                return Long.valueOf(rhF.lastModified()).compareTo(lhF.lastModified());
            });
        } else if (sortBy == SORT_BY_NAME){
            Collections.sort(list);
        }
        return list;
    }

    private SQLiteDatabase mDatabase;

    private void setDatabase(){
        mDatabase= new DatabaseHelper(mContext).getWritableDatabase();
    }

    public SQLiteDatabase getDatabase(){
        if (mDatabase == null || !mDatabase.isOpen()){
            setDatabase();
        }
        return mDatabase;
    }

    public Cursor getCursor(String tableName, String[] projection){
        return getDatabase().query(tableName, projection, null, null, null, null, null);
    }

    public void showToast(String message){
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    public void showSnackbar(View view, String message){
        showSnackbar(view, message, Snackbar.LENGTH_SHORT, null, null);
    }

    public void showSnackbar(View view, String message, int duration, @Nullable String actionName, @Nullable View.OnClickListener listener){
        Snackbar.make(view, message, duration).setAction(actionName, listener).show();
    }

    public static class Static {
        public static final int REQUEST_CODE_IMAGE_VIEWER = 5464;
        static String EXTRA_VIEWPAGER_POS = "viewpager_position";
        static String EXTRA_GOTO_RECYCLER_POS = "goto_recycler";
    }
}
