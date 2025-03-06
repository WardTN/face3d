package com.wardtn.modellibrary.util.android.assets;


import com.wardtn.modellibrary.util.android.AndroidURLConnection;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 *  App's assets URL handler
 */
public class Handler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(final URL url) {
        return new AndroidURLConnection(url);
    }

}