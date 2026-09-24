package com.pix.dayline.data.room

data class RoomMigrationPlan(
    val importItems: Boolean,
    val importSpaces: Boolean,
    val markComplete: Boolean
)

fun planLegacyRoomMigration(
    alreadyImported: Boolean,
    roomItemCount: Int,
    roomSpaceCount: Int,
    legacyItemCount: Int,
    legacySpaceCount: Int
): RoomMigrationPlan {
    if (alreadyImported) {
        return RoomMigrationPlan(
            importItems = false,
            importSpaces = false,
            markComplete = false
        )
    }

    return RoomMigrationPlan(
        importItems = roomItemCount == 0 && legacyItemCount > 0,
        importSpaces = roomSpaceCount == 0 && legacySpaceCount > 0,
        markComplete = true
    )
}
