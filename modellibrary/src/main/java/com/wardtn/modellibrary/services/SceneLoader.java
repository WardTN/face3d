package com.wardtn.modellibrary.services;

import android.app.Activity;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;




import com.wardtn.modellibrary.animation.Animator;
import com.wardtn.modellibrary.collision.CollisionEvent;
import com.wardtn.modellibrary.controller.TouchEvent;
import com.wardtn.modellibrary.model.Camera;
import com.wardtn.modellibrary.model.Dimensions;
import com.wardtn.modellibrary.model.Object3DData;
import com.wardtn.modellibrary.model.Transform;
import com.wardtn.modellibrary.objects.Point;
import com.wardtn.modellibrary.services.wavefront.WavefrontLoaderTask;
import com.wardtn.modellibrary.util.android.ContentUtils;
import com.wardtn.modellibrary.util.event.EventListener;
import com.wardtn.modellibrary.util.io.IOUtils;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 3D场景
 *
 * @author andresoviedo
 */
public class SceneLoader implements LoadListener, EventListener {

    /**
     * 任意轴上的默认最大尺寸
     */
    private static final float DEFAULT_MAX_MODEL_SIZE = 30;
    /**
     * 相机在 Z 轴上的位置
     * 模型最大尺寸的一半加上25
     */
    private static final float DEFAULT_CAMERA_POSITION = DEFAULT_MAX_MODEL_SIZE / 2 + 25;
    /**
     * Parent component
     */
    protected final Activity parent;

    /**
     * List of 3D models
     */
    private List<Object3DData> objects = new ArrayList<>();

    private boolean haveLoad3DData; //是否已经加载过3D数据

    /**
     * Point of view camera
     */
    private Camera camera = new Camera(DEFAULT_CAMERA_POSITION);
    /**
     * Blender uses different coordinate system.
     * This is a patch to turn camera and SkyBox 90 degree on X axis
     */
    private boolean isFixCoordinateSystem = false;
    /**
     * Enable or disable blending (transparency)
     */
    private boolean isBlendingEnabled = true;
    /**
     * Force transparency
     */
    private boolean isBlendingForced = false;
    /**
     * state machine for drawing modes
     */
    private int drawwMode = 0;
    /**
     * Whether to draw objects as wireframes
     */
    private boolean drawWireframe = false;
    /**
     * Whether to draw using points
     */
    private boolean drawPoints = false;
    /**
     * Whether to draw bounding boxes around objects
     */
    private boolean drawBoundingBox = false;
    /**
     * Whether to draw face normals. Normally used to debug models
     */
    private boolean drawNormals = false;
    /**
     * Whether to draw using textures
     */
    private boolean drawTextures = true;
    /**
     * Whether to draw using colors or use default white color
     */
    private boolean drawColors = true;
    /**
     * Light toggle feature: we have 3 states: no light, light, light + rotation
     */
    private boolean rotatingLight = true;
    /**
     * Light toggle feature: whether to draw using lights
     */
    private boolean drawLighting = false;
    /**
     * Animate model (dae only) or not
     */
    private boolean doAnimation = true;
    /**
     * Animate model (dae only) or not
     */
    private boolean isSmooth = false;
    /**
     * show bind pose only
     */
    private boolean showBindPose = false;
    /**
     * Draw skeleton or not
     */
    private boolean drawSkeleton = false;
    /**
     * Toggle collision detection
     */
    private boolean isCollision = false;
    /**
     * Toggle 3d
     */
    private boolean isStereoscopic = false;
    /**
     * Toggle 3d anaglyph (red, blue glasses)
     */
    private boolean isAnaglyph = false;
    /**
     * Toggle 3d VR glasses
     */
    private boolean isVRGlasses = false;
    /**
     * Object selected by the user
     */
    private Object3DData selectedObject = null;
    /**
     * Light bulb 3d data
     */
    private final Object3DData lightBulb = Point.build(new float[]{0, 0, 0}).setId("light");
    /**
     * Animator
     */
    private Animator animator = new Animator();
    /**
     * Did the user touched the model for the first time?
     */
    private boolean userHasInteracted;
    /**
     * time when model loading has started (for stats)
     */
    private long startTime;

    /**
     * A cache to save original model dimensions before rescaling them to fit in screen
     * This enables rescaling several times
     */
    private Map<Object3DData, Dimensions> originalDimensions = new HashMap<>();
    private Map<Object3DData, Transform> originalTransforms = new HashMap<>();

    public SceneLoader(Activity main, URI uri, int type) {
        this.parent = main;

        lightBulb.setLocation(new float[]{0, 0, DEFAULT_CAMERA_POSITION});
    }


    public boolean startLoad(String path, int isLightType, boolean isPreloadMode) {
        camera.setChanged(true); // force first draw

//        if (haveLoad3DData) {
//            Log.e("CHEN", "已经加载过数据");
//            List<Object3DData> objects = getObjects();
//            if (!ListUtil.listIsEmp(objects)) {
//                Object3DData object3DData = objects.get(0);
//                Log.e("CHEN", "当前的纹理为" + object3DData.getTetxurePath());
//                String oldImgPath = object3DData.getTetxurePath();
//                if (StringUtils.isEmpty(oldImgPath)) return false;
//                String curImgPath = get3DDownloadPath() + "/" + getRenderImg(isLightType);
//                if (!oldImgPath.equalsIgnoreCase(curImgPath)) {
//                    Log.e("CHEN","路径不一致 当前原路径为 " + oldImgPath + " 新路径为 " + curImgPath + " 开始切换纹理");
//                    String imgPath = get3DDownloadPath() + "/" + getRenderImg(isLightType);
//                    List<Element> elements = ModelRenderer.loadMaterials(this.objects.get(0).getMeshData(), path, imgPath);
//                    this.objects.get(0).setTetxurePath(imgPath);  // 纹理路径
//                    this.objects.get(0).setElements(elements); // 元素
//                    return true;
//                }
//            }
//        } else {
            Log.e("CHEN", "开始 调用 AsyncTask 获取数据");
            new WavefrontLoaderTask(parent, null, this, path,  isPreloadMode).execute();
//        }
        return false;
    }

    //判断当前是否为预加载模型状态
    private boolean isPreloadMode;

//    public void preloadLoadData(String path, String useID, String testID) {
//        if (haveLoad3DData) {
//            Log.e("CHEN", "预加载 已经加载过 数据");
//            return;
//        }
//        isPreloadMode = true;
//        new WavefrontLoaderTask(parent, null, this, path, useID, testID, false).execute();
//    }


    public void fixCoordinateSystem() {
        final List<Object3DData> objects = getObjects();
        for (int i = 0; i < objects.size(); i++) {
            final Object3DData objData = objects.get(i);
            if (objData.getAuthoringTool() != null && objData.getAuthoringTool().toLowerCase().contains("blender")) {
                getCamera().rotate(90f, 1, 0, 0);
                Log.i("SceneLoader", "Fixed coordinate system to 90 degrees on x axis. object: " + objData.getId());
                this.isFixCoordinateSystem = true;
                break;
            }
        }
    }

    public boolean isFixCoordinateSystem() {
        return this.isFixCoordinateSystem;
    }

    public final Camera getCamera() {
        return camera;
    }

    private final void makeToastText(final String text, final int toastDuration) {
        parent.runOnUiThread(() -> Toast.makeText(parent.getApplicationContext(), text, toastDuration).show());
    }

    public final Object3DData getLightBulb() {
        return lightBulb;
    }

    /**
     * Hook for animating the objects before the rendering
     */
    public final synchronized void addObject(Object3DData obj) {
        Log.i("SceneLoader", "Adding object to scene... " + obj);
        objects.add(obj);
        //requestRender();

        // rescale objects so they fit in the viewport
        // FIXME: this does not be reviewed
        //rescale(this.getObjects(), DEFAULT_MAX_MODEL_SIZE, new float[3]);
    }


    /**
     * 还原 3D 展示状态
     */
    public void reset3D() {
        if (camera != null) {
            camera.resetRotation();
            camera.resetZoom();
            camera.setChanged(true);
        }
    }


    public final synchronized List<Object3DData> getObjects() {
        return objects;
    }

    public final boolean isDrawWireframe() {
        return this.drawWireframe;
    }


    public final boolean isDoAnimation() {
        return doAnimation;
    }

    public final boolean isShowBindPose() {
        return showBindPose;
    }


    public final boolean isDrawTextures() {
        return drawTextures;
    }

    public final boolean isDrawColors() {
        return drawColors;
    }

    public final boolean isDrawLighting() {
        return drawLighting;
    }

    public final boolean isDrawSkeleton() {
        return drawSkeleton;
    }

    public final boolean isCollision() {
        return isCollision;
    }

    public final boolean isStereoscopic() {
        return isStereoscopic;
    }


    public final boolean isAnaglyph() {
        return isAnaglyph;
    }

    public final boolean isBlendingEnabled() {
        return isBlendingEnabled;
    }

    public final boolean isBlendingForced() {
        return isBlendingForced;
    }

    @Override
    public void onLoadStart() {

        // mark start time
        startTime = SystemClock.uptimeMillis();

        // provide context to allow reading resources
        ContentUtils.setThreadActivity(parent);
    }

    @Override
    public void onProgress(String progress) {
    }

    @Override
    public synchronized void onLoad(Object3DData data) {

        // if we add object, we need to initialize Animation, otherwise ModelRenderer will crash
        if (doAnimation) {
            animator.update(data, isShowBindPose());
        }

        // load new object and rescale all together so they fit in the viewport
        addObject(data);

        // rescale objects so they fit in the viewport
        //rescale(this.getObjects(), DEFAULT_MAX_MODEL_SIZE, new float[3]);

    }

    public interface LoadListener {
        void load3DSuc();
    }

    private LoadListener loadListener;

    public void setLoadListener(LoadListener loadListener) {
        this.loadListener = loadListener;
    }

    @Override
    public synchronized void onLoadComplete(List<Object3DData> object3DDataList) {

        // get complete list of objects loaded
        final List<Object3DData> objs = getObjects();
        haveLoad3DData = true;

        // show object errors
        List<String> allErrors = new ArrayList<>();

        for (Object3DData data : objs) {
            allErrors.addAll(data.getErrors());
        }

        if (!allErrors.isEmpty()) {
            makeToastText(allErrors.toString(), Toast.LENGTH_LONG);
        }

        // notify user
        final String elapsed = (SystemClock.uptimeMillis() - startTime) / 1000 + " secs";

//        makeToastText("Load complete (" + elapsed + ")", Toast.LENGTH_LONG);

        // clear thread local
        ContentUtils.setThreadActivity(null);

//         rescale all object so they fit in the screen
        rescale(this.getObjects(), DEFAULT_MAX_MODEL_SIZE, new float[3]);

        // fix coordinate system
//        fixCoordinateSystem();
        if (loadListener != null) {
            loadListener.load3DSuc();
        }
    }

    private void rescale(List<Object3DData> objs) {
        Log.v("SceneLoader", "Rescaling objects... " + objs.size());

        // get largest object in scene
        float largest = 1;
        for (int i = 0; i < objs.size(); i++) {
            Object3DData data = objs.get(i);
            float candidate = data.getCurrentDimensions().getLargest();
            if (candidate > largest) {
                largest = candidate;
            }
        }
        Log.v("SceneLoader", "Object largest dimension: " + largest);

        // rescale objects
        float ratio = DEFAULT_MAX_MODEL_SIZE / largest;
        Log.v("SceneLoader", "Scaling " + objs.size() + " objects with factor: " + ratio);
        float[] newScale = new float[]{ratio, ratio, ratio};
        for (Object3DData data : objs) {
            // data.center();
            data.setScale(newScale);
        }
    }

    @Override
    public void onLoadError(Exception ex) {
        Log.e("SceneLoader", ex.getMessage(), ex);
        makeToastText("There was a problem building the model: " + ex.getMessage(), Toast.LENGTH_LONG);
        ContentUtils.setThreadActivity(null);
    }

    public Object3DData getSelectedObject() {
        return selectedObject;
    }

    private void setSelectedObject(Object3DData selectedObject) {
        this.selectedObject = selectedObject;
    }

    public void loadTexture(Object3DData obj, Uri uri) throws IOException {
        if (obj == null && objects.size() != 1) {
            makeToastText("Unavailable", Toast.LENGTH_SHORT);
            return;
        }
        obj = obj != null ? obj : objects.get(0);

        // load new texture
        obj.setTextureData(IOUtils.read(ContentUtils.getInputStream(uri)));

        this.drawTextures = true;
    }

    public final boolean isRotatingLight() {
        return rotatingLight;
    }

//    public void setView(ModelSurfaceView view) {
//        this.glView = view;
//    }

    @Override
    public boolean onEvent(EventObject event) {
        //Log.v("SceneLoader","Processing event... "+event);
        if (event instanceof TouchEvent) {
            userHasInteracted = true;
        } else if (event instanceof CollisionEvent) {
            Object3DData objectToSelect = ((CollisionEvent) event).getObject();
            Object3DData point = ((CollisionEvent) event).getPoint();
            if (isCollision() && point != null) {
                addObject(point);
            } else {
                if (getSelectedObject() == objectToSelect) {
                    Log.i("SceneLoader", "Unselected object " + objectToSelect.getId());
                    Log.d("SceneLoader", "Unselected object " + objectToSelect);
                    setSelectedObject(null);
                } else {
                    Log.i("SceneLoader", "Selected object " + objectToSelect.getId());
                    Log.d("SceneLoader", "Selected object " + objectToSelect);
                    setSelectedObject(objectToSelect);
                }
            }
        }
        return true;
    }

    private void rescale(List<Object3DData> datas, float newScale, float[] newPosition) {

        // check we have objects to scale, otherwise, there should be an issue with LoaderTask
        if (datas == null || datas.isEmpty()) {
            return;
        }

        Log.d("SceneLoader", "Scaling datas... total: " + datas.size());

        // 抽取数据中第一个数据
        // calculate the global max length
        final Object3DData firstObject = datas.get(0);
        final Dimensions currentDimensions;
        if (this.originalDimensions.containsKey(firstObject)) {
            currentDimensions = this.originalDimensions.get(firstObject);
        } else {
            currentDimensions = firstObject.getCurrentDimensions();
            this.originalDimensions.put(firstObject, currentDimensions);
        }
        Log.v("SceneLoader", "Model[0] dimension: " + currentDimensions.toString());

        final float[] corner01 = currentDimensions.getCornerLeftTopNearVector();
        final float[] corner02 = currentDimensions.getCornerRightBottomFar();
        final float[] center01 = currentDimensions.getCenter();

        float maxLeft = corner01[0]; //最左边顶点坐标
        float maxTop = corner01[1];  //最上边顶点坐标
        float maxNear = corner01[2]; // 最近顶点坐标

        float maxRight = corner02[0];  //最右边顶点坐标
        float maxBottom = corner02[1];  //最
        float maxFar = corner02[2];

        //中心点 坐标
        float maxCenterX = center01[0];
        float maxCenterY = center01[1];
        float maxCenterZ = center01[2];

        for (int i = 1; i < datas.size(); i++) {
            final Object3DData obj = datas.get(i);
            final Dimensions original;

            if (this.originalDimensions.containsKey(obj)) {
                original = this.originalDimensions.get(obj);
                Log.v("SceneLoader", "Found dimension: " + original.toString());
            } else {
                original = obj.getCurrentDimensions();
                this.originalDimensions.put(obj, original);
            }


            Log.v("SceneLoader", "Model[" + i + "] '" + obj.getId() + "' dimension: " + original.toString());

            final float[] corner1 = original.getCornerLeftTopNearVector();
            final float[] corner2 = original.getCornerRightBottomFar();
            final float[] center = original.getCenter();

            float maxLeft2 = corner1[0];
            float maxTop2 = corner1[1];
            float maxNear2 = corner1[2];

            float maxRight2 = corner2[0];
            float maxBottom2 = corner2[1];
            float maxFar2 = corner2[2];

            float centerX = center[0];
            float centerY = center[1];
            float centerZ = center[2];

            // 通过对比 得出最大值
            if (maxRight2 > maxRight) maxRight = maxRight2;
            if (maxLeft2 < maxLeft) maxLeft = maxLeft2;
            if (maxTop2 > maxTop) maxTop = maxTop2;
            if (maxBottom2 < maxBottom) maxBottom = maxBottom2;
            if (maxNear2 > maxNear) maxNear = maxNear2;
            if (maxFar2 < maxFar) maxFar = maxFar2;
            if (maxCenterX < centerX) maxCenterX = centerX;
            if (maxCenterY < centerY) maxCenterY = centerY;
            if (maxCenterZ < centerZ) maxCenterZ = centerZ;
        }

        float lengthX = maxRight - maxLeft; //X轴直径
        float lengthY = maxTop - maxBottom; // Y轴直径
        float lengthZ = maxNear - maxFar;   //Z 轴直径

        float maxLength = lengthX;
        if (lengthY > maxLength) maxLength = lengthY;
        if (lengthZ > maxLength) maxLength = lengthZ;
        Log.v("SceneLoader", "Max length: " + maxLength);

        float maxLocation = 0;
        if (datas.size() > 1) {
            maxLocation = maxCenterX;
            if (maxCenterY > maxLocation) maxLocation = maxCenterY;
            if (maxCenterZ > maxLocation) maxLocation = maxCenterZ;
        }
        Log.v("SceneLoader", "Max location: " + maxLocation);

        // calculate the scale factor
        float scaleFactor = newScale / (maxLength + maxLocation);
        Log.d("SceneLoader", "New scale: " + scaleFactor + " maxLegth + maxLocation =" + maxLength + maxLocation);

        // calculate the global center
        float centerX = (maxRight + maxLeft) / 2;
        float centerY = (maxTop + maxBottom) / 2;
        float centerZ = (maxNear + maxFar) / 2;
        Log.d("SceneLoader", "Total center: " + centerX + "," + centerY + "," + centerZ);

        // calculate the new location
        float translationX = -centerX + newPosition[0];
        float translationY = -centerY + newPosition[1];
        float translationZ = -centerZ + newPosition[2];
        final float[] globalDifference = new float[]{translationX * scaleFactor, translationY * scaleFactor, translationZ * scaleFactor};
        Log.d("SceneLoader", "Total translation: " + Arrays.toString(globalDifference));

        for (Object3DData data : datas) {
            final Transform original;
            if (this.originalTransforms.containsKey(data)) {
                original = this.originalTransforms.get(data);
                Log.v("SceneLoader", "Found transform: " + original);
            } else {
                original = data.getTransform();
                this.originalTransforms.put(data, original);
            }

            //
            float localScaleX = scaleFactor * original.getScale()[0];
            float localScaleY = scaleFactor * original.getScale()[1];
            float localScaleZ = scaleFactor * original.getScale()[2];
            data.setScale(new float[]{localScaleX, localScaleY, localScaleZ});
            Log.v("SceneLoader", "Mew model scale: " + Arrays.toString(data.getScale()));

            // relocate
            float localTranlactionX = original.getLocation()[0] * scaleFactor;
            float localTranlactionY = original.getLocation()[1] * scaleFactor;
            float localTranlactionZ = original.getLocation()[2] * scaleFactor;
            data.setLocation(new float[]{localTranlactionX, localTranlactionY, localTranlactionZ});
            Log.v("SceneLoader", "Mew model location: " + Arrays.toString(data.getLocation()));

            // center
            data.translate(globalDifference);
            Log.v("SceneLoader", "Mew model translated: " + Arrays.toString(data.getLocation()));
        }
    }


}
