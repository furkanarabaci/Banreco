package com.illegaldisease.banreco.activities

import android.Manifest
import android.app.AlertDialog
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.NotificationCompat
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
import java.io.*

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import android.app.NotificationManager
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.os.Build
import android.provider.CalendarContract
import android.provider.CalendarContract.Events.*
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.illegaldisease.banreco.camera.CameraActivity


class ImageActivity : AppCompatActivity(), TimePickerDialog.OnTimeSetListener,DatePickerDialog.OnDateSetListener {
    companion object {
        private const val MY_PERMISSIONS_REQUEST_WRITE_CALENDAR = 8
    }
    private lateinit var imageView : ImageView
    private lateinit var trashButton : FloatingActionButton
    private lateinit var setDateButton : FloatingActionButton
    private lateinit var setTimeButton : FloatingActionButton
    private lateinit var doneButton : FloatingActionButton
    private lateinit var dateTextView : TextView
    private var willShowButtons : Boolean = false

    private val format = SimpleDateFormat("dd MM yyyy HH:mm")

    private var lastEventDate : Calendar = GregorianCalendar.getInstance(TimeZone.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
        imageView = findViewById(R.id.activityImage)

        willShowButtons = intent.getSerializableExtra("willshow") as Boolean

        if(willShowButtons){ //If we reached this by clicking photo button, go here.
            buttonActions()
            imageView.setImageBitmap(EventHandler.lastImageBitmap)
        }
        else{ //If we reached this by clicking photo on logs or events, go here.
            val bitmapDate = intent.getSerializableExtra("Bitmap") as Int
            imageView.setImageBitmap(EventHandler.convertToBitmap(this,bitmapDate).rotate(90F))
        }
    }
    private fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
    override fun onDestroy() {
        super.onDestroy()
        OcrHandler.stringList.clear()
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
                //TODO: Properly check if user set the time. Otherwise current date will be entered.
                saveToFile(EventHandler.lastImageBitmap,lastEventDate.timeInMillis / 1000) //It takes current time if we could not detect the time.
                EventHandler.addEvent(this, EventModel(0,(lastEventDate.timeInMillis / 1000).toInt()))
                addToGoogleCalendar()
                finish()
            }
            catch (e : Exception){
                Snackbar.make(window.decorView,getString(R.string.eventaddingfailed),Snackbar.LENGTH_LONG).show()
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
        dateTextView.text = format.format(lastEventDate.time) //Always update things beforehand.
    }
    override fun onTimeSet(view: TimePickerDialog?, hourOfDay: Int, minute: Int, second: Int) {
        lastEventDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
        lastEventDate.set(Calendar.MINUTE, minute)
        lastEventDate.set(Calendar.SECOND, second)
        dateTextView.text = format.format(lastEventDate.time)
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
        dateTextView.text = getString(R.string.dateparsefailed)
        try{
            Log.d("testmeup",parseToTry)
            val date = format.parse(parseToTry)
            dateTextView.text = format.format(date)
            lastEventDate.time = date //Make it automatic.
        }
        catch (e : ParseException){
            //Means we failed. Don't raise errors, only tell the user that we failed miserably.
            Log.d("Whatever",parseToTry)
            if(willShowButtons) dateTextView.text = getString(R.string.dateparsefailed)
        }
    }
    private fun createNotification(){
        //TODO: Implement this.
        val intent = Intent(this, ImageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "banrecoApp"
            val description = "You have one event"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("banreco", name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
        else{
            val mBuilder = NotificationCompat.Builder(this, "banreco")
                    .setSmallIcon(R.mipmap.logo)
                    .setContentTitle("You have one event")
                    .setContentText("test") //TODO: Change later.
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
        }
    }
    private fun addToGoogleCalendar(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)
                == PackageManager.PERMISSION_GRANTED) {
            val calID = 3
            //TODO: Change reminder settings here. Somehow.
            val cr = contentResolver
            val values = ContentValues()
            values.put(DTSTART, lastEventDate.timeInMillis)
            values.put(DTEND, lastEventDate.timeInMillis + 10000) //TODO: Random end date, change maybe.
            values.put(TITLE, "BanrecoEvent")
            values.put(DESCRIPTION, "Custom event added by banreco")
            values.put(CALENDAR_ID, calID)
            values.put(EVENT_TIMEZONE, "Turkey/Istanbul")
            val uri = cr.insert(CONTENT_URI, values)
            val eventID = uri.lastPathSegment.toLong() //TODO: Save this to database, maybe.
        }
        else{
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_CALENDAR),
                    MY_PERMISSIONS_REQUEST_WRITE_CALENDAR)
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_WRITE_CALENDAR-> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    addToGoogleCalendar()
                } else {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Permission")
                            .setMessage("You did not permit me to insert event to the calendar, so it will not be saved. " +
                                    "But you can still see your events under this application.") //TODO: No hardcoded string, move these to res
                            .setPositiveButton(R.string.ok) { it, _ ->
                                it.dismiss()
                            }
                            .show()
                }
                return
            }

        // Add other 'when' lines to check for other
        // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }
}
