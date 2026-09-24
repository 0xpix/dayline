package com.pix.dayline.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        DaylineItemEntity::class,
        DaylineSpaceEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DaylineRoomConverters::class)
abstract class DaylineDatabase : RoomDatabase() {
    abstract fun itemDao(): DaylineItemDao
    abstract fun spaceDao(): DaylineSpaceDao

    companion object {
        private const val DATABASE_NAME = "dayline.db"

        @Volatile
        private var instance: DaylineDatabase? = null

        fun get(context: Context): DaylineDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DaylineDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
    }
}
