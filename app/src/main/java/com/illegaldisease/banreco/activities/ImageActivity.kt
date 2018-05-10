package com.illegaldisease.banreco.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.widget.ImageView
import com.illegaldisease.banreco.R
import com.illegaldisease.banreco.databaserelated.EventHandler

class ImageActivity : AppCompatActivity() {

    private lateinit var imageView : ImageView
    private lateinit var trashButton : FloatingActionButton
    private lateinit var setDateButton : FloatingActionButton
    private lateinit var setTimeButton : FloatingActionButton
    private lateinit var doneButton : FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
        imageView = findViewById(R.id.activityImage)

        val bitmapDate = intent.getSerializableExtra("Bitmap") as Int
        val willShowButtons = intent.getSerializableExtra("willshow") as Boolean

        imageView.setImageBitmap(EventHandler.convertToBitmap(this,bitmapDate))
        if(willShowButtons) buttonActions()
    }
    private fun buttonActions(){
        //Just for better usage, i put them here.
        trashButton = findViewById(R.id.imageTrash)
        setDateButton = findViewById(R.id.imageSetDate)
        setTimeButton = findViewById(R.id.imageSetTime)
        doneButton = findViewById(R.id.imageDone)

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
