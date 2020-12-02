package com.afterroot.tagit.async

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.AsyncTask
import androidx.core.app.NotificationCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.tagit.*
import net.yazeed44.imagepicker.model.ImageEntry
import java.io.File
import java.util.*

class AddToDatabaseTask(context: Context, private var mAddTaskCallback: TaskCallback) : AsyncTask<ArrayList<ImageEntry>, String, String>() {
    var mContext = context
    private lateinit var currentFileName: String
    private lateinit var builder: NotificationCompat.Builder
    private lateinit var mNotificationManager: NotificationManager
    private lateinit var tag: String
    private lateinit var mProgressDialog: MaterialDialog
    private var max = 0
    private var curr = 0
    private val mHelper: Helper = Helper(mContext)

    fun setTag(tag: String) {
        this.tag = tag
    }

    override fun onPreExecute() {
        mProgressDialog = MaterialDialog.Builder(mContext)
                .title("Adding..." + "Tag: " + tag)
                .progress(false, max, true)
                .content("Please Wait...")
                .negativeText("Cancel")
                .onNegative { _, _ -> cancel(true) }
                .cancelable(false)
                .build()
        mProgressDialog.show()
        super.onPreExecute()
    }

    override fun onProgressUpdate(vararg values: String) {
        mProgressDialog.setContent(values[0])
        if (mProgressDialog.maxProgress < 1) {
            mProgressDialog.maxProgress = max
        }
        mProgressDialog.setProgress(curr)
        super.onProgressUpdate(*values)
    }

    override fun doInBackground(vararg arrayLists: ArrayList<ImageEntry>): String {
        max = arrayLists[0].size
        mNotificationManager = mContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val dbImagePaths = mHelper.getImagePaths(tag, 0)
        val pendingIntent = PendingIntent.getActivity(mContext,
                0, Intent(mContext, MainActivity::class.java), PendingIntent.FLAG_NO_CREATE)

        for (i in 0 until max) {
            curr = i
            val sourcePath = arrayLists[0][curr].path
            val sourceFile = File(sourcePath)
            currentFileName = sourceFile.name
            publishProgress(currentFileName)
            val values = ContentValues()
            values.put(DatabaseHelper.TableColumns.COLUMN_NAME_FILENAME, currentFileName)
            values.put(DatabaseHelper.TableColumns.COLUMN_NAME_PATH, sourceFile.path)
            values.put(DatabaseHelper.TableColumns.COLUMN_NAME_TAG, tag)

            if (!dbImagePaths.contains(sourcePath)) {
                mHelper.database!!.insert(DatabaseHelper.TableColumns.TABLE_NAME_IMAGES, null, values)
            }

            builder = NotificationCompat.Builder(mContext, "general")
                    .setSmallIcon(R.drawable.ic_action_add)
                    .setContentTitle("Adding...")
                    .setProgress(max, curr, false)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setContentText(currentFileName)
            mNotificationManager.notify(Helper.Companion.ADD_NOTI_ID, builder.build())
        }

        if (tag != " " && !mHelper.tags.contains(tag) && !tag.substring(tag.length - 1, tag.length).endsWith(" ")) {
            val tagsValues = ContentValues()
            tagsValues.put(DatabaseHelper.TableColumns.COLUMN_NAME_TAG, tag)
            mHelper.database!!.insert(DatabaseHelper.TableColumns.TABLE_NAME_TAGS, null, tagsValues)
        }
        return "Added to Database."
    }

    override fun onPostExecute(result: String) {
        mAddTaskCallback.onTaskFinished()
        mHelper.showToast(result)
        mProgressDialog.dismiss()
        mHelper.database!!.close()
        mNotificationManager.cancelAll()
        super.onPostExecute(result)
    }
}
