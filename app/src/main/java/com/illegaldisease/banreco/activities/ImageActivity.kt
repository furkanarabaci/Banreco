package com.illegaldisease.banreco.activities

import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView

import com.illegaldisease.banreco.R
import com.illegaldisease.banreco.databaserelated.EventHandler
import com.illegaldisease.banreco.databaserelated.EventModel
import com.illegaldisease.banreco.ocrstuff.OcrHandler

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class ImageActivity : AppCompatActivity(), TimePickerDialog.OnTimeSetListener,DatePickerDialog.OnDateSetListener {

    private lateinit var imageView : ImageView
    private lateinit var trashButton : FloatingActionButton
    private lateinit var setDateButton : FloatingActionButton
    private lateinit var setTimeButton : FloatingActionButton
    private lateinit var doneButton : FloatingActionButton
    private lateinit var dateTextView : TextView

    private var lastEventDate : Calendar = GregorianCalendar.getInstance(TimeZone.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
        imageView = findViewById(R.id.activityImage)

        val willShowButtons = intent.getSerializableExtra("willshow") as Boolean

        if(willShowButtons){ //If we reached this by clicking photo button, go here.
            buttonActions()
            imageView.setImageBitmap(EventHandler.lastImageBitmap)
        }
        else{ //If we reached this by clicking photo on logs or events, go here.
            val bitmapDate = intent.getSerializableExtra("Bitmap") as Int
            imageView.setImageBitmap(EventHandler.convertToBitmap(this,bitmapDate))
        }
    }

    override fun onStart() {
        super.onStart()
        tryAutomaticMethod()
    }
    private fun buttonActions(){
        //Just for better usage, i put them here.
        trashButton = findViewById(R.id.imageTrash)
        setDateButton = findViewById(R.id.imageSetDate)
        setTimeButton = findViewById(R.id.imageSetTime)
        doneButton = findViewById(R.id.imageDone)
        dateTextView = findViewById(R.id.imageTextDate) //Shhhh. You are not a button, but this is a perfect place for you.

        trashButton.show()
        setDateButton.show()
        setTimeButton.show()
        doneButton.show()
        dateTextView.visibility = View.VISIBLE //Show yourself, but still be hidden.....

        trashButton.setOnClickListener {
            finish() //It is very similar to pressing back button.
        }
        setDateButton.setOnClickListener { pickDate() }
        setTimeButton.setOnClickListener { pickTime() }
        doneButton.setOnClickListener {
            try{
                saveToFile(EventHandler.lastImageBitmap,lastEventDate.timeInMillis / 1000)
                EventHandler.addEvent(this, EventModel(0,(lastEventDate.timeInMillis / 1000).toInt()))
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

    @Throws(IOException::class)
    private fun saveToFile(bitmap: Bitmap, date : Long) {
        if (checkIfFileExists(date.toInt())) {
            //There is already an event that exact time.
            //TODO: Warn user about this.
        }
        val filePath = File(filesDir.toURI())
        val imageFile = File(filePath.absolutePath
                + File.separator
                + date
                + ".jpeg")
        imageFile.createNewFile()
        val outStream = ByteArrayOutputStream()
        // save image into gallery
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
        FileOutputStream(imageFile).apply {
            write(outStream.toByteArray())
            close()
        }
    }
    private fun checkIfFileExists(date : Int) : Boolean{
        //Call this BEFORE saving to file.
        val currentURI = EventHandler.parsePhotoUri(date,this.filesDir)
        return File(currentURI.toString()).exists()
    }
    private fun tryAutomaticMethod(){
        //We will use the data we already created in CameraFragment.
        val parseToTry = OcrHandler.getRenderedDate() //day,month(0-11),year,hour,minute
        val format = SimpleDateFormat("dd MM yyyy HH:mm")
        try{
            val date = format.parse(parseToTry)
            dateTextView.text = format.format(date)
        }
        catch (e : ParseException){
            //Means we failed. Don't raise errors, only tell the user that we failed miserably.
            Log.d("Whatever",parseToTry)
            dateTextView.text = getString(R.string.dateparsefailed)
        }
    }
}
