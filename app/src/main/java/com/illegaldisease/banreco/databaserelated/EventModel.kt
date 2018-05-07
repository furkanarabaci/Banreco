package com.illegaldisease.banreco.databaserelated

import android.graphics.Bitmap

data class EventModel(val id: Int, val date: Int, val photoTaken: String) {
    //This fits exactly with database.
    companion object {
        const val databaseName = "BanrecoDB"
        const val tableName = "Events"
        const val idColumn = "id"
        const val dateColumn = "date"
        const val imageColumn = "photoUri"
    }
}
data class EventsRemastered(val id : Int, val date : String, val photo : Bitmap) //This is exactly how it will be shown in fragments.
