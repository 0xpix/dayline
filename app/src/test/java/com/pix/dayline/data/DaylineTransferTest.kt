package com.pix.dayline.data

import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.Recurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DaylineTransferTest {
    @Test
    fun icsRoundTripPreservesNotes() {
        val item = DaylineItem(
            id = "event-1",
            title = "LUMEN review",
            kind = AgendaKind.EVENT,
            startDate = LocalDate.of(2026, 9, 28),
            startTime = LocalTime.of(11, 0),
            endTime = LocalTime.of(12, 30),
            recurrence = Recurrence.ONCE,
            notes = "Bring model sketch, paper notes, and the validation questions."
        )

        val exported = DaylineTransfer.exportIcs(listOf(item))

        assertTrue(exported.contains("DESCRIPTION:Bring model sketch"))
        val imported = DaylineTransfer.importIcs(exported).single()
        assertEquals(item.title, imported.title)
        assertEquals(item.notes, imported.notes)
        assertEquals(item.startDate, imported.startDate)
        assertEquals(item.startTime, imported.startTime)
        assertEquals(item.endTime, imported.endTime)
    }
}
