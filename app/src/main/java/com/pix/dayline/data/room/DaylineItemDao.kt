package com.pix.dayline.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface DaylineItemDao {
    @Query("SELECT * FROM dayline_items ORDER BY position ASC")
    fun loadAll(): List<DaylineItemEntity>

    @Query("SELECT COUNT(*) FROM dayline_items")
    fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: List<DaylineItemEntity>)

    @Query("DELETE FROM dayline_items")
    fun deleteAll()

    @Transaction
    fun replaceAll(items: List<DaylineItemEntity>) {
        deleteAll()
        if (items.isNotEmpty()) insertAll(items)
    }
}
