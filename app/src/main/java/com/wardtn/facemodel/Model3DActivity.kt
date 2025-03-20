package com.wardtn.facemodel

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import androidx.core.app.ActivityCompat
import com.wardtn.modellibrary.Constant.PATH_Folder_3D
import com.wardtn.modellibrary.Constant.SOLEX
import com.wardtn.modellibrary.Native3DLayout
import com.wardtn.modellibrary.util.CommonUtils
import com.wardtn.modellibrary.util.android.ContentUtils

class Model3DActivity : AppCompatActivity() {

    private val REQUEST_PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private val PERMISSION_REQUEST_CODE = 1
    private var  isRemove = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_model3_dactivity)

        if (!hasPermissionsGranted(REQUEST_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, REQUEST_PERMISSIONS, PERMISSION_REQUEST_CODE)
        }

        findViewById<Button>(R.id.btn_show_3d).setOnClickListener {
            val fileDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath
            CommonUtils.copyAssetsDirToSDCard(this@Model3DActivity, "3DMark", fileDir)


            findViewById<Native3DLayout>(R.id.native3dLayout).set3DActivity(this)
            PATH_Folder_3D = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath + "/3DMark"
            var obj = "$PATH_Folder_3D/$SOLEX.obj"

            var overLayPath = "$PATH_Folder_3D/output_mask.png"

            findViewById<Native3DLayout>(R.id.native3dLayout).initData(this, obj, overLayPath)
        }

        // 更换上层纹理
        findViewById<Button>(R.id.btn_remove_mask).setOnClickListener {
            findViewById<Native3DLayout>(R.id.native3dLayout).getRenderer()
                ?.isShowMarkTexure()
        }

        // 更换底层纹理
        findViewById<Button>(R.id.btn_change_texure).setOnClickListener {
            findViewById<Native3DLayout>(R.id.native3dLayout).getRenderer()
                ?.setChangeMarkTexture(ContentUtils.getOverlayPath2())
        }
    }

    protected fun hasPermissionsGranted(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(
                    this, permission!!
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

}