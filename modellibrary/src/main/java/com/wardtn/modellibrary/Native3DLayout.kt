package com.wardtn.modellibrary

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.wardtn.modellibrary.view.ModelRenderer
import com.wardtn.modellibrary.camera.CameraController
import com.wardtn.modellibrary.controller.TouchController
import com.wardtn.modellibrary.services.SceneLoader
import com.wardtn.modellibrary.view.ModelSurfaceView
import com.wardtn.modellibrary.util.event.EventListener

import java.util.*

/**
 * 原生3D组件
 */
class Native3DLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr),
    EventListener, SceneLoader.LoadListener {
    private var modelSurfaceView: ModelSurfaceView? = null
    private var touchController: TouchController? = null
    private var cameraController: CameraController? = null
    private var frameLayout: FrameLayout
    private var sceneLoader: SceneLoader? = null
    private val backgroundColor = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
    private var userID: String? = null
    private var testID: String? = null



    private var isload3D = false
    fun set3DActivity(activity: Activity) {
        sceneLoader =
            SceneLoader(activity, null, 0)
        sceneLoader?.setLoadListener(this)
        try {
            touchController = TouchController(activity)
            touchController!!.addListener(this)
        } catch (e: Exception) {
            Log.e("CHEN", "初始化失败", e);
        }

        try {
            cameraController =
                CameraController(sceneLoader!!.camera)
            touchController!!.addListener(cameraController)
        } catch (e: Exception) {
            Log.e("CHEN", "初始化失败", e)
        }
    }

    fun getRenderer(): ModelRenderer? {
        return modelSurfaceView?.modelRenderer
    }


    fun initData(
        activity: Activity?,
        objPath: String,
    ) {
        if (frameLayout.childCount > 0) {
            Log.e("CHEN", "当前已经存在 SurfaceView")
            return
        }
        //开始加载
        sceneLoader?.startLoad(objPath,  1, false)
        isload3D = true

        try {
            modelSurfaceView = ModelSurfaceView(
                activity,
                backgroundColor,
                sceneLoader)
            modelSurfaceView!!.addListener(this)
            frameLayout.addView(modelSurfaceView)
            modelSurfaceView!!.modelRenderer.addListener(cameraController)
        } catch (e: Exception) {
           Log.e("CHEN", "初始化失败", e);
        }


//        if (needChangeTexure == true) {
//            var mtlPath = get3DDownloadPath() + "/blackAWhite.mtl"
//            var imgPath = get3DDownloadPath() + "/" + "blackAWhite"
//            changeTexture(mtlPath, imgPath)
//        }

        this.userID = userID
        this.testID = testID
    }

//    /**
//     * 在展现3D之前提前加载3D顶点数据
//     */
//    fun preload3DData(userID: String, testID: String) {
//        this.userID = userID
//        this.testID = testID
//        Log.e("CHEN", "提前加载3D顶点数据")
//
//
//        var objPath = get3DDownloadPath(userID, testID) + File.separator + Constant.COPY_OBJ_NAME
//        sceneLoader?.startLoad(objPath, userID, testID, Constant.WHITE_LIGHT, true)
//    }


    /**
     * 更换纹理
     */
    fun changeTexture(mtl: String, img: String) {
        if (!isload3D) {
            Log.e("CHEN","当前还未展示3D 无法切换纹理");
            return
        }
        modelSurfaceView!!.modelRenderer.setChangeTexture(mtl, img)
        modelSurfaceView!!.modelRenderer.setReset()
    }

    override fun onEvent(event: EventObject): Boolean {
        if (event is ModelRenderer.ViewEvent) {
            if (event.code == ModelRenderer.ViewEvent.Code.SURFACE_CHANGED) {
                touchController!!.setSize(event.width, event.height)
                modelSurfaceView!!.setTouchController(touchController)
            }
        }
        return true
    }

    init {
        Constant.DENSITY = resources.displayMetrics.density.toInt()
        val view = LayoutInflater.from(getContext()).inflate(
            R.layout.layout_native_3d,
            this, true
        )
        frameLayout = view.findViewById(R.id.frame)
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility == VISIBLE) {
            Log.e("CHEN", "Native 3D Visible")
        } else {
            modelSurfaceView?.onPause()
            frameLayout.removeAllViews()
        }
    }

    fun clearView() {
        modelSurfaceView?.onPause()
        frameLayout?.removeAllViews()
    }

    fun clearCache() {
        clearView()
        sceneLoader = null
        touchController = null
        cameraController = null
    }

    override fun load3DSuc() {
//        aliyunOssUrlBean?.let {
//            if (Pre3DLoadManager.checkRGBAllGenerate(userID!!, testID!!)) {
//                LoggerUtil.dq_log("当前 RGB 文件已经全部生成")
//                return
//            }
//            LoggerUtil.dq_log("开始生成RGB 3D 文件")
//            Utils.onRunRgbTransformerBitmap(
//                it.whiteLightBean?.faceUrl,
//                it.whiteLightBean?.leftUrl,
//                it.whiteLightBean?.rightUrl,
//                userID, testID
//            )
//        }
    }
}