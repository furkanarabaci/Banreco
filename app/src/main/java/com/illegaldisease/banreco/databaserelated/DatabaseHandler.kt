package com.illegaldisease.banreco.databaserelated

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.illegaldisease.banreco.databaserelated.DatabaseHandler.Companion.getInstance
import org.jetbrains.anko.db.*

class DatabaseHandler(ctx: Context) : ManagedSQLiteOpenHelper(ctx, EventModel.databaseName, null, 1) {
    companion object {
        private var instance: DatabaseHandler? = null

        @Synchronized
        fun getInstance(ctx: Context): DatabaseHandler {
            if (instance == null) {
                instance = DatabaseHandler(ctx.applicationContext)
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Here you create tables
        db.createTable(EventModel.tableName, true,
                EventModel.idColumn to INTEGER + PRIMARY_KEY + UNIQUE,
                EventModel.dateColumn to INTEGER,
                EventModel.imageColumn to BLOB)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }
}
// Access property for Context
val Context.database: DatabaseHandler
    get() = getInstance(applicationContext)