package com.scanapp.scanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(
    entities = [ScanHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(ScanSourceConverter::class)
abstract class ScanHistoryDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile
        private var instance: ScanHistoryDatabase? = null

        fun getInstance(context: Context): ScanHistoryDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScanHistoryDatabase::class.java,
                    "scan-history.db",
                ).build().also { instance = it }
            }
        }
    }
}

class ScanSourceConverter {
    @TypeConverter
    fun fromSource(source: ScanSource): String = source.name

    @TypeConverter
    fun toSource(value: String): ScanSource = ScanSource.valueOf(value)
}
