package com.afterroot.photos.async;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afterroot.photos.DatabaseHelper;
import com.afterroot.photos.Helper;
import com.afterroot.photos.MainActivity;
import com.afterroot.photos.R;

import net.yazeed44.imagepicker.model.ImageEntry;

import java.io.File;
import java.util.ArrayList;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.afterroot.photos.Helper.ADD_NOTI_ID;

/**
 * Created by Sandip on 24-03-2017.
 */

public class AddToDatabaseTask extends AsyncTask<ArrayList<ImageEntry>, String, String> {
    private String currentFileName;
    private NotificationCompat.Builder builder;
    private NotificationManager mNotificationManager;
    private String tag = "Untagged";
    private MaterialDialog mProgressDialog;
    private int max = 0, curr = 0;
    private Context mContext;
    private Helper mHelper;
    public OnPreExcuteListener mOnPreExcuteListener;

    public AddToDatabaseTask(Context context, OnPreExcuteListener listener){
        this.mContext = context;
        this.mHelper = new Helper(mContext);
        mOnPreExcuteListener = listener;

    }

    public void setTag(String tag){
        this.tag = tag;
    }

    @Override
    protected void onPreExecute() {
        mProgressDialog = new MaterialDialog.Builder(mContext)
                .title("Adding..." + "Tag: " + tag)
                .progress(false, max, true)
                .content("Please Wait...")
                .negativeText("Cancel")
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        cancel(true);
                    }
                })
                .cancelable(false)
                .build();
        mProgressDialog.show();
        super.onPreExecute();
    }

    @Override
    protected void onProgressUpdate(String... values) {
        mProgressDialog.setContent(values[0]);
        if (mProgressDialog.getMaxProgress() < 1){
            mProgressDialog.setMaxProgress(max);
        }
        mProgressDialog.setProgress(curr);
        super.onProgressUpdate(values);
    }

    @Override
    protected String doInBackground(ArrayList<ImageEntry>... arrayLists) {
        boolean error = false;
        max = arrayLists[0].size();
        mNotificationManager = (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);

        for (int i = 0; i < max; i++){
            curr = i;
            String sourcePath = arrayLists[0].get(curr).path;
            File sourceFile = new File(sourcePath);
            currentFileName = sourceFile.getName();
            publishProgress(currentFileName);
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.TableColumns.COLUMN_NAME_FILENAME, currentFileName);
            values.put(DatabaseHelper.TableColumns.COLUMN_NAME_PATH, sourceFile.getPath());
            values.put(DatabaseHelper.TableColumns.COLUMN_NAME_TAG, tag);

            mHelper.getDatabase().insert(DatabaseHelper.TableColumns.TABLE_NAME_IMAGES, null, values);

            PendingIntent pendingIntent = PendingIntent.getActivity(mContext,
                    0, new Intent(mContext, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
            builder = new NotificationCompat.Builder(mContext)
                    .setSmallIcon(R.drawable.ic_action_add)
                    .setContentTitle("Adding...")
                    .setProgress(max, curr, false)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setContentText(currentFileName);
            mNotificationManager.notify(ADD_NOTI_ID, builder.build());
        }

        if (error){
            return "An Error Occurred.";
        } else {
            if (tag != null && !tag.equals(" ") && !mHelper.getTags().contains(tag)){
                ContentValues tagsValues = new ContentValues();
                tagsValues.put(DatabaseHelper.TableColumns.COLUMN_NAME_TAG, tag);
                mHelper.getDatabase().insert(DatabaseHelper.TableColumns.TABLE_NAME_TAGS, null, tagsValues);
            }
            return "Added to Database.";
        }
    }

    @Override
    protected void onPostExecute(String result) {
        mOnPreExcuteListener.onTaskFinished();
        mHelper.showToast(result);
        mProgressDialog.dismiss();
        mHelper.getDatabase().close();
        mNotificationManager.cancelAll();
        super.onPostExecute(result);
    }

    public interface OnPreExcuteListener {
        public void onTaskFinished();
    }
}
