package com.pix.dayline.model

data class DaylineSpace(
    val id: String,
    val name: String,
    val color: ItemColor = ItemColor.MONO,
    val calendarId: Long? = null
)
