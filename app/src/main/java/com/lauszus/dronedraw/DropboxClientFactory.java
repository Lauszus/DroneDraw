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
            DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder("dronedraw")
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
