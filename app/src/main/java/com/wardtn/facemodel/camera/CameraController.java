package com.wardtn.facemodel.camera;

import android.util.Log;


import com.wardtn.facemodel.controller.TouchEvent;
import com.wardtn.facemodel.model.Camera;
import com.wardtn.facemodel.util.event.EventListener;
import com.wardtn.facemodel.view.ModelRenderer;

import java.util.EventObject;

public final class CameraController implements EventListener {

    private final Camera camera;
    private int width;
    private int height;


    public CameraController(Camera camera) {
        this.camera = camera;
    }


    @Override
    public boolean onEvent(EventObject event) {
        if (event instanceof ModelRenderer.ViewEvent) {
            this.width = ((ModelRenderer.ViewEvent) event).getWidth();
            this.height = ((ModelRenderer.ViewEvent) event).getHeight();
        } else if (event instanceof TouchEvent) {
            TouchEvent touchEvent = (TouchEvent) event;
            switch (touchEvent.getAction()) {
                case PINCH:
                    float zoomFactor = ((TouchEvent) event).getZoom() / 10;
                    Log.v("CameraController", "Zooming '" + zoomFactor + "'...");
                    camera.MoveCameraZ(zoomFactor);
                    break;
                case ORBIT:
                    float offsetX = touchEvent.getX();
                    float offsetY = touchEvent.getY();
                    camera.rabitRotation(offsetX, -offsetY);
                    break;
            }
        }
        return true;
    }


    /**
     * 围绕Z轴旋转
     *
     * @param degree
     */
    private float curXDegree = 0f;
    private final float MAX_X_ANGLE = 30f;

    private void rotateX(float degree) {
        if (Math.abs(degree + curXDegree) >= MAX_X_ANGLE) {
            //当前度数大于 限制度数
            if (degree >= 0) {
                //当前右滑动
                degree = MAX_X_ANGLE - curXDegree;
            } else {
                //当前向左滑动
                degree = -MAX_X_ANGLE - curXDegree;
            }
        }
        curXDegree += degree;
        camera.rotate(degree, 0, 1f, 0);
    }

    /**
     * 围绕 X 轴 旋转
     *
     * @param degree
     */
    private float curYDegree = 0f;
    private final float MAX_Y_ANGLE = 30;

    private void rotateY(float degree) {
        if (Math.abs(degree + curYDegree) >= MAX_Y_ANGLE) {
            //当前度数大于 限制度数
            if (degree >= 0) {
                //当前上滑动
                degree = MAX_Y_ANGLE - curYDegree;
            } else {
                //当前向下滑动
                degree = -MAX_Y_ANGLE - curYDegree;
            }
        }
        curYDegree += degree;
        camera.rotate(degree, 1, 0, 0);
    }

}
