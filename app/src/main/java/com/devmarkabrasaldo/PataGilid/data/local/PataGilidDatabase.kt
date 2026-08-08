package com.devmarkabrasaldo.PataGilid.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.devmarkabrasaldo.PataGilid.domain.models.Mountain

@Database(entities = [Mountain::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PataGilidDatabase : RoomDatabase() {

    abstract fun mountainDao(): MountainDao

    companion object {
        @Volatile
        private var INSTANCE: PataGilidDatabase? = null

        fun getDatabase(context: Context): PataGilidDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PataGilidDatabase::class.java,
                    "patagilid_database"
                ).fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
