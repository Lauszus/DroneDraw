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
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PathMeasure;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVWriter;

public class DrawActivity extends AppCompatActivity {
    public static final String TAG = DrawActivity.class.getSimpleName();
    public static final boolean D = BuildConfig.DEBUG; // This is automatically set when building

    private final int WRITE_EXTERNAL_STORAGE_REQUEST = 0;

    @SuppressLint("InlinedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_draw);

        final DrawingView mDrawView = (DrawingView) findViewById(R.id.draw_view);
        mDrawView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        Button mClearButton = (Button) findViewById(R.id.clear_button);
        mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDrawView.clearView();
            }
        });

        if (ContextCompat.checkSelfPermission(this,  Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, WRITE_EXTERNAL_STORAGE_REQUEST);

        findViewById(R.id.upload_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(),  Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    return;

                if (!mDrawView.mFullPath.isEmpty()) {
                    File csvFileLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "path.csv");
                    CSVWriter writer;
                    try {
                        writer = new CSVWriter(new FileWriter(csvFileLocation), ',', CSVWriter.NO_QUOTE_CHARACTER);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }

                    int dataSize = 10 * mDrawView.touchCounter; // Sample path 10 times more than actual touches
                    for (int i = 0; i <= dataSize; i++) {
                        PathMeasure mPathMeasure = new PathMeasure(mDrawView.mFullPath, false);

                        float t = (float)i/(float)dataSize;
                        float[] xy = new float[2];
                        mPathMeasure.getPosTan(mPathMeasure.getLength() * t, xy, null);

                        writer.writeNext(new String[] { Float.toString(t), Float.toString(xy[0]), Float.toString(xy[1]), "1" });

                        if (D)
                            Log.d(TAG, "t: " + t + " x: " + xy[0] + " y: " + xy[1]);
                    }
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }

                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.setType("vnd.android.cursor.dir/email");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { "lauszus@gmail.com" });
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Drone path");
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "Please see attachment");
                    emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(csvFileLocation));

                    if (emailIntent.resolveActivity(getPackageManager()) != null) { // Make sure that an app exist that can handle the intent
                        startActivity(emailIntent);
                    } else
                        Toast.makeText(getApplicationContext(), "No email app found", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted, yay!
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    // Permission denied, boo!
                    Toast.makeText(this, "Permission required!", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }
}
