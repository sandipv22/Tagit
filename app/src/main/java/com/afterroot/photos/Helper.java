package com.afterroot.photos;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Sandip on 22-03-2017.
 */

public class Helper {

    private Context mContext;
    public Helper(Context context){
        mContext = context;
    }

    public static int ADD_NOTI_ID = 7865;

    public String getTagitPath(){
        return Environment.getExternalStorageDirectory().getPath() + "/AfterROOT/TagIt/";
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
        return list;
    }

    public ArrayList<String> getImagePaths(){
        String[] proj = {DatabaseHelper.TableColumns.COLUMN_NAME_PATH};
        Cursor cursor = getCursor(DatabaseHelper.TableColumns.TABLE_NAME_IMAGES, proj);
        ArrayList<String> list = new ArrayList<>();
        if (cursor.moveToFirst()){
            do {
                String dbTag = cursor.getString(0);
                list.add(dbTag);
            } while (cursor.moveToNext());
        }
        cursor.close();
        getDatabase().close();
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
}
