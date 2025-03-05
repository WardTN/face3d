package com.wardtn.facemodel.util.android;

import com.wardtn.facemodel.util.android.assets.Handler;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class AndroidURLStreamHandlerFactory implements URLStreamHandlerFactory {

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if ("android".equals(protocol)) {
            return new Handler();
        } else if ("content".equals(protocol)){
            return new Handler();
        }
        return null;
    }
}
