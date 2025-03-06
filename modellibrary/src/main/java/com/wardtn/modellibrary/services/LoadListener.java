package com.wardtn.modellibrary.services;


import com.wardtn.modellibrary.model.Object3DData;

import java.util.List;

public interface LoadListener {

    void onLoadStart();

    void onProgress(String progress);

    void onLoadError(Exception ex);

    void onLoad(Object3DData data);

    void onLoadComplete(List<Object3DData> data);
}
