package com.pix.dayline.data.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pix.dayline.model.DaylineSpace
import com.pix.dayline.model.ItemColor

@Entity(
    tableName = "dayline_spaces",
    indices = [Index(value = ["calendarId"])]
)
@TypeConverters(DaylineRoomConverters::class)
data class DaylineSpaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: ItemColor,
    val calendarId: Long?
) {
    fun toModel(): DaylineSpace = DaylineSpace(
        id = id,
        name = name,
        color = color,
        calendarId = calendarId
    )

    companion object {
        fun fromModel(space: DaylineSpace): DaylineSpaceEntity = DaylineSpaceEntity(
            id = space.id,
            name = space.name,
            color = space.color,
            calendarId = space.calendarId
        )
    }
}
