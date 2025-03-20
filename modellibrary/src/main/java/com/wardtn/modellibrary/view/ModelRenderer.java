package com.wardtn.modellibrary.view;

import static com.wardtn.modellibrary.util.ModelFileUtilKt.isFileExists;

import android.app.Activity;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.wardtn.modellibrary.animation.Animator;
import com.wardtn.modellibrary.drawer.Renderer;
import com.wardtn.modellibrary.drawer.RendererFactory;
import com.wardtn.modellibrary.model.Camera;
import com.wardtn.modellibrary.model.Element;
import com.wardtn.modellibrary.model.Material;
import com.wardtn.modellibrary.model.Materials;
import com.wardtn.modellibrary.model.Object3DData;
import com.wardtn.modellibrary.objects.Axis;
import com.wardtn.modellibrary.objects.Grid;
import com.wardtn.modellibrary.services.SceneLoader;
import com.wardtn.modellibrary.services.collada.entities.MeshData;
import com.wardtn.modellibrary.services.wavefront.WavefrontMaterialsParser;
import com.wardtn.modellibrary.util.android.ContentUtils;
import com.wardtn.modellibrary.util.android.GLUtil;
import com.wardtn.modellibrary.util.event.EventListener;
import com.wardtn.modellibrary.util.io.IOUtils;
import com.wardtn.modellibrary.util.AndroidUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ModelRenderer implements GLSurfaceView.Renderer {


    public static class ViewEvent extends EventObject {

        private final Code code;
        private final int width;
        private final int height;

        public enum Code {SURFACE_CREATED, SURFACE_CHANGED}

        public ViewEvent(Object source, Code code, int width, int height) {
            super(source);
            this.code = code;
            this.width = width;
            this.height = height;
        }

        public Code getCode() {
            return code;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }


    private final static String TAG = ModelRenderer.class.getSimpleName();

    // 网格参数
    private static final float GRID_WIDTH = 100f;
    private static final float GRID_SIZE = 10f;
    private static final float[] GRID_COLOR = {0.25f, 0.25f, 0.25f, 0.5f};

    // 默认遮罩
    private static final float[] BLENDING_MASK_DEFAULT = {1.0f, 1.0f, 1.0f, 1.0f};

    // 半透明遮罩
    private static final float[] BLENDING_MASK_FORCED = {1.0f, 1.0f, 1.0f, 0.5f};

    // 控制相机的可视范围
    // 视锥体近平面
    private static final float near = 1.0f;
    // 视锥体源远平面
    private static final float far = 5000f;

    private final float[] backgroundColor;
    private final SceneLoader scene;

    private final List<EventListener> listeners = new ArrayList<>();

    // 3D window (parent component)
    private GLSurfaceView main;

    // width of the screen
    private int width;

    // height of the screen
    private int height;

    /**
     * Drawer factory to get right renderer/shader based on object attributes
     */
    private final RendererFactory drawer;

    private Map<Object, Integer> textures = new HashMap<>();


    private boolean debugSkeleton = false;


    // 3D matrices to project our 3D world
    private final float[] viewMatrix = new float[16];  //摄像机位置朝向9参数矩阵
    private final float[] projectionMatrix = new float[16];  //4x4矩阵 投影用
    private float[] currentMatrix = new float[16];

    // light
    private final float[] lightPosInWorldSpace = new float[3];

    private final float[] cameraPosInWorldSpace = new float[3];

    // Decoration
    private final List<Object3DData> extras = new ArrayList<>();
    private final Object3DData axis = Axis.build().setId("axis").setSolid(false).setScale(new float[]{50, 50, 50});
    private final Object3DData gridx = Grid.build(-GRID_WIDTH, 0f, -GRID_WIDTH, GRID_WIDTH, 0f, GRID_WIDTH, GRID_SIZE).setColor(GRID_COLOR).setId("grid-x").setSolid(false);
    private final Object3DData gridy = Grid.build(-GRID_WIDTH, -GRID_WIDTH, 0f, GRID_WIDTH, GRID_WIDTH, 0f, GRID_SIZE).setColor(GRID_COLOR).setId("grid-y").setSolid(false);
    private final Object3DData gridz = Grid.build(0, -GRID_WIDTH, -GRID_WIDTH, 0, GRID_WIDTH, GRID_WIDTH, GRID_SIZE).setColor(GRID_COLOR).setId("grid-z").setSolid(false);

    {
        extras.add(axis);
        extras.add(gridx);
        extras.add(gridy);
        extras.add(gridz);
    }


    // settings
    private boolean lightsEnabled = true;
    private boolean wireframeEnabled = false;
    private boolean texturesEnabled = true;
    private boolean colorsEnabled = true;
    private boolean animationEnabled = true;

    /**
     * Whether the info of the model has been written to console log
     */
    private Map<String, Boolean> infoLogged = new HashMap<>();
    /**
     * Switch to akternate drawing of right and left image
     */
    private boolean anaglyphSwitch = false;

    /**
     * Skeleton Animator
     */
    private Animator animator = new Animator();
    /**
     * Did the application explode?
     */
    private boolean fatalException = false;

    /**
     * Construct a new renderer for the specified surface view
     *
     * @param modelSurfaceView the 3D window
     */
    public ModelRenderer(Activity parent, ModelSurfaceView modelSurfaceView, float[] backgroundColor, SceneLoader scene) throws IOException, IllegalAccessException {
        this.main = modelSurfaceView;
        this.backgroundColor = backgroundColor;
        this.scene = scene;
        this.drawer = new RendererFactory(parent);
    }

    public ModelRenderer addListener(EventListener listener) {
        this.listeners.add(listener);
        return this;
    }

    public float getNear() {
        return near;
    }

    public float getFar() {
        return far;
    }

    public boolean isLightsEnabled() {
        return lightsEnabled;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // log event
        Log.d(TAG, "onSurfaceCreated. config: " + config);

        GLES20.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        ContentUtils.setThreadActivity(main.getContext());
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        Log.e("CHEN", "当前 Width =" + width + " Height " + height);
        this.width = width;
        this.height = height;

        // Adjust the viewport based on geometry changes, such as screen rotation
        GLES20.glViewport(0, 0, width, height);
        final float aspectRatio = (float) width / height;

        Log.e("CHEN", "当前ratio = " + aspectRatio);

        Matrix.frustumM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, 2f, 5000f);

        AndroidUtils.fireEvent(listeners, new ViewEvent(this, ViewEvent.Code.SURFACE_CHANGED, width, height));
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        if (fatalException) {
            return;
        }
        try {
            GLES20.glViewport(0, 0, width, height);  // 设置视图大小
            GLES20.glScissor(0, 0, width, height);  // 设置裁剪窗口

            //  使用 GLES20.glClear 清除颜色缓冲区和深度缓冲区。
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            if (scene == null) {
                // scene not ready
                return;
            }

            float[] colorMask = BLENDING_MASK_DEFAULT;

            //设置混合模式
            if (scene.isBlendingEnabled()) {
                // Enable blending for combining colors when there is transparency
                GLES20.glEnable(GLES20.GL_BLEND);
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                if (scene.isBlendingForced()) {
                    colorMask = BLENDING_MASK_FORCED;
                }
            } else {
                GLES20.glDisable(GLES20.GL_BLEND);
            }

            // 从 SceneLoader 中获取当前相机对象，并获取相机的位置和旋转角度
            Camera camera = scene.getCamera();

            cameraPosInWorldSpace[0] = camera.getxPos();  //0
            cameraPosInWorldSpace[1] = camera.getyPos();  //0
            cameraPosInWorldSpace[2] = camera.getzPos();  //1

            float angleX = camera.rotateAngleX;
            float angleY = camera.rotateAngleY;

            if (camera.hasChanged()) {
                if (!scene.isStereoscopic()) {
                    // 更新视图矩阵
                    // 世界坐标系转换为相机坐标系
                    Matrix.setLookAtM(viewMatrix, 0, camera.getxPos(), camera.getyPos(), camera.getzPos(), camera.getxView(), camera.getyView(), camera.getzView(), camera.getxUp(), camera.getyUp(), camera.getzUp());
                    // 绕 X 轴旋转 angleX 度
                    Matrix.rotateM(viewMatrix, 0, angleX, 1f, 0f, 0f);
                    // 绕 Y 轴旋转 angleY 度
                    Matrix.rotateM(viewMatrix, 0, angleY, 0f, 1f, 0f);
                    Matrix.multiplyMM(currentMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
                }
                camera.setChanged(false);
            }

            if (!scene.isStereoscopic()) {
                this.onDrawFrame(viewMatrix, projectionMatrix, currentMatrix, lightPosInWorldSpace, colorMask, cameraPosInWorldSpace);
                return;
            }
        } catch (Exception ex) {
            Log.e("ModelRenderer", "Fatal exception: " + ex.getMessage(), ex);
            fatalException = true;
        } catch (Error err) {
            Log.e("ModelRenderer", "Fatal error: " + err.getMessage(), err);
            fatalException = true;
        }
    }

    private void onDrawFrame(float[] viewMatrix, float[] projectionMatrix, float[] viewProjectionMatrix, float[] lightPosInWorldSpace, float[] colorMask, float[] cameraPosInWorldSpace) {

        // draw light
        boolean doAnimation = scene.isDoAnimation() && animationEnabled;
        boolean drawLighting = scene.isDrawLighting() && isLightsEnabled();
        boolean drawWireframe = scene.isDrawWireframe() || wireframeEnabled;
        boolean drawTextures = scene.isDrawTextures() && texturesEnabled;
        boolean drawColors = scene.isDrawColors() && colorsEnabled;

        List<Object3DData> objects = scene.getObjects();
        for (int i = 0; i < objects.size(); i++) {
            drawObject(viewMatrix, projectionMatrix, lightPosInWorldSpace, colorMask, cameraPosInWorldSpace, doAnimation, drawLighting, drawWireframe, drawTextures, drawColors, objects, i);
        }

        debugSkeleton = !debugSkeleton;
    }

    private void drawObject(float[] viewMatrix, float[] projectionMatrix, float[] lightPosInWorldSpace, float[] colorMask, float[] cameraPosInWorldSpace, boolean doAnimation, boolean drawLighting, boolean drawWireframe, boolean drawTextures, boolean drawColors, List<Object3DData> objects, int i) {
        Object3DData objData = null;
        try {
            objData = objects.get(i);

            Renderer drawerObject = drawer.getDrawer(objData, false, drawTextures, drawLighting, doAnimation, drawColors);

            objData.setChanged(false);

            //切换纹理
//            if (isChangeTexture) {
//                // 删除纹理
//                GLUtil.deleteTexture();
//                List<Element> elements = loadMaterials(objData.getMeshData(), changeObj, changeJpg);
//                objData.setTetxurePath(changeJpg);
//                objData.setElements(elements);
//                isChangeTexture = false;
//            }
//
//            //还原3D
//            if (isReset) {
//                scene.reset3D();
//                isReset = false;
//            }


            // load textures
            Integer textureId = null;
            Integer overlayTextureId = -1;


            // 首次不添加标注,这个时候要创建一个标注纹理
            // 首次添加标注,则算切换纹理
            // 显示和替换标注

            if (objData.getElements() == null) return;

            for (int e = 0; e < objData.getElements().size(); e++) {
                Element element = objData.getElements().get(e);
                // check required info
                if (element.getMaterial() == null || element.getMaterial().getTextureData() == null)
                    continue;

                // 加载基础纹理
                textureId = textures.get(element.getMaterial().getTextureData());

                int markTextureId = -1;
                if (textures.get(element.getMaterial().getOverlayData()) != null) {
                    markTextureId = textures.get(element.getMaterial().getOverlayData());
                }


                // 用户选择点击显示标注 或者隐藏标注
                if (overlayTextureId != null) {
                    // 用户点击取消标注 传回去-1
                    if (isShowMarkTexture) {
                        element.getMaterial().setOverlayTextureId(markTextureId);
                    } else {
                        element.getMaterial().setOverlayTextureId(-1);
                    }
                }

                // 切换标注纹理
                if (isChangeMarkTexture && isFileExists(changeOverlayPath)) {
                    try (InputStream stream = new FileInputStream(changeOverlayPath)) {
                        // read data
                        objData.getMaterial().setOverlayData(IOUtils.read(stream));
                        if (overlayTextureId != -1) {
                            GLUtil.deleteTexture1();
                        }
                        overlayTextureId = GLUtil.loadTexture1(objData.getMaterial().getOverlayData());
                        // cache texture
                        textures.put(element.getMaterial().getOverlayData(), overlayTextureId);
                        isChangeMarkTexture = false;
                        isShowMarkTexture = true;
                    } catch (Exception ex) {
                        Log.e("ModelRenderer", String.format("Error reading texture file: %s", ex.getMessage()));
                    }
                }

                // 纹理存在后不执行
                if (textureId != null) continue;

                // 加载基础纹理
                // bind texture
                Log.i("ModelRenderer", "Loaded top textureId texture OK. id: " + textureId);
                textureId = GLUtil.loadTexture(element.getMaterial().getTextureData());
                element.getMaterial().setTextureId(textureId);
                // cache texture
                textures.put(element.getMaterial().getTextureData(), textureId);
                objData.setTextureData(element.getMaterial().getTextureData());

                // 如果首次存在标注纹理 则一起显示
                overlayTextureId = textures.get(element.getMaterial().getOverlayData());
                if (overlayTextureId != null) {
                    continue;
                }
                Log.i("ModelRenderer", "Loaded top overlayTextureId texture OK. id: " + overlayTextureId);
                // bind texture
                overlayTextureId = GLUtil.loadTexture1(element.getMaterial().getOverlayData());
                if (overlayTextureId == null) {
                    isShowMarkTexture = false;
                } else {
                    isShowMarkTexture = true;
                }
                element.getMaterial().setOverlayTextureId(overlayTextureId);
                // cache texture
                textures.put(element.getMaterial().getOverlayData(), overlayTextureId);
                objData.setOverLayTextureData(element.getMaterial().getOverlayData());

            }
            if (textureId == null) {
                textureId = -1;
            }
            drawerObject.draw(objData, projectionMatrix, viewMatrix, textureId, lightPosInWorldSpace, colorMask, cameraPosInWorldSpace);
        } catch (Exception ex) {
            if (!infoLogged.containsKey(ex.getMessage())) {
                Log.e("ModelRenderer", "There was a problem rendering the object '" + objData.getId() + "':" + ex.getMessage(), ex);
                infoLogged.put(ex.getMessage(), true);
            }
        } catch (Error ex) {
            Log.e("ModelRenderer", "There was a problem rendering the object '" + objData.getId() + "':" + ex.getMessage(), ex);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float[] getProjectionMatrix() {
        return projectionMatrix;
    }

    public float[] getViewMatrix() {
        return viewMatrix;
    }

    /**
     * 更换纹理
     *
     * @param meshData
     * @param resources
     * @param mtlPath
     * @param texturePath
     * @return
     */

    private boolean isChangeTexture;
    private boolean isShowMarkTexture = false; //是否展现Mark纹理
    private boolean isChangeMarkTexture; //是否更换标记纹理

    private String changeObj, changeJpg;
    private String changeOverlayPath;

    private boolean isReset;

    public void setChangeTexture(String obj, String jpg) {
        isChangeTexture = true;
        changeObj = obj;
        changeJpg = jpg;
    }

    public void isShowMarkTexure() {
        isShowMarkTexture = !isShowMarkTexture;
    }

    public void setChangeMarkTexture(String markPath) {
        isChangeMarkTexture = true;
        changeOverlayPath = markPath;
    }


    public void setReset() {
        isReset = true;
    }


    public static List<Element> loadMaterials(MeshData meshData, String mtlPath, String texturePath) {
        // process materials
        if (meshData.getMaterialFile() == null) return meshData.getElements();
        List<Element> elements = meshData.getElements();
        // log event
        Log.i("WavefrontLoader", "--------------------------------------------------");
        Log.i("WavefrontLoader", "Parsing materials... ");
        Log.i("WavefrontLoader", "--------------------------------------------------");
        try {
            // get materials stream
//            final InputStream inputStream = ContentUtils.getInputStream(meshData.getMaterialFile());
            final InputStream inputStream = new FileInputStream(mtlPath);

            // 解析材质
            final WavefrontMaterialsParser materialsParser = new WavefrontMaterialsParser();
            final Materials materials = materialsParser.parse(mtlPath, inputStream);

            // check if there is any material
            if (materials.size() > 0) {
                // bind materials
                for (int e = 0; e < elements.size(); e++) {
                    // get element
                    final Element element = elements.get(e);
                    // log event
                    Log.i("WavefrontLoader", "Render Processing element... " + element.getId());
                    // get material id
                    final String elementMaterialId = element.getMaterialId();
                    // check if element has material
                    if (elementMaterialId != null && materials.contains(elementMaterialId)) {
                        // get material for element
                        final Material elementMaterial = materials.get(elementMaterialId);
                        // bind material
                        element.setMaterial(elementMaterial);
                        // check if element has texture mapped
                        if (elementMaterial.getTextureFile() != null) {
                            // log event
                            Log.i("WavefrontLoader", "Reading texture file... " + elementMaterial.getTextureFile());
                            try (InputStream stream = new FileInputStream(texturePath)) {
                                // read data
                                elementMaterial.setTextureData(IOUtils.read(stream));
                                // log event
                                Log.i("WavefrontLoader", "Texture linked... " + element.getMaterial().getTextureFile());
                            } catch (Exception ex) {
                                Log.e("WavefrontLoader", String.format("Error reading texture file: %s", ex.getMessage()));
                            }

                        }
                    }
                }
            }
        } catch (IOException ex) {
            Log.e("WavefrontLoader", "Error loading materials... " + meshData.getMaterialFile());
        }
        return elements;
    }
}