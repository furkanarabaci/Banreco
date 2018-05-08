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

                    val parser = rowParser{ id : Int, date : Int ->
                        EventModel(id,date)
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
            //Future or past, they belong at the same table.
            val rowsDeleted = 0
            val deleteTask = context.database.use {
                rowsDeleted.plus(delete(EventModel.tableName,"${EventModel.idColumn} = ${eventToDelete.id}", arrayOf())) //Sorry for complexity,
                //I just added the rowsDeleted val ( similar to final in java. )
                //Probably faulty syntax for anko. Your app might be broken if they fix that. For a workaround, i provided empty array to satisfy WhereArgs.
            }
            deleteTask.doAsyncResult {
                if(rowsDeleted == 0){
                    //Means there is some sql error and could not remove from db. Do not warn user for now.
                }
                else{
                    if(futureEvents.contains(eventToDelete)){
                        if(futureEvents.size == 1){
                            futureEvents.clear() //It seems like kotlin does not clear when there is a single element left.
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
                }
            }
        }
        fun addEvent(context: Context, eventModel: EventModel){
            //This function will never be called from inside LogsActivity or EventsActivity. Only when taking photo, it will be called.
            //TODO: Currently we don't know how to retrieve image and data from activity itself. Fix this when you implement that.
            context.database.use {
                val photoUri = parsePhotoUri(eventModel.date.toString(), context.filesDir)
                if(File(photoUri.toString()).exists()){ //Check if file is created properly before we add it to database.
                    val thisContent = ContentValues()
                    thisContent.put(EventModel.dateColumn, eventModel.date)
                    insert(EventModel.tableName,null,thisContent)
                }
            }
        }
        private fun parsePhotoUri(date : String, filesDir : File) : Uri{
            return Uri.parse("file://${filesDir.absolutePath}/$date.jpeg")
        }
        private fun addEventToLists(context: Context, currentModel : EventModel){
            //This function will separate entries using current date ( past or future ) and place it to Lists.
            doAsync {
                val thisDate = convertTimestampToString(currentModel.date)
                val thisPhoto = convertToBitmap(context, thisDate)
                uiThread {
                    if(System.currentTimeMillis()/1000 >= currentModel.date){
                        //Why would i compare two calendars, when i have timestamps already ???
                        //Here is past events.
                        pastEvents.add(EventsRemastered(currentModel.id, thisDate,thisPhoto))
                    }
                    else{
                        futureEvents.add(EventsRemastered(currentModel.id, thisDate,thisPhoto))
                    }
                }
            }
        }
        private fun convertToBitmap(context: Context, date : String) : Bitmap {
            val photoUri = parsePhotoUri(date,context.filesDir)
            return MediaStore.Images.Media.getBitmap(context.contentResolver, photoUri) ?:
            BitmapFactory.decodeResource(context.resources, R.drawable.images) //Have a placeholder if url is not found somehow.
        }
        private fun convertTimestampToString(timestamp : Int) : String {
            val calendar = Calendar.getInstance(Locale.ENGLISH)
            calendar.timeInMillis = timestamp.toLong() * 1000
            return DateFormat.format("dd-MM-yyyy HH:MM",calendar).toString()
        }

    }
}
