package com.wardtn.facemodel.services;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;


import com.wardtn.facemodel.model.Object3DData;
import com.wardtn.facemodel.services.LoadListener;

import java.net.URI;
import java.util.List;

/**
 * This component allows loading the model without blocking the UI.
 *
 * @author andresoviedo
 */
public abstract class LoaderTask extends AsyncTask<Void, String, List<Object3DData>> implements LoadListener {
    protected Boolean isPreload;

    /**
     * URL to the 3D model
     */
    protected final URI uri;
    /**
     * Callback to notify of events
     */
    private final LoadListener callback;
    /**
     * The dialog that will show the progress of the loading
     */
    private final ProgressDialog dialog;

    /**
     * Build a new progress dialog for loading the data model asynchronously
     *
     * @param uri the URL pointing to the 3d model
     */
    public LoaderTask(Activity parent, URI uri, LoadListener callback) {
        this.uri = uri;
        this.dialog = new ProgressDialog(parent);
        this.callback = callback;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (!isPreload) {
            this.dialog.setMessage("Loading...");
            this.dialog.setCancelable(false);
            //this.dialog.getWindow().setGravity(Gravity.BOTTOM);
            this.dialog.show();
        }
    }


    @Override
    protected List<Object3DData> doInBackground(Void... params) {
        try {
//		    callback.onStart();
            List<Object3DData> data = build();
            callback.onLoadComplete(data);
            return data;
        } catch (Exception ex) {
            callback.onLoadError(ex);
            return null;
        }
    }

    protected abstract List<Object3DData> build() throws Exception;

    public void onLoad(Object3DData data) {
        callback.onLoad(data);
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
		this.dialog.setMessage(values[0]);
    }

    @Override
    protected void onPostExecute(List<Object3DData> data) {
        super.onPostExecute(data);
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @Override
    public void onLoadStart() {
        callback.onLoadStart();
    }

    @Override
    public void onProgress(String progress) {
        super.publishProgress(progress);
        callback.onProgress(progress);
    }

    @Override
    public void onLoadError(Exception ex) {
        callback.onLoadError(ex);
    }

    @Override
    public void onLoadComplete(List<Object3DData> data) {
        callback.onLoadComplete(data);
    }
}