package com.afterroot.photos.async;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afterroot.photos.DatabaseHelper;
import com.afterroot.photos.Helper;
import com.afterroot.photos.adapter.ImagesRecyclerViewAdapter;
import com.afterroot.photos.async.AddToDatabaseTask.TaskCallback;

import java.io.File;
import java.util.List;

public class DeleteFromDatabaseTask extends AsyncTask<List<Integer>, String, String> {
    private Cursor mCursor;
    private Helper mHelper;
    private ImagesRecyclerViewAdapter mAdapter;
    private TaskCallback mRemoveTaskCallback;
    private MaterialDialog mProgressDialog;
    private Context mContext;

    public DeleteFromDatabaseTask(Context context, ImagesRecyclerViewAdapter adapter, TaskCallback removeTaskCallback){
        this.mContext = context;
        mHelper = new Helper(mContext);
        this.mAdapter = adapter;
        this.mRemoveTaskCallback = removeTaskCallback;
    }

    @Override
    protected void onPreExecute() {
        String[] projection = {
                DatabaseHelper.TableColumns.COLUMN_NAME_FILENAME,
                DatabaseHelper.TableColumns.COLUMN_NAME_PATH
        };
        mCursor = mHelper.getCursor(DatabaseHelper.TableColumns.TABLE_NAME_IMAGES, projection);

        mProgressDialog = new MaterialDialog.Builder(mContext)
                .progress(false, mCursor.getCount(), true)
                .title("Removing from Database")
                .content("Loading...")
                .negativeText("Cancel")
                .onNegative((dialog, which) -> cancel(true))
                .cancelable(false)
                .build();
        mProgressDialog.show();

        super.onPreExecute();
    }

    @Override
    protected String doInBackground(List<Integer>[] lists) {
        mCursor.moveToFirst();
        for (int i : lists[0]){
            File file = new File(mAdapter.getPath(i));
            mCursor.moveToNext();
            mHelper.getDatabase().delete(DatabaseHelper.TableColumns.TABLE_NAME_IMAGES,
                    DatabaseHelper.TableColumns.COLUMN_NAME_PATH + "=? AND "+ DatabaseHelper.TableColumns.COLUMN_NAME_TAG + "=?",
                    new String[]{mAdapter.getPath(i), mHelper.getFilterTag()});
            publishProgress(file.getName());
        }
        if (isCancelled()) {
            return "Removing from Database cancelled";
        } else {
            return "Removed from Database";
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        mProgressDialog.setContent(values[0]);
        mProgressDialog.setProgress(mCursor.getPosition());
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(String s) {
        mCursor.close();
        mHelper.getDatabase().close();
        mProgressDialog.dismiss();
        mRemoveTaskCallback.onTaskFinished();
        mHelper.showToast(s);
        super.onPostExecute(s);
    }
}
