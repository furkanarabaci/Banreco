package com.illegaldisease.banreco.activities

import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import com.illegaldisease.banreco.R
import com.illegaldisease.banreco.databaserelated.EventHandler

class ImageActivity : AppCompatActivity() {

    private var imageView : ImageView ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
        imageView = findViewById(R.id.activityImage)
        val bitmapDate = intent.getSerializableExtra("Bitmap") as Int
        imageView!!.setImageBitmap(EventHandler.convertToBitmap(this,bitmapDate))
    }
}
