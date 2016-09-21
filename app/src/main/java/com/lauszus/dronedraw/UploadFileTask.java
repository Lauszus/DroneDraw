/*******************************************************************************
 * Copyright (C) 2016 Kristian Sloth Lauszus. All rights reserved.
 *
 * This software may be distributed and modified under the terms of the GNU
 * General Public License version 2 (GPL2) as published by the Free Software
 * Foundation and appearing in the file GPL2.TXT included in the packaging of
 * this file. Please note that GPL2 Section 2[b] requires that all works based
 * on this software must also be made publicly available under the terms of
 * the GPL2 ("Copyleft").
 *
 * Contact information
 * -------------------
 *
 * Kristian Sloth Lauszus
 * Web      :  http://www.lauszus.com
 * e-mail   :  lauszus@gmail.com
 ******************************************************************************/

package com.lauszus.dronedraw;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Async task to upload a file to a directory
 * Source: https://github.com/dropbox/dropbox-sdk-java/blob/master/examples/android/src/main/java/com/dropbox/core/examples/android/UploadFileTask.java
 */
class UploadFileTask extends AsyncTask<String, Void, FileMetadata> {
    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    private Exception mException;

    public interface Callback {
        void onUploadComplete(FileMetadata result);
        void onError(Exception e);
    }

    UploadFileTask(DbxClientV2 dbxClient, Callback callback) {
        mDbxClient = dbxClient;
        mCallback = callback;
    }

    @Override
    protected void onPostExecute(FileMetadata result) {
        super.onPostExecute(result);
        if (mException != null)
            mCallback.onError(mException);
        else if (result == null)
            mCallback.onError(null);
        else
            mCallback.onUploadComplete(result);
    }

    @Override
    protected FileMetadata doInBackground(String... params) {
        String localUri = params[0];
        String remoteFolderPath = params[1];

        File localFile = new File(Uri.parse(localUri).getPath());
        String remoteFileName = localFile.getName();

        String path = remoteFolderPath + "/" + remoteFileName;

        if (DroneDrawActivity.D)
            Log.d(DroneDrawActivity.TAG, "Uploading: \"" + localUri + "\" to: \"" + path + "\"");

        try {
            InputStream inputStream = new FileInputStream(localFile);
            return mDbxClient.files().uploadBuilder(path)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(inputStream);
        } catch (DbxException | IOException e) {
            mException = e;
        }

        return null;
    }
}
