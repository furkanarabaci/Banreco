package com.illegaldisease.banreco

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.illegaldisease.banreco.EventHandler.Companion.getInstance
import org.jetbrains.anko.db.*

class EventHandler(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "BanrecoDB", null, 1) {
    companion object {
        private var instance: EventHandler? = null

        @Synchronized
        fun getInstance(ctx: Context): EventHandler {
            if (instance == null) {
                instance = EventHandler(ctx.applicationContext)
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Here you create tables
        db.createTable("Events", true,
                "id" to INTEGER + PRIMARY_KEY + UNIQUE,
                "date" to INTEGER,
                "photoTaken" to BLOB)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }
}

// Access property for Context
val Context.database: EventHandler
    get() = getInstance(applicationContext)