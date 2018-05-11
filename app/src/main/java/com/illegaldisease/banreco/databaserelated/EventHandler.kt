package com.illegaldisease.banreco.databaserelated

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import org.jetbrains.anko.db.parseList
import org.jetbrains.anko.db.rowParser
import org.jetbrains.anko.db.select
import org.jetbrains.anko.doAsyncResult
import java.io.File
import kotlin.collections.ArrayList
import com.illegaldisease.banreco.R
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.text.SimpleDateFormat


class EventHandler { //Don't construct this object more than once. PLEASE.
    companion object {
        val futureEvents : MutableList<EventsRemastered> = ArrayList()
        val pastEvents : MutableList<EventsRemastered> = ArrayList()
        lateinit var lastImageBitmap : Bitmap

        private fun addEventToLists(context: Context, currentModel : EventModel){
            //This function will separate entries using current date ( past or future ) and place it to Lists.
            doAsync {
                val thisPhoto = convertToBitmap(context, currentModel.date)
                uiThread {
                    if(System.currentTimeMillis()/1000 >= currentModel.date){
                        //Why would i compare two calendars, when i have timestamps already ???
                        //Here is past events.
                        pastEvents.add(EventsRemastered(currentModel.id, currentModel.date,thisPhoto))
                    }
                    else{
                        futureEvents.add(EventsRemastered(currentModel.id, currentModel.date,thisPhoto))
                    }
                }
            }
        }

        @Throws(SQLiteException::class)
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
        @Throws(Exception::class)
        fun deleteEvent(context : Context,  eventToDelete : EventsRemastered){
            //Deletes from database and relevant list as well. Don't forget to check already nonexistent data.
            //Future or past, they belong at the same table.
            var rowsDeleted = 0
            val deleteTask = context.database.use {
                rowsDeleted += delete(EventModel.tableName,"${EventModel.idColumn} = ${eventToDelete.id}", arrayOf())
                //Probably faulty syntax for anko. Your app might be broken if they fix that. For a workaround, i provided empty array to satisfy WhereArgs.
            }
            deleteTask.doAsyncResult {
                if(rowsDeleted == 0){
                    throw SQLException("Could not delete from database.")
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
                    else {
                        //If it is not on future, it is probably belong to the past.
                        if(pastEvents.size == 1){
                            pastEvents.clear()
                        }
                        else{
                            pastEvents.remove(eventToDelete)
                        }
                    }
                    //Probably not a good place, but i will refactor later if i get an error because of this.
                    val fileToDelete = File(parsePhotoUri(eventToDelete.date,context.filesDir))
                    //I think this is too complex, but it just tries to find a file.
                    if(fileToDelete.exists()){
                        fileToDelete.delete()
                    }
                    else{
                        throw FileNotFoundException("File not found : ${fileToDelete.absolutePath}")
                    }
                }
            }
        }
        @Throws(Exception::class) //May throw multiple exceptions.
        fun addEvent(context: Context, eventModel: EventModel){
            //This function will only be called when taking photo. You can assign id whatever you want, it will not be used.
            context.database.use {
                val photoUri = parsePhotoUri(eventModel.date, context.filesDir)
                if(File(photoUri).exists()){ //Check if file is created properly before we add it to database.
                    val thisContent = ContentValues()
                    thisContent.put(EventModel.dateColumn, eventModel.date)
                    insert(EventModel.tableName,null,thisContent)
                }
                else{
                    throw IOException("File does not exist. Path : $photoUri")
                }
            }
        }
        @Throws(FileNotFoundException::class)
        fun parsePhotoUri(date : Int, filesDir : File) : URI{
            //It does not check if it exists or not.
            return URI("file://${filesDir.absolutePath}/$date.jpeg")
        }
        fun convertToBitmap(context: Context, date : Int) : Bitmap {
            val photoUri = parsePhotoUri(date,context.filesDir)
            return MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.parse(photoUri.toString())) ?:
            BitmapFactory.decodeResource(context.resources, R.drawable.images) //Have a placeholder if url is not found somehow.
        }
        fun getContentUriOfFile(context: Context,fileURI: URI): Uri{
            return FileProvider.getUriForFile(context,context.packageName + ".provider",File(fileURI))
        }

        @SuppressLint("SimpleDateFormat")
        fun convertTimestampToString(timestamp : Int) : String {
            return SimpleDateFormat("dd-MM-yyyy HH:mm").format(timestamp.toLong() * 1000).toString()
        }
    }
}
