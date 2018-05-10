package com.illegaldisease.banreco.activities

import android.graphics.Bitmap
import android.opengl.Visibility
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.widget.ImageView
import com.illegaldisease.banreco.R
import com.illegaldisease.banreco.databaserelated.EventHandler

class ImageActivity : AppCompatActivity() {

    private val imageView : ImageView = findViewById(R.id.activityImage)
    private val trashButton : FloatingActionButton = findViewById(R.id.imageTrash)
    private val setDateButton : FloatingActionButton = findViewById(R.id.imageSetDate)
    private val setTimeButton : FloatingActionButton = findViewById(R.id.imageSetTime)
    private val doneButton : FloatingActionButton = findViewById(R.id.imageDone)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
        val bitmapDate = intent.getSerializableExtra("Bitmap") as Int
        val willShowButtons = intent.getSerializableExtra("willshow") as Boolean
        imageView.setImageBitmap(EventHandler.convertToBitmap(this,bitmapDate))
        if(willShowButtons) buttonActions()
    }
    private fun buttonActions(){
        //Just for better usage, i put them here.
        trashButton.show()
        setDateButton.show()
        setTimeButton.show()
        doneButton.show()
        trashButton.setOnClickListener { /*TODO: Get rid of image here */ }
        setDateButton.setOnClickListener { /*TODO: Open DateDialogPicker here */}
        setTimeButton.setOnClickListener { /*TODO: Open TimeDialogPicker here */}
        doneButton.setOnClickListener { /*TODO: Add to database here */ }
    }
}
