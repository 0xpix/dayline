package com.pix.dayline.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DaylineItemEntity::class,
        DaylineSpaceEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(DaylineRoomConverters::class)
abstract class DaylineDatabase : RoomDatabase() {
    abstract fun itemDao(): DaylineItemDao
    abstract fun spaceDao(): DaylineSpaceDao

    companion object {
        private const val DATABASE_NAME = "dayline.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE dayline_items ADD COLUMN notes TEXT"
                )
            }
        }

        @Volatile
        private var instance: DaylineDatabase? = null

        fun get(context: Context): DaylineDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DaylineDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
