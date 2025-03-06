package com.wardtn.modellibrary.compare;

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
import com.wardtn.modellibrary.services.collada.entities.MeshData;
import com.wardtn.modellibrary.services.wavefront.WavefrontMaterialsParser;
import com.wardtn.modellibrary.util.AndroidUtils;
import com.wardtn.modellibrary.util.android.ContentUtils;
import com.wardtn.modellibrary.util.android.GLUtil;
import com.wardtn.modellibrary.util.event.EventListener;
import com.wardtn.modellibrary.util.io.IOUtils;
import com.wardtn.modellibrary.view.ModelRenderer;

import java.io.ByteArrayInputStream;
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

import static javax.microedition.khronos.opengles.GL10.GL_DEPTH_BUFFER_BIT;

public class CompareRender implements GLSurfaceView.Renderer {

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


    private final static String TAG = CompareRender.class.getSimpleName();

    // grid
    private static final float GRID_WIDTH = 100f;
    private static final float GRID_SIZE = 10f;
    private static final float[] GRID_COLOR = {0.25f, 0.25f, 0.25f, 0.5f};

    // blending
    private static final float[] BLENDING_MASK_DEFAULT = {1.0f, 1.0f, 1.0f, 1.0f};
    // Add 0.5f to the alpha component to the global shader so we can see through the skin
    private static final float[] BLENDING_MASK_FORCED = {1.0f, 1.0f, 1.0f, 0.5f};

    // frustrum - nearest pixel
    private static final float near = 1.0f;
    // frustrum - fartest pixel
    private static final float far = 5000f;

    // stereoscopic variables
    private static float EYE_DISTANCE = 0.64f;
    private static final float[] COLOR_RED = {1.0f, 0.0f, 0.0f, 1f};
    private static final float[] COLOR_BLUE = {0.0f, 1.0f, 0.0f, 1f};
    private static final float[] COLOR_WHITE = {1f, 1f, 1f, 1f};
    private static final float[] COLOR_HALF_TRANSPARENT = {1f, 1f, 1f, 0.5f};
    private static final float[] COLOR_ALMOST_TRANSPARENT = {1f, 1f, 1f, 0.1f};

    private final float[] backgroundColor;
    private final CompareSceneLoader scene;

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


    // The wireframe associated shape (it should be made of lines only)
    private Map<Object3DData, Object3DData> wireframes = new HashMap<>();
    // The loaded textures
    private Map<Object, Integer> textures = new HashMap<>();
    // The corresponding opengl bounding boxes and drawer
    private Map<Object3DData, Object3DData> boundingBoxes = new HashMap<>();
    // The corresponding opengl bounding boxes
    private Map<Object3DData, Object3DData> normals = new HashMap<>();

    // skeleton
    private Map<Object3DData, Object3DData> skeleton = new HashMap<>();
    private boolean debugSkeleton = false;


    // 3D matrices to project our 3D world
    private final float[] viewMatrix = new float[16];  //摄像机位置朝向9参数矩阵
    private final float[] projectionMatrix = new float[16];  //4x4矩阵 投影用
    private float[] currentMatrix = new float[16];

    // light
    private final float[] tempVector4 = new float[4];
    private final float[] lightPosInWorldSpace = new float[3];
    private final float[] cameraPosInWorldSpace = new float[3];
    private final float[] lightPosition = new float[]{0, 0, 0, 1};

    // Decoration
    private final List<Object3DData> extras = new ArrayList<>();
    private final Object3DData axis = Axis.build().setId("axis").setSolid(false).setScale(new float[]{50, 50, 50});
    private final Object3DData gridx = Grid.build(-GRID_WIDTH, 0f, -GRID_WIDTH, GRID_WIDTH, 0f, GRID_WIDTH, GRID_SIZE)
            .setColor(GRID_COLOR).setId("grid-x").setSolid(false);
    private final Object3DData gridy = Grid.build(-GRID_WIDTH, -GRID_WIDTH, 0f, GRID_WIDTH, GRID_WIDTH, 0f, GRID_SIZE)
            .setColor(GRID_COLOR).setId("grid-y").setSolid(false);
    private final Object3DData gridz = Grid.build(0, -GRID_WIDTH, -GRID_WIDTH, 0, GRID_WIDTH, GRID_WIDTH, GRID_SIZE)
            .setColor(GRID_COLOR).setId("grid-z").setSolid(false);

    {
        extras.add(axis);
        extras.add(gridx);
        extras.add(gridy);
        extras.add(gridz);
    }

    // 3D stereoscopic matrix (left & right camera)
    private final float[] viewMatrixLeft = new float[16];
    private final float[] projectionMatrixLeft = new float[16];
    private final float[] viewProjectionMatrixLeft = new float[16];
    private final float[] viewMatrixRight = new float[16];
    private final float[] projectionMatrixRight = new float[16];
    private final float[] viewProjectionMatrixRight = new float[16];

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
    public CompareRender(Activity parent, CompareModelSurfaceView modelSurfaceView,
                         float[] backgroundColor, CompareSceneLoader scene, DividerChangeListener listener) throws IOException, IllegalAccessException {
        this.main = modelSurfaceView;
        this.backgroundColor = backgroundColor;
        this.scene = scene;
        this.drawer = new RendererFactory(parent);
        this.listener = listener;
    }

    public CompareRender addListener(EventListener listener) {
        this.listeners.add(listener);
        return this;
    }

    public float getNear() {
        return near;
    }

    public float getFar() {
        return far;
    }

    public void toggleLights() {
        lightsEnabled = !lightsEnabled;
    }

    public void toggleSkyBox() {
//        isUseskyBoxId++;
//        if (isUseskyBoxId > 1) {
//            isUseskyBoxId = -3;
//        }
//        Log.i("ModelRenderer", "Toggled skybox. Idx: " + isUseskyBoxId);
    }

    public boolean isLightsEnabled() {
        return lightsEnabled;
    }

    public void toggleWireframe() {
        this.wireframeEnabled = !wireframeEnabled;
    }

    public void toggleTextures() {
        this.texturesEnabled = !texturesEnabled;
    }

    public void toggleColors() {
        this.colorsEnabled = !colorsEnabled;
    }

    public void toggleAnimation() {
        this.animationEnabled = !animationEnabled;
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

    private int scissorWidth = 0;

    public void setScissor(int scissorWidth) {
        this.scissorWidth = scissorWidth;
        setDivider();
    }


    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        Log.e("CHEN", "当前 Width =" + width + " Height " + height);
        this.width = width;
        this.height = height;
        if (listener != null) {
            listener.feedBackWidth(width);
        }
        if (scissorWidth == 0) {
            scissorWidth = width / 2;
            setDivider();
        }


        // Adjust the viewport based on geometry changes, such as screen rotation
        GLES20.glViewport(0, 0, width, height);
        final float aspectRatio = (float) width / height;
        Matrix.frustumM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, 2f, 5000f);
        AndroidUtils.fireEvent(listeners, new ModelRenderer.ViewEvent(this, ModelRenderer.ViewEvent.Code.SURFACE_CHANGED, width, height));
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        if (fatalException) {
            return;
        }
        try {
            GLES20.glViewport(0, 0, width, height);  // 设置视图大小
            GLES20.glScissor(0, 0, width, height);  // 设置裁剪窗口

            // Draw background color
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            if (scene == null) {
                // scene not ready
                return;
            }

            float[] colorMask = BLENDING_MASK_DEFAULT;
            //混合
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

            // recalculate mvp matrix according to where we are looking at now
            Camera camera = scene.getCamera();
            cameraPosInWorldSpace[0] = camera.getxPos();  //0
            cameraPosInWorldSpace[1] = camera.getyPos();  //0
            cameraPosInWorldSpace[2] = camera.getzPos();  //1

            float angleX = camera.rotateAngleX;
            float angleY = camera.rotateAngleY;


            if (camera.hasChanged()) {
                if (!scene.isStereoscopic()) {
                    Matrix.setLookAtM(viewMatrix, 0, camera.getxPos(), camera.getyPos(), camera.getzPos(), camera.getxView(), camera.getyView(), camera.getzView(), camera.getxUp(), camera.getyUp(), camera.getzUp());
                    Matrix.rotateM(viewMatrix, 0, angleX, 1f, 0f, 0f);
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

    private void onDrawFrame(float[] viewMatrix, float[] projectionMatrix, float[] viewProjectionMatrix,
                             float[] lightPosInWorldSpace, float[] colorMask, float[] cameraPosInWorldSpace) {

        // draw light
        boolean doAnimation = scene.isDoAnimation() && animationEnabled;
        boolean drawLighting = scene.isDrawLighting() && isLightsEnabled();
        boolean drawWireframe = scene.isDrawWireframe() || wireframeEnabled;
        boolean drawTextures = scene.isDrawTextures() && texturesEnabled;
        boolean drawColors = scene.isDrawColors() && colorsEnabled;

        List<Object3DData> objects = scene.getObjects();

        if (objects != null && objects.size() == 2) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glClear(GL_DEPTH_BUFFER_BIT);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            drawObject(viewMatrix, projectionMatrix, lightPosInWorldSpace, colorMask, cameraPosInWorldSpace, doAnimation, drawLighting, drawWireframe, drawTextures, drawColors, objects, 0);
            clearPart();
            drawObject(viewMatrix, projectionMatrix, lightPosInWorldSpace, colorMask, cameraPosInWorldSpace, doAnimation, drawLighting, drawWireframe, drawTextures, drawColors, objects, 1);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        }

        debugSkeleton = !debugSkeleton;
    }

    private void clearPart() {
        GLES20.glScissor(scissorWidth, 0, width, height);
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }


    private void drawObject(float[] viewMatrix, float[] projectionMatrix, float[] lightPosInWorldSpace, float[] colorMask, float[] cameraPosInWorldSpace, boolean doAnimation, boolean drawLighting, boolean drawWireframe, boolean drawTextures, boolean drawColors, List<Object3DData> objects, int i) {
        Object3DData objData = null;
        try {
            objData = objects.get(i);
            if (!objData.isVisible()) {
                return;
            }

            if (!infoLogged.containsKey(objData.getId())) {
                Log.i("ModelRenderer", "Drawing model: " + objData.getId() + ", " + objData.getClass().getSimpleName());
                infoLogged.put(objData.getId(), true);
            }


            Renderer drawerObject = drawer.getDrawer(objData, false, drawTextures, drawLighting, doAnimation, drawColors);
            if (drawerObject == null) {
                if (!infoLogged.containsKey(objData.getId() + "drawer")) {
                    Log.e("ModelRenderer", "No drawer for " + objData.getId());
                    infoLogged.put(objData.getId() + "drawer", true);
                }
                return;
            }

            objData.setChanged(false);

            //更换纹理
            resetTexture(i, objData);

            //还原3D
            if (isReset) {
                scene.reset3D();
                isReset = false;
            }


            // load textures
            Integer textureId = null;
            if (drawTextures) {
                // TODO: move texture loading to Renderer
                if (objData.getElements() != null) {
                    for (int e = 0; e < objData.getElements().size(); e++) {
                        Element element = objData.getElements().get(e);
                        // check required info
                        if (element.getMaterial() == null || element.getMaterial().getTextureData() == null)
                            continue;
                        // check if texture is already binded
                        textureId = textures.get(element.getMaterial().getTextureData());
                        if (textureId != null) continue;
                        // bind texture
                        Log.i("ModelRenderer", "Loading material texture for element... '" + element);

                        switch (i) {
                            case 0:
                                textureId = GLUtil.loadTexture(element.getMaterial().getTextureData());
                                break;
                            case 1:
                                textureId = GLUtil.loadTexture1(element.getMaterial().getTextureData());
                                break;
                        }

//                        textureId = GLUtil.loadTexture(element.getMaterial().getTextureData());
                        element.getMaterial().setTextureId(textureId);
                        // cache texture
                        textures.put(element.getMaterial().getTextureData(), textureId);

                        // log event
                        Log.i("ModelRenderer", "Loaded material texture for element. id: " + textureId);

                        // FIXME: we have to set this, otherwise the RendererFactory won't return textured shader
                        objData.setTextureData(element.getMaterial().getTextureData());
                    }
                } else {
                    textureId = textures.get(objData.getTextureData());
                    if (textureId == null && objData.getTextureData() != null) {
                        Log.i("ModelRenderer", "Loading texture for obj: '" + objData.getId() + "'... bytes: " + objData.getTextureData().length);
                        ByteArrayInputStream textureIs = new ByteArrayInputStream(objData.getTextureData());
                        textureId = GLUtil.loadTexture(textureIs);
                        textureIs.close();
                        textures.put(objData.getTextureData(), textureId);
                        objData.getMaterial().setTextureId(textureId);

                        Log.i("ModelRenderer", "Loaded texture OK. id: " + textureId);
                    }
                }
            }
            if (textureId == null) {
                textureId = -1;
            }

            // draw points
            if (objData.getDrawMode() == GLES20.GL_POINTS) {
                Renderer basicDrawer = drawer.getBasicShader();
                basicDrawer.draw(objData, projectionMatrix, viewMatrix, GLES20.GL_POINTS, lightPosInWorldSpace, cameraPosInWorldSpace);
            } else {
                drawerObject.draw(objData, projectionMatrix, viewMatrix,
                        textureId, lightPosInWorldSpace, colorMask, cameraPosInWorldSpace);
                objData.render(drawer, lightPosInWorldSpace, colorMask);
            }
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

    //    "green.mtl", "green.jpg");
    private boolean isChangeTexture0, isChangeTexture1;
    private String firstChangeObj, firstChangeJpg;
    private String secondChangeObj, secondChangeJpg;

    private boolean isReset;

    public void setChangeTexture(String firstObj, String firstJpg, String secondObj, String secondJpg) {
        isChangeTexture0 = true;
        isChangeTexture1 = true;
        firstChangeObj = firstObj;
        firstChangeJpg = firstJpg;
        secondChangeObj = secondObj;
        secondChangeJpg = secondJpg;
    }

    public void setReset() {
        isReset = true;
    }


    public Object3DData resetTexture(int index, Object3DData object3DData) {

        String obj = "", jpg = "";
        switch (index) {
            case 0:
                if (!isChangeTexture0) return object3DData;
                GLUtil.deleteTexture();
                obj = firstChangeObj;
                jpg = firstChangeJpg;
                break;
            case 1:
                if (!isChangeTexture1) return object3DData;
                GLUtil.deleteTexture1();
                obj = secondChangeObj;
                jpg = secondChangeJpg;
                break;
        }
        List<Element> elements = loadMaterials(object3DData.getMeshData(), obj, jpg);
        object3DData.setTetxurePath(jpg);
        object3DData.setElements(elements);
        switch (index) {
            case 0:
                isChangeTexture0 = false;
                break;
            case 1:
                isChangeTexture1 = false;
                break;
        }
        return object3DData;
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

            // parse materials
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

    public void setDivider() {
        if (listener != null) {
            listener.dividerChange(scissorWidth);
        }
    }

    private DividerChangeListener listener;

    public interface DividerChangeListener {
        void dividerChange(int width);

        void feedBackWidth(int surfaceWidth);
    }

}