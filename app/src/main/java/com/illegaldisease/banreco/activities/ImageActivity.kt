package com.illegaldisease.banreco.activities

import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.widget.ImageView

import com.illegaldisease.banreco.R
import com.illegaldisease.banreco.databaserelated.EventHandler
import com.illegaldisease.banreco.databaserelated.EventModel

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*

class ImageActivity : AppCompatActivity(), TimePickerDialog.OnTimeSetListener,DatePickerDialog.OnDateSetListener {

    private lateinit var imageView : ImageView
    private lateinit var trashButton : FloatingActionButton
    private lateinit var setDateButton : FloatingActionButton
    private lateinit var setTimeButton : FloatingActionButton
    private lateinit var doneButton : FloatingActionButton
    private var bitmapDate : Int = 0

    private var lastEventDate : Calendar = GregorianCalendar.getInstance(TimeZone.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
        imageView = findViewById(R.id.activityImage)

        bitmapDate = intent.getSerializableExtra("Bitmap") as Int
        val willShowButtons = intent.getSerializableExtra("willshow") as Boolean

        imageView.setImageBitmap(EventHandler.lastImageBitmap)
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

        trashButton.setOnClickListener {
            finish() //It is very similar to pressing back button.
        }
        setDateButton.setOnClickListener { pickDate() }
        setTimeButton.setOnClickListener { pickTime() }
        doneButton.setOnClickListener {
            try{
                saveToFile(EventHandler.lastImageBitmap,bitmapDate.toLong())
                EventHandler.addEvent(this, EventModel(0,bitmapDate))
                Snackbar.make(window.decorView,getString(R.string.evensuccessfullyadded),Snackbar.LENGTH_LONG)
            }
            catch (e : Exception){
                Snackbar.make(window.decorView,getString(R.string.eventaddingfailed),Snackbar.LENGTH_LONG)
            }

        }
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
    }
    override fun onTimeSet(view: TimePickerDialog?, hourOfDay: Int, minute: Int, second: Int) {
        lastEventDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
        lastEventDate.set(Calendar.MINUTE, minute)
        lastEventDate.set(Calendar.SECOND, second)
    }

    private fun saveToFile(bitmap: Bitmap, date : Long){
        val filePath = File(filesDir.toURI())

        val imageFile = File(filePath.absolutePath
                + File.separator
                + date
                + ".jpeg")
        imageFile.createNewFile()
        val ostream = ByteArrayOutputStream()
        // save image into gallery
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream)
        FileOutputStream(imageFile).apply {
            write(ostream.toByteArray())
            close()
        }
    }
}
