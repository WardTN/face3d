package com.wardtn.modellibrary.controller;

import android.app.Activity;
import android.graphics.PointF;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;



import com.wardtn.modellibrary.util.android.AndroidUtils;
import com.wardtn.modellibrary.util.event.EventListener;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

public class TouchController {

    // constants
    private int width;
    private int height;

    // variables
    private final List<EventListener> listeners = new ArrayList<>();

    private float x1 = Float.MIN_VALUE;
    private float y1 = Float.MIN_VALUE;
    private float x2 = Float.MIN_VALUE;
    private float y2 = Float.MIN_VALUE;
    private float dx1 = Float.MIN_VALUE;
    private float dy1 = Float.MIN_VALUE;
    private float dx2 = Float.MIN_VALUE;
    private float dy2 = Float.MIN_VALUE;

    private float length = Float.MIN_VALUE;
    private float previousLength = Float.MIN_VALUE;

    public TouchController(Activity parent) {
        super();
        try {
            if (!AndroidUtils.supportsMultiTouch(parent.getPackageManager())) {
                Log.w("ModelActivity", "Multitouch not supported. Some app features may not be available");
            } else {
                Log.i("ModelActivity", "Initializing TouchController...");
            }
        } catch (Exception e) {
            Toast.makeText(parent, "Error loading Touch Controller:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void addListener(EventListener listener) {
        this.listeners.add(listener);
    }

    private void fireEvent(EventObject eventObject) {
        AndroidUtils.fireEvent(listeners, eventObject);
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                Log.e("CHEN", "ACTION_DOWN");
                previousX = motionEvent.getX();
                previousY = motionEvent.getY();
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (motionEvent.getPointerCount() == 1) {
                    singleMove(motionEvent);
                } else {
                    twoFingerAction(motionEvent);
                }
            }
        }
        return true;
    }

    private float previousX = 0f;
    private float previousY = 0f;

    private SelfFloat yaw = new SelfFloat(-45f);
    private SelfFloat pitch = new SelfFloat(30f);


    private void singleMove(MotionEvent event) {
        Log.e("CHEN", "ACTION_MOVE");

        if (touchMode != TOUCH_ROTATE) {
            previousX = event.getX();
            previousY = event.getY();
        }
        touchMode = TOUCH_ROTATE;
        float x = event.getX();
        float y = event.getY();
        float dx = x - previousX;
        float dy = y - previousY;
        previousX = x;
        previousY = y;
        fireEvent(new TouchEvent(this, TouchEvent.ORBIT, width, height, dx, dy));
    }

    private void twoFingerAction(MotionEvent motionEvent) {
        if (touchMode != TOUCH_ZOOM) {
            pinchStartDistance = getPinchDistance(motionEvent);
            getPinchCenterPoint(motionEvent, pinchStartPoint);
            previousX = pinchStartPoint.x;
            previousY = pinchStartPoint.y;
            touchMode = TOUCH_ZOOM;
        } else {
            PointF pointF = new PointF();
            getPinchCenterPoint(motionEvent, pointF);
            float dx = pointF.x - previousX;
            float dy = pointF.y - previousY;
            previousX = pointF.x;
            previousY = pointF.y;
            float pinchScale = getPinchDistance(motionEvent) / pinchStartDistance;
            if (pinchScale < 1f) {
                pinchScale = -pinchScale;
            }
            //增加缩放系数
            pinchScale = 7 * pinchScale;
            pinchStartDistance = getPinchDistance(motionEvent);
//            Log.e("CHEN", " pinchScale =" + pinchScale);
            fireEvent(new TouchEvent(this, TouchEvent.PINCH, width, height, dx, dy, pinchScale));
        }
    }

    private final int TOUCH_NONE = 0;
    private final int TOUCH_ROTATE = 1;
    private final int TOUCH_ZOOM = 2;
    private int touchMode = TOUCH_NONE;
    private PointF pinchStartPoint = new PointF();
    private float pinchStartDistance = 0.0f;

    public void handleRotate(float xOffset, float yOffset) {
//        yaw.setVal(yaw.getVal() + xOffset / 16f);
//        pitch.setVal(pitch.getVal() + yOffset / 16f);
//
//        pitch.setVal(MathUtils.clamp(pitch.getVal(), -89.0f, 89.0f));
//        if (yaw.getVal() > 180.0f) {
//            yaw.setVal(yaw.getVal() - 360.0f);
//        }
//        if (yaw.getVal() < -180.0f) {
//            yaw.setVal(360.0f + yaw.getVal());
//        }

        //设置摄像在 3D中的位置
        fireEvent(new TouchEvent(this, TouchEvent.ORBIT, width, height, 0, 0,
                x1, y1, xOffset, yOffset, (length - previousLength), null));
    }

    private float getPinchDistance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return ((Double) Math.sqrt(x * x + y * y)).floatValue();
    }

    private void getPinchCenterPoint(MotionEvent event, PointF f) {
        f.x = (event.getX(0) + event.getX(1)) * 0.5f;
        f.y = (event.getY(0) + event.getY(1)) * 0.5f;
    }


    class SelfFloat {
        private float val;

        public SelfFloat() {
            this.val = 0.0f;
        }

        public SelfFloat(float val) {
            this.val = val;
        }

        public float getVal() {
            return val;
        }

        public void setVal(float val) {
            this.val = val;
        }

        public void plusEqual(float newVal) {
            this.val += newVal;
        }

        public void swapData(Object newDataObject) {
            if (newDataObject instanceof SelfFloat) {
                SelfFloat newSelfFloat = (SelfFloat) newDataObject;
                this.val = newSelfFloat.getVal();
            } else {
                throw new IllegalArgumentException("Object \"" + newDataObject.getClass().getName() + "\" is not instance of " + this.getClass().getName());
            }
        }
    }

}

