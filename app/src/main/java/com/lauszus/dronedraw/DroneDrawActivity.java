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

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PathMeasure;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVWriter;

public class DroneDrawActivity extends AppCompatActivity {
    public static final String TAG = DroneDrawActivity.class.getSimpleName();
    public static final boolean D = BuildConfig.DEBUG; // This is automatically set when building

    private DrawingView mDrawView;
    private final int PERMISSIONS_REQUEST_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_draw);

        if (!hasDropboxToken())
            Auth.startOAuth2Authentication(DroneDrawActivity.this, getString(R.string.app_key));

        mDrawView = (DrawingView) findViewById(R.id.draw_view);
        findViewById(R.id.upload_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Request permission if needed
                if (ContextCompat.checkSelfPermission(getApplicationContext(),  Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
                    ActivityCompat.requestPermissions(DroneDrawActivity.this, new String[]{ Manifest.permission.INTERNET }, PERMISSIONS_REQUEST_CODE);
                else
                    uploadCsvFile();
            }
        });
    }
    
    private void sendEmail(File file) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("vnd.android.cursor.dir/email");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{ "lauszus@gmail.com" });
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Drone path");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Please see attachment");
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));

        if (emailIntent.resolveActivity(getPackageManager()) != null) { // Make sure that an app exist that can handle the intent
            startActivity(emailIntent);
        } else
            Toast.makeText(getApplicationContext(), "No email app found", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String accessToken = prefs.getString("access-token", null);
        if (accessToken == null) {
            accessToken = Auth.getOAuth2Token();
            if (accessToken != null) {
                prefs.edit().putString("access-token", accessToken).apply();
                DropboxClientFactory.init(accessToken);
            }
        } else
            DropboxClientFactory.init(accessToken);
    }

    protected boolean hasDropboxToken() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString("access-token", null) != null;
    }

    private void uploadFileToDropbox(File file) {
        DbxClientV2 client;
        try {
            client = DropboxClientFactory.getClient();
        } catch (IllegalStateException e) {
            Toast.makeText(DroneDrawActivity.this, "Please setup your Dropbox account", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCancelable(false);
        dialog.setMessage("Uploading to Dropbox");
        dialog.show();

        new UploadFileTask(client, new UploadFileTask.Callback() {
            @Override
            public void onUploadComplete(FileMetadata result) {
                dialog.dismiss();
                Toast.makeText(DroneDrawActivity.this, "Path uploaded to Dropbox", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                dialog.dismiss();
                if (D)
                    Log.e(TAG, "Failed to upload file: ", e);
                Toast.makeText(DroneDrawActivity.this, "Failed to upload path to Dropbox", Toast.LENGTH_SHORT).show();
            }
        }).execute(Uri.fromFile(file).toString(), "");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    uploadCsvFile();
                } else {
                    Toast.makeText(this, "Permission required!", Toast.LENGTH_LONG).show();
                }
        }
    }

    private void uploadCsvFile() {
        if (!mDrawView.mFullPath.isEmpty()) {
            File csvFileLocation = new File(getFilesDir(), "path.csv");
            try {
                CSVWriter writer = new CSVWriter(new FileWriter(csvFileLocation), ',', CSVWriter.NO_QUOTE_CHARACTER);
                final int dataSize = 10 * mDrawView.touchCounter; // Sample path 10 times more than actual touches
                for (int i = 0; i <= dataSize; i++) {
                    PathMeasure mPathMeasure = new PathMeasure(mDrawView.mFullPath, false);

                    final float t = (float) i / (float) dataSize;
                    float[] xy = new float[2];
                    mPathMeasure.getPosTan(mPathMeasure.getLength() * t, xy, null);

                    final int maxDimension = Math.max(mDrawView.getWidth(), mDrawView.getHeight());

                    xy[0] /= (float) maxDimension; // Normalize coordinates
                    xy[1] /= (float) maxDimension;

                    writer.writeNext(new String[]{Integer.toString(i), Float.toString(xy[0]), Float.toString(xy[1]), "1"});

                    //if (D) Log.d(TAG, "t: " + t + " x: " + xy[0] + " y: " + xy[1]);
                }
                writer.close();
                //sendEmail(csvFileLocation);
                uploadFileToDropbox(csvFileLocation);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
