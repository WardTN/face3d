package com.wardtn.modellibrary.services.wavefront;

import android.app.Activity;
import android.opengl.GLES20;

import com.wardtn.modellibrary.model.Object3DData;
import com.wardtn.modellibrary.services.LoadListener;
import com.wardtn.modellibrary.services.LoaderTask;

import java.net.URI;
import java.util.List;

/**
 * Wavefront loader implementation
 *
 * @author andresoviedo
 */

public class WavefrontLoaderTask extends LoaderTask {
    private Activity activity;
    private boolean isChangeTexture;
    private String objPath;
    private String overLayoutPath;

    public WavefrontLoaderTask(final Activity parent, final URI uri, final LoadListener callback, String objPath, String overLayoutPath,Boolean isPreload) {
        super(parent, uri, callback);
        this.activity = parent;
        isChangeTexture = false;
        this.objPath = objPath;
        this.isPreload = isPreload;
        this.overLayoutPath = overLayoutPath;
    }


    @Override
    protected List<Object3DData> build() {
        final WavefrontLoader wfl = new WavefrontLoader(GLES20.GL_TRIANGLE_FAN, this);
        if (!isPreload) {
            super.publishProgress("加载3D模型中...");
        }
        final List<Object3DData> load = wfl.load(objPath,overLayoutPath);
        return load;
    }

    @Override
    public void onProgress(String progress) {
        super.publishProgress(progress);
    }
}
