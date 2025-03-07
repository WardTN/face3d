package com.wardtn.modellibrary.drawer;


import com.wardtn.modellibrary.model.Object3DData;

public interface Renderer {

	void draw(Object3DData obj, float[] pMatrix, float[] vMatrix, int textureId, float[] lightPosInWorldSpace, float[] cameraPos);

	void draw(Object3DData obj, float[] pMatrix, float[] vMatrix, int textureId,boolean isOverlay, float[] lightPosInWorldSpace, float[] colorMask, float[] cameraPos);

	void draw(Object3DData obj, float[] pMatrix, float[] vMatrix, int drawType, int drawSize, int textureId,boolean isOverlay, float[]
			lightPosInWorldSpace, float[] colorMask, float[] cameraPos);
}