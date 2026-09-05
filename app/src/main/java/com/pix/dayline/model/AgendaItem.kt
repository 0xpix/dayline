package com.pix.dayline.model

enum class AgendaKind {
    EVENT,
    TASK
}

data class AgendaItem(
    val kind: AgendaKind,
    val timeLabel: String?,
    val title: String
)
