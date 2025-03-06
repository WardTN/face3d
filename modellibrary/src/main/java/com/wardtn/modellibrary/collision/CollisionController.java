package com.wardtn.modellibrary.collision;

import android.util.Log;


import com.wardtn.modellibrary.controller.TouchEvent;
import com.wardtn.modellibrary.model.Object3DData;
import com.wardtn.modellibrary.objects.Point;
import com.wardtn.modellibrary.services.SceneLoader;
import com.wardtn.modellibrary.util.android.AndroidUtils;
import com.wardtn.modellibrary.util.event.EventListener;
import com.wardtn.modellibrary.view.ModelSurfaceView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;

public class CollisionController implements EventListener {

    private final ModelSurfaceView view;
    private final SceneLoader scene;
    private final List<Object3DData> objects;

    private final List<EventListener> listeners = new ArrayList<>();

    public CollisionController(ModelSurfaceView view, SceneLoader scene) {
        this.view = view;
        this.scene = scene;
        this.objects = scene.getObjects();
    }

    public List<Object3DData> getObjects() {
        return objects;
    }

    public void addListener(EventListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public boolean onEvent(EventObject event) {
        Log.v("CollisionController", "Processing event... " + event.toString());
        if (event instanceof TouchEvent) {
            TouchEvent touchEvent = (TouchEvent) event;
            if (touchEvent.getAction() == TouchEvent.CLICK) {
                if (objects.isEmpty()) return true;
                Log.v("CollisionController", objects.get(0).getCurrentDimensions().toString());
                final float x = touchEvent.getX();
                final float y = touchEvent.getY();
                Log.v("CollisionController", "Testing for collision... (" + objects.size() + ") " + x + "," + y);
                Object3DData objectHit = CollisionDetection.getBoxIntersection(
                        objects, view.getWidth(), view.getHeight(),
                        view.getViewMatrix(), view.getProjectionMatrix(), x, y);
                if (objectHit != null) {

                    // intersection point
                    Object3DData point3D = null;

                    if (this.scene.isCollision()) {

                        Log.i("CollisionController", "Collision. Getting triangle intersection... " + objectHit.getId());
                        float[] point = CollisionDetection.getTriangleIntersection(objectHit, view.getWidth(), view.getHeight
                                (), view.getViewMatrix(), view.getProjectionMatrix(), x, y);

                        if (point != null) {
                            Log.i("CollisionController", "Drawing intersection point: " + Arrays.toString(point));
                            point3D = Point.build(point).setColor(new float[]{1.0f, 0f, 0f, 1f});
                        }
                    }

                    final CollisionEvent collisionEvent = new CollisionEvent(this, objectHit, x, y, point3D);
                    AndroidUtils.fireEvent(listeners, collisionEvent);
                }
            }
        }
        return true;
    }

}