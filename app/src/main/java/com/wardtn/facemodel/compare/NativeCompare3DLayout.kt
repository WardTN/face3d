package com.wardtn.facemodel.compare

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import com.wardtn.facemodel.Constant
import com.wardtn.facemodel.R
import com.wardtn.facemodel.camera.CameraController
import com.wardtn.facemodel.compare.CompareRender.DividerChangeListener
import com.wardtn.facemodel.controller.TouchController
import com.wardtn.facemodel.services.SceneLoader
import com.wardtn.facemodel.util.event.EventListener
import com.wardtn.facemodel.view.ModelRenderer
import java.util.*

/**
 * 原生本地3D对比组件
 */
class NativeCompare3DLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), EventListener, SceneLoader.LoadListener,
    DividerChangeListener, CompareSeekBar.ComPareSeekListener {

    private var modelSurfaceView: CompareModelSurfaceView? = null
    private var sceneLoader: CompareSceneLoader? = null
    private var touchController: TouchController? = null
    private var cameraController: CameraController? = null
    private var frameLayout: FrameLayout
    private val backgroundColor = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)

    private var seekBar: CompareSeekBar? = null


    init {
        Constant.DENSITY = resources.displayMetrics.density.toInt()
        val view = LayoutInflater.from(getContext()).inflate(
            R.layout.layout_native_3d,
            this, true
        )
        frameLayout = view.findViewById(R.id.frame)
        seekBar = findViewById(R.id.comparebar)
        seekBar?.visibility = VISIBLE
        seekBar?.listener = this
//        findViewById<Button>(R.id.changeTexture).setOnClickListener {
////            var objPath =
////                "/storage/emulated/0/Android/data/com.solex.skinhd/files/BallSkindev/Assets/www/file/272/1007655732792922112/solexCopy.obj"
//            var customId = "272"
//            var testId = "1007655732792922112"
//
//
////            var objPath2 =
////                "/storage/emulated/0/Android/data/com.solex.skinhd/files/BallSkindev/Assets/www/file/266/1008683087451787265/solexCopy.obj"
//            var customId2 = "266"
//            var testId2 = "1008683087451787265"
//
//
//            var mtlPath = get3DDownloadPath(customId, testId) + "/${Constant.SOLEX}.mtl"
//            var imgPath =
//                get3DDownloadPath(customId, testId) + "/" + getRenderImg(Constant.BLUE_LIGHT)
//
//            var mtlPath2 = get3DDownloadPath(customId2, testId2) + "/${Constant.SOLEX}.mtl"
//            var imgPath2 =
//                get3DDownloadPath(customId2, testId2) + "/" + getRenderImg(Constant.CROSS_LIGHT)
//
//            modelSurfaceView!!.compareRender.setChangeTexture(mtlPath, imgPath, mtlPath2, imgPath2)
//            modelSurfaceView!!.compareRender.setReset()
//        }
    }


    private var isload3D = false
    fun set3DActivity(activity: Activity) {
        sceneLoader = CompareSceneLoader(activity, null, 0)
        sceneLoader?.setLoadListener(this)
        try {
            touchController = TouchController(activity)
            touchController!!.addListener(this)
        } catch (e: Exception) {
            Log.e("CHEN", "TouchController", e);
        }

        try {
            cameraController = CameraController(sceneLoader!!.camera)
            touchController!!.addListener(cameraController)
        } catch (e: Exception) {
            Log.e("CHEN", "CameraController", e);
        }
    }

    fun initData(
        activity: Activity,
        objPath: String, isLightType: Int,
        objPath1: String, isLightType1: Int

    ) {
        if (frameLayout.childCount > 0) {
            Log.e("CHEN", "当前已经存在 SurfaceView");
            return
        }

        sceneLoader?.startLoad(objPath, isLightType, false)
        sceneLoader?.startLoad(objPath1, isLightType1, false)

        isload3D = true

        try {
            modelSurfaceView = CompareModelSurfaceView(activity, backgroundColor, sceneLoader, this)
            modelSurfaceView!!.addListener(this)
            frameLayout.addView(modelSurfaceView)
            modelSurfaceView!!.compareRender.addListener(cameraController)
        } catch (e: Exception) {
            Log.e("CHEN", "ModelSurfaceView", e);
        }
    }


    /**
     * 更换纹理
     */
    fun changeTexture(mtl: String, img: String, secondMtl: String, secondImg: String) {
        if (!isload3D) {
            Log.e("CHEN", "当前还未展示3D 无法切换纹理");
            return
        }
        modelSurfaceView!!.compareRender.setChangeTexture(mtl, img, secondMtl, secondImg)
        modelSurfaceView!!.compareRender.setReset()
    }


    override fun onEvent(event: EventObject?): Boolean {
        if (event is ModelRenderer.ViewEvent) {
            val viewEvent = event
            if (viewEvent.code == ModelRenderer.ViewEvent.Code.SURFACE_CHANGED) {
                touchController!!.setSize(viewEvent.width, viewEvent.height)
                modelSurfaceView!!.setTouchController(touchController)
            }
        }
        return true
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
        // TODO: 2022/6/24 开始调用 生成其他图片
    }

    private var screenWidth = 0
    private fun changeViewWidth(progress: Int) {
//        Log.e(CHEN, "当前进度为$progress")
        if (screenWidth == 0) return
//        val width = (screenWidth / (100 * progress).toFloat()).toInt()
        val width = screenWidth / 100 * progress
        modelSurfaceView?.compareRender?.setScissor(width)
    }

    override fun dividerChange(width: Int) {
//        var params: LayoutParams = divider?.layoutParams as LayoutParams
//
//        divider?.post {
//            params.let {
//                it.leftMargin = width
//                divider?.layoutParams = params
//                divider?.invalidate()
//            }
//        }
    }

    override fun feedBackWidth(surfaceWidth: Int) {
        screenWidth = surfaceWidth

//        var params: LayoutParams = divider?.layoutParams as LayoutParams
//
//        divider?.post {
//            params.let {
//                it.leftMargin = screenWidth
//                divider?.layoutParams = params
//                divider?.invalidate()
//            }
//        }

    }

    override fun seekChange(leftDistance: Float) {
        modelSurfaceView?.compareRender?.setScissor(leftDistance.toInt())
    }

}