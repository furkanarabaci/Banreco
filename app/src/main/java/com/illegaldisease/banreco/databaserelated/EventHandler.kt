package com.illegaldisease.banreco.databaserelated

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.text.format.DateFormat
import android.widget.Toast
import org.jetbrains.anko.db.parseList
import org.jetbrains.anko.db.rowParser
import org.jetbrains.anko.db.select
import org.jetbrains.anko.doAsyncResult
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import android.provider.MediaStore.Images.Media.getBitmap
import com.illegaldisease.banreco.R
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread


class EventHandler { //Don't construct this object more than once. PLEASE.
    companion object {
        val futureEvents : MutableList<EventsRemastered> = ArrayList()
        val pastEvents : MutableList<EventsRemastered> = ArrayList()
        fun fillOutTables(context : Context){
            //Also inspects them if they belong to past or future. Will consider exact this time as past.
            futureEvents.clear()
            pastEvents.clear() //Just in case, i don't ever want duplicate entries.
            var wholeEvents : MutableList<EventModel> = ArrayList() //Temporary list.
            val dbCall = context.database.use {
                select(EventModel.tableName).exec {

                    val parser = rowParser{ id : Int, date : Int, photoTaken : String ->
                        EventModel(id,date,photoTaken)
                    }
                    wholeEvents = parseList(parser).toMutableList()
                }
            }
            dbCall.doAsyncResult {
                //Take care of NullPointerException.
                wholeEvents.forEach { current ->
                    addEventToLists(context, current)
                }
            }
        }
        fun deleteEvent(context : Context, eventToDelete : EventsRemastered){
            //Deletes from database and relevant list as well. Don't forget to check already nonexistent data.
            if(futureEvents.contains(eventToDelete)){
                if(futureEvents.size == 1){
                    futureEvents.clear() //It seems like kotlin does not clear with remove
                }
                else{
                    futureEvents.remove(eventToDelete)
                }
            }
            else{
                //If it is not on future, it is probably belong to the past.
                if(pastEvents.size == 1){
                    pastEvents.clear()
                }
                else{
                    pastEvents.remove(eventToDelete)
                }

            }
            //Future or past, they belong at the same table.
            context.database.use {
                var rowsDeleted = delete(EventModel.tableName,"${EventModel.idColumn} = ${eventToDelete.id}", arrayOf())
                //Probably faulty syntax for anko. Your app might be broken if they fix that. For a workaround, i provided empty array to satisfy WhereArgs.
            }
        }
        fun addEvent(context: Context){
            //TODO: Currently we don't know how to retrieve image and data from activity itself. Fix this when you implement that.
            var file = context.filesDir
            val image0 = File( file.absolutePath + "/20180418_131822.jpg")
            val image1 = File( file.absolutePath  + "/20180418_131624.jpg")
            if(!image0.exists() || !image1.exists()){
                Toast.makeText(context,context.filesDir.toString(),Toast.LENGTH_LONG).show()
                return //Get out.
            }
            val thisGoodTime = 1524297600
            val thisUglyTime = 1524315600
            val thisContent = ContentValues()
            thisContent.put(EventModel.dateColumn, thisUglyTime)
            thisContent.put(EventModel.imageColumn, image0.absolutePath)
            val thisContent1 = ContentValues()
            thisContent1.put(EventModel.dateColumn, thisGoodTime)
            thisContent1.put(EventModel.imageColumn, image1.absolutePath)
            context.database.use {
                insert(EventModel.tableName,null,thisContent)
                insert(EventModel.tableName,null,thisContent1)
            }
        }
        private fun addEventToLists(context: Context, currentModel : EventModel){
            //This function will separate entries using current date ( past or future ) and place it to Lists.
            var async = doAsync {
                val thisDate = convertTimestampToString(currentModel.date)
                val thisPhoto = convertToBitmap(context, currentModel.photoTaken)
                uiThread {
                    if(System.currentTimeMillis()/1000 >= currentModel.date){
                        //Why would i compare two calendars, when i have timestamps already ???
                        //Here is past events.
                        pastEvents.add(EventsRemastered(currentModel.id, thisDate,thisPhoto))
                    } //TODO: Call this two in async way.
                    else{
                        futureEvents.add(EventsRemastered(currentModel.id, thisDate,thisPhoto))
                    }
                }
            }
        }
        private fun convertToBitmap(context: Context, stringUri : String) : Bitmap {
            return MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.parse("file://$stringUri")) ?:
            BitmapFactory.decodeResource(context.resources, R.drawable.images) //Have a placeholder if url is not found somehow.
            //TODO: Screw this, find a different way to accomplish this.

        }
        private fun convertTimestampToString(timestamp : Int) : String {
            val calendar = Calendar.getInstance(Locale.ENGLISH)
            calendar.timeInMillis = timestamp.toLong() * 1000
            return DateFormat.format("dd-MM-yyyy HH:MM",calendar).toString()
        }

    }
}
