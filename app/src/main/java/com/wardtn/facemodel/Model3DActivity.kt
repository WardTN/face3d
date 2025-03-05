package com.wardtn.facemodel

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import com.wardtn.facemodel.Constant.PATH_Folder_3D
import com.wardtn.facemodel.Constant.SOLEX
import com.wardtn.facemodel.Native3DLayout

class Model3DActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_model3_dactivity)

        findViewById<Button>(R.id.btn_show_3d).setOnClickListener {
            findViewById<Native3DLayout>(R.id.native3dLayout).set3DActivity(this)
            PATH_Folder_3D =
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath + "/3DMark"
            var obj = "$PATH_Folder_3D/$SOLEX.obj"

            findViewById<Native3DLayout>(R.id.native3dLayout).initData(this, obj)

        }

        findViewById<Button>(R.id.btn_remove_mask).setOnClickListener {
            findViewById<Native3DLayout>(R.id.native3dLayout).getRenderer()
                ?.isRemoveMarkTexture(true)
        }

        findViewById<Button>(R.id.btn_change_texure).setOnClickListener {
            findViewById<Native3DLayout>(R.id.native3dLayout).getRenderer()
                ?.setChangeMarkTexture(true)
        }
        
    }
}