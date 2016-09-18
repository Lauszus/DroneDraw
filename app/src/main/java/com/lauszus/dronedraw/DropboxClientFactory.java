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

import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.DbxRequestConfig;

import java.util.Locale;

/**
 * Singleton instance of {@link DbxClientV2} and friends
 * Source: https://github.com/dropbox/dropbox-sdk-java/blob/master/examples/android/src/main/java/com/dropbox/core/examples/android/DropboxClientFactory.java
 */
public class DropboxClientFactory {
    private static DbxClientV2 sDbxClient;

    public static void init(String accessToken) {
        if (sDbxClient == null) {
            DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder(BuildConfig.APPLICATION_ID + BuildConfig.VERSION_NAME)
                    .withUserLocale(Locale.getDefault().toString())
                    .withHttpRequestor(OkHttp3Requestor.INSTANCE)
                    .build();
            sDbxClient = new DbxClientV2(requestConfig, accessToken);
        }
    }

    public static DbxClientV2 getClient() throws IllegalStateException {
        if (sDbxClient == null)
            throw new IllegalStateException("Client not initialized.");
        return sDbxClient;
    }
}
