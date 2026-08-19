package com.devmarkabrasaldo.PataGilid.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.devmarkabrasaldo.PataGilid.domain.models.Mountain
import com.devmarkabrasaldo.PataGilid.domain.models.MountainList
import com.devmarkabrasaldo.PataGilid.domain.models.HikeLog
import com.devmarkabrasaldo.PataGilid.domain.models.CoordinateSubmission

@Database(entities = [Mountain::class, MountainList::class, HikeLog::class, CoordinateSubmission::class], version = 6, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PataGilidDatabase : RoomDatabase() {

    abstract fun mountainDao(): MountainDao
    abstract fun mountainListDao(): MountainListDao
    abstract fun hikeLogDao(): HikeLogDao
    abstract fun coordinateSubmissionDao(): CoordinateSubmissionDao

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
