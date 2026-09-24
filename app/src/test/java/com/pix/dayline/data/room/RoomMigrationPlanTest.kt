package com.pix.dayline.data.room

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomMigrationPlanTest {
    @Test
    fun firstMigrationImportsOnlyEmptyRoomTables() {
        val plan = planLegacyRoomMigration(
            alreadyImported = false,
            roomItemCount = 0,
            roomSpaceCount = 0,
            legacyItemCount = 4,
            legacySpaceCount = 2
        )

        assertTrue(plan.importItems)
        assertTrue(plan.importSpaces)
        assertTrue(plan.markComplete)
    }

    @Test
    fun existingRoomRowsWinOverStaleLegacyData() {
        val plan = planLegacyRoomMigration(
            alreadyImported = false,
            roomItemCount = 3,
            roomSpaceCount = 1,
            legacyItemCount = 9,
            legacySpaceCount = 5
        )

        assertFalse(plan.importItems)
        assertFalse(plan.importSpaces)
        assertTrue(plan.markComplete)
    }

    @Test
    fun migrationCanFillOnlyTheMissingTable() {
        val plan = planLegacyRoomMigration(
            alreadyImported = false,
            roomItemCount = 5,
            roomSpaceCount = 0,
            legacyItemCount = 7,
            legacySpaceCount = 2
        )

        assertFalse(plan.importItems)
        assertTrue(plan.importSpaces)
        assertTrue(plan.markComplete)
    }

    @Test
    fun emptyLegacyDataStillCompletesMigrationWithoutWritingRows() {
        val plan = planLegacyRoomMigration(
            alreadyImported = false,
            roomItemCount = 0,
            roomSpaceCount = 0,
            legacyItemCount = 0,
            legacySpaceCount = 0
        )

        assertFalse(plan.importItems)
        assertFalse(plan.importSpaces)
        assertTrue(plan.markComplete)
    }

    @Test
    fun completedMigrationNeverReimportsLegacyData() {
        val plan = planLegacyRoomMigration(
            alreadyImported = true,
            roomItemCount = 0,
            roomSpaceCount = 0,
            legacyItemCount = 10,
            legacySpaceCount = 10
        )

        assertFalse(plan.importItems)
        assertFalse(plan.importSpaces)
        assertFalse(plan.markComplete)
    }
}
