package com.pix.dayline.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface DaylineSpaceDao {
    @Query("SELECT * FROM dayline_spaces ORDER BY position ASC")
    fun loadAll(): List<DaylineSpaceEntity>

    @Query("SELECT COUNT(*) FROM dayline_spaces")
    fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(spaces: List<DaylineSpaceEntity>)

    @Query("DELETE FROM dayline_spaces")
    fun deleteAll()

    @Transaction
    fun replaceAll(spaces: List<DaylineSpaceEntity>) {
        deleteAll()
        if (spaces.isNotEmpty()) insertAll(spaces)
    }
}
