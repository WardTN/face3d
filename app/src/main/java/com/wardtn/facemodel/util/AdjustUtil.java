package com.wardtn.facemodel.util;

public class AdjustUtil {
    protected static float maxX = Float.MIN_VALUE;
    protected static float maxY = Float.MIN_VALUE;
    protected static float maxZ = Float.MIN_VALUE;
    protected static float minX = Float.MAX_VALUE;
    protected static float minY = Float.MAX_VALUE;
    protected static float minZ = Float.MAX_VALUE;


    private static final float MODEL_BOUND_SIZE = 50f;

    public static void adjustMaxMin(float x, float y, float z) {
        if (x > maxX) {
            maxX = x;
        }
        if (y > maxY) {
            maxY = y;
        }
        if (z > maxZ) {
            maxZ = z;
        }
        if (x < minX) {
            minX = x;
        }
        if (y < minY) {
            minY = y;
        }
        if (z < minZ) {
            minZ = z;
        }
    }


    protected static float getBoundScale() {
        float scaleX = (maxX - minX) / MODEL_BOUND_SIZE;
        float scaleY = (maxY - minY) / MODEL_BOUND_SIZE;
        float scaleZ = (maxZ - minZ) / MODEL_BOUND_SIZE;
        float scale = scaleX;
        if (scaleY > scale) {
            scale = scaleY;
        }
        if (scaleZ > scale) {
            scale = scaleZ;
        }
        return scale;
    }


}
