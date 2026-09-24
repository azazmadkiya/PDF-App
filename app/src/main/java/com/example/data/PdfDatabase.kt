package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PdfHistoryEntity::class], version = 1, exportSchema = false)
abstract class PdfDatabase : RoomDatabase() {
    abstract fun pdfDao(): PdfDao

    companion object {
        @Volatile
        private var INSTANCE: PdfDatabase? = null

        fun getDatabase(context: Context): PdfDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PdfDatabase::class.java,
                    "pdf_vault_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
