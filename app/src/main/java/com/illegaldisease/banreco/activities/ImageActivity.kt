package com.illegaldisease.banreco.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.widget.ImageView
import com.illegaldisease.banreco.R
import com.illegaldisease.banreco.databaserelated.EventHandler
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import java.util.*

class ImageActivity : AppCompatActivity(), TimePickerDialog.OnTimeSetListener,DatePickerDialog.OnDateSetListener {

    private lateinit var imageView : ImageView
    private lateinit var trashButton : FloatingActionButton
    private lateinit var setDateButton : FloatingActionButton
    private lateinit var setTimeButton : FloatingActionButton
    private lateinit var doneButton : FloatingActionButton

    private var lastEventDate : Calendar = GregorianCalendar.getInstance(TimeZone.getDefault()) //Don't forget to re-initialize

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
    private fun pickTime(){
        val tpd = TimePickerDialog.newInstance(
                this@ImageActivity,
                true
        )
        tpd.show(fragmentManager,"TimePicker")
        tpd.version = TimePickerDialog.Version.VERSION_2
    }
    private fun pickDate(){
        val now = Calendar.getInstance()
        val dpd = DatePickerDialog.newInstance(
                this@ImageActivity,
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        )
        dpd.show(fragmentManager, "DatePicker")
        dpd.version = DatePickerDialog.Version.VERSION_2
    }
    override fun onDateSet(view: DatePickerDialog?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        lastEventDate.set(Calendar.MONTH, monthOfYear)
        lastEventDate.set(Calendar.YEAR, year)
        lastEventDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        pickTime()
    }
    override fun onTimeSet(view: TimePickerDialog?, hourOfDay: Int, minute: Int, second: Int) {
        lastEventDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
        lastEventDate.set(Calendar.MINUTE, minute)
        lastEventDate.set(Calendar.SECOND, second)
    }

}
