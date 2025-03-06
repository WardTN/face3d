package com.wardtn.modellibrary.objects;

import android.opengl.GLES20;

import com.wardtn.modellibrary.model.Object3DData;
import com.wardtn.modellibrary.util.io.IOUtils;


public final class Line {

    public static Object3DData build(float[] line) {
        return new Object3DData(IOUtils.createFloatBuffer(line.length).put(line))
                .setDrawMode(GLES20.GL_LINES).setId("Line");
    }
}
