package com.afterroot.tagit.async

import android.content.Context
import android.database.Cursor
import android.os.AsyncTask
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.tagit.DatabaseHelper
import com.afterroot.tagit.Helper
import com.afterroot.tagit.TaskCallback
import com.afterroot.tagit.adapter.ImagesRecyclerViewAdapter
import java.io.File

class DeleteFromDatabaseTask(private val mContext: Context, private val mAdapter: ImagesRecyclerViewAdapter, private val mRemoveTaskCallback: TaskCallback) : AsyncTask<List<Int>, String, String>() {
    private lateinit var mCursor: Cursor
    private val mHelper: Helper = Helper(mContext)
    private lateinit var mProgressDialog: MaterialDialog

    override fun onPreExecute() {
        val projection = arrayOf(DatabaseHelper.TableColumns.COLUMN_NAME_FILENAME, DatabaseHelper.TableColumns.COLUMN_NAME_PATH)
        mCursor = mHelper.getCursor(DatabaseHelper.TableColumns.TABLE_NAME_IMAGES, projection)

        mProgressDialog = MaterialDialog.Builder(mContext)
                .progress(false, mCursor.count, true)
                .title("Removing from Database")
                .content("Loading...")
                .negativeText("Cancel")
                .onNegative { _, _ -> cancel(true) }
                .cancelable(false)
                .build()
        mProgressDialog.show()

        super.onPreExecute()
    }

    override fun doInBackground(lists: Array<List<Int>>): String {
        mCursor.moveToFirst()
        for (i in lists[0]) {
            val file = File(mAdapter.getPath(i))
            mCursor.moveToNext()
            mHelper.database!!.delete(DatabaseHelper.TableColumns.TABLE_NAME_IMAGES,
                    DatabaseHelper.TableColumns.COLUMN_NAME_PATH + "=? AND " + DatabaseHelper.TableColumns.COLUMN_NAME_TAG + "=?",
                    arrayOf(mAdapter.getPath(i), mHelper.filterTag))
            publishProgress(file.name)
        }
        return if (isCancelled) {
            "Removing from Database cancelled"
        } else {
            "Removed from Database"
        }
    }

    override fun onProgressUpdate(vararg values: String) {
        mProgressDialog.setContent(values[0])
        mProgressDialog.setProgress(mCursor.position)
        super.onProgressUpdate(*values)
    }

    override fun onPostExecute(s: String) {
        mCursor.close()
        mHelper.database!!.close()
        mProgressDialog.dismiss()
        mRemoveTaskCallback.onTaskFinished()
        mHelper.showToast(s)
        super.onPostExecute(s)
    }
}
