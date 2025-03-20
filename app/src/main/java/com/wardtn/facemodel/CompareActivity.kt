package com.wardtn.facemodel

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.wardtn.modellibrary.Model3DActivity

class CompareActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compare)
        startActivity(Intent(this, Model3DActivity::class.java))
    }
}