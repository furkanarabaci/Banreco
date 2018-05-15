package com.illegaldisease.banreco.databaserelated

import android.graphics.Bitmap

data class EventModel(val id: Int, val date: Int) {
    //This fits exactly with database.
    /**
     * id = typical id you know.
     * date = do not let it confuse you, this is an integer value which holds epoch value of that date.
     */
    companion object {
        const val databaseName = "BanrecoDB"
        const val tableName = "Events"
        const val idColumn = "id"
        const val dateColumn = "date"
    }
}
data class EventsRemastered(val id : Int, val date : Int, val photo : Bitmap) //This is exactly how it will be shown in fragments.
