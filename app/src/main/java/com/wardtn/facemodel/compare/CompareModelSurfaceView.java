package com.wardtn.facemodel.compare;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Toast;

import com.wardtn.facemodel.controller.TouchController;
import com.wardtn.facemodel.util.AndroidUtils;
import com.wardtn.facemodel.util.event.EventListener;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

public class CompareModelSurfaceView extends GLSurfaceView implements EventListener {

    private final CompareRender mRenderer;
    private TouchController touchController;
    private GestureDetector mGestureDetector;
    private final List<EventListener> listeners = new ArrayList<>();


    public CompareModelSurfaceView(Activity parent, float[] backgroundColor, CompareSceneLoader scene, CompareRender.DividerChangeListener listener) {
        super(parent);
        try {
            Log.i("ModelSurfaceView", "Loading [OpenGL 2] ModelSurfaceView...");

            // Create an OpenGL ES 2.0 context.
            setEGLContextClientVersion(2);

            // This is the actual renderer of the 3D space
            mRenderer = new CompareRender(parent, this, backgroundColor, scene, listener);
            mRenderer.addListener(this);
            setRenderer(mRenderer);

            mGestureDetector = new GestureDetector(getContext(), new CustomGestureListener());

        } catch (Exception e) {
            Log.e("ModelActivity", e.getMessage(), e);
            Toast.makeText(parent, "Error loading shaders:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            throw new RuntimeException(e);
        }
    }

    public void setTouchController(TouchController touchController) {
        this.touchController = touchController;
    }

    public void addListener(EventListener listener) {
        listeners.add(listener);
    }

    public float[] getProjectionMatrix() {
        return mRenderer.getProjectionMatrix();
    }

    public float[] getViewMatrix() {
        return mRenderer.getViewMatrix();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            return touchController.onTouchEvent(event);
        } catch (Exception ex) {
            Log.e("ModelSurfaceView", "Exception: " + ex.getMessage(), ex);
        }
        return false;
    }


    public CompareRender getCompareRender() {
        return mRenderer;
    }

    private void fireEvent(EventObject event) {
        AndroidUtils.fireEvent(listeners, event);
    }

    @Override
    public boolean onEvent(EventObject event) {
        fireEvent(event);
        return true;
    }


    private class CustomGestureListener extends GestureDetector.SimpleOnGestureListener {

        float previousX, previousY;

        @Override
        public boolean onDown(MotionEvent e) {
            Log.e("CHEN", "OnDown");
            previousX = e.getX(0);
            previousY = e.getY(0);
            return super.onDown(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            final float deltaX = e2.getX() - previousX;
            final float deltaY = e2.getY() - previousY;

            Log.e("CHEN", "deltaX = " + deltaX + " deltaY =" + deltaY);

            touchController.handleRotate(deltaX, deltaY);
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    }

}
