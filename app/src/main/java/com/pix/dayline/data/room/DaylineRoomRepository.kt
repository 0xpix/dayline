package com.pix.dayline.data.room

import android.content.Context
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.DaylineSpace
import java.util.concurrent.Executors

/**
 * Process-wide compatibility bridge between DaylineStore and Room.
 *
 * Ordinary item/Space mutations update the in-memory snapshot immediately and
 * queue disk persistence on one dedicated executor, preserving caller order
 * without blocking the UI path. Restore/rollback uses explicit blocking methods
 * so transactional recovery semantics remain deterministic.
 */
class DaylineRoomRepository private constructor(context: Context) {
    private val database = DaylineDatabase.get(context)
    private val itemDao = database.itemDao()
    private val spaceDao = database.spaceDao()
    private val metadata = context.applicationContext.getSharedPreferences(
        METADATA_PREFERENCES,
        Context.MODE_PRIVATE
    )
    private val io = Executors.newSingleThreadExecutor { task ->
        Thread(task, "dayline-room").apply { isDaemon = true }
    }

    private val initializeLock = Any()

    @Volatile
    private var initialized = false

    @Volatile
    private var itemSnapshot: List<DaylineItem> = emptyList()

    @Volatile
    private var spaceSnapshot: List<DaylineSpace> = emptyList()

    fun initialize(
        legacyItems: List<DaylineItem>,
        legacySpaces: List<DaylineSpace>,
        legacyItemsValid: Boolean = true,
        legacySpacesValid: Boolean = true
    ): Boolean {
        if (initialized) return metadata.getBoolean(KEY_LEGACY_IMPORTED, false)

        synchronized(initializeLock) {
            if (initialized) return metadata.getBoolean(KEY_LEGACY_IMPORTED, false)

            val result = io.submit<Triple<List<DaylineItem>, List<DaylineSpace>, Boolean>> {
                val alreadyImported = metadata.getBoolean(KEY_LEGACY_IMPORTED, false)
                val plan = planLegacyRoomMigration(
                    alreadyImported = alreadyImported,
                    roomItemCount = itemDao.count(),
                    roomSpaceCount = spaceDao.count(),
                    legacyItemCount = legacyItems.size,
                    legacySpaceCount = legacySpaces.size,
                    legacyItemsValid = legacyItemsValid,
                    legacySpacesValid = legacySpacesValid
                )

                if (plan.importItems) {
                    itemDao.replaceAll(
                        legacyItems.mapIndexed { index, item ->
                            DaylineItemEntity.fromModel(item, position = index)
                        }
                    )
                }
                if (plan.importSpaces) {
                    spaceDao.replaceAll(
                        legacySpaces.mapIndexed { index, space ->
                            DaylineSpaceEntity.fromModel(space, position = index)
                        }
                    )
                }

                var migrationComplete = alreadyImported
                if (plan.markComplete) {
                    check(
                        metadata.edit()
                            .putBoolean(KEY_LEGACY_IMPORTED, true)
                            .commit()
                    ) {
                        "Could not mark Dayline Room migration complete"
                    }
                    migrationComplete = true
                }

                Triple(
                    itemDao.loadAll().map(DaylineItemEntity::toModel),
                    spaceDao.loadAll().map(DaylineSpaceEntity::toModel),
                    migrationComplete
                )
            }.get()

            itemSnapshot = result.first
            spaceSnapshot = result.second
            initialized = true
            return result.third
        }
    }

    fun loadItems(): List<DaylineItem> {
        check(initialized) { "Dayline Room repository has not been initialized" }
        return itemSnapshot
    }

    fun loadSpaces(): List<DaylineSpace> {
        check(initialized) { "Dayline Room repository has not been initialized" }
        return spaceSnapshot
    }

    fun replaceItems(items: List<DaylineItem>) {
        check(initialized) { "Dayline Room repository has not been initialized" }
        val snapshot = items.toList()
        itemSnapshot = snapshot
        io.execute { persistItems(snapshot) }
    }

    fun replaceItemsBlocking(items: List<DaylineItem>) {
        check(initialized) { "Dayline Room repository has not been initialized" }
        val snapshot = items.toList()
        io.submit { persistItems(snapshot) }.get()
        itemSnapshot = snapshot
    }

    fun replaceSpaces(spaces: List<DaylineSpace>) {
        check(initialized) { "Dayline Room repository has not been initialized" }
        val snapshot = spaces.toList()
        spaceSnapshot = snapshot
        io.execute { persistSpaces(snapshot) }
    }

    fun replaceSpacesBlocking(spaces: List<DaylineSpace>) {
        check(initialized) { "Dayline Room repository has not been initialized" }
        val snapshot = spaces.toList()
        io.submit { persistSpaces(snapshot) }.get()
        spaceSnapshot = snapshot
    }

    private fun persistItems(items: List<DaylineItem>) {
        itemDao.replaceAll(
            items.mapIndexed { index, item ->
                DaylineItemEntity.fromModel(item, position = index)
            }
        )
    }

    private fun persistSpaces(spaces: List<DaylineSpace>) {
        spaceDao.replaceAll(
            spaces.mapIndexed { index, space ->
                DaylineSpaceEntity.fromModel(space, position = index)
            }
        )
    }

    companion object {
        private const val METADATA_PREFERENCES = "dayline_room_meta"
        private const val KEY_LEGACY_IMPORTED = "legacy_items_spaces_imported_v1"

        @Volatile
        private var instance: DaylineRoomRepository? = null

        fun get(context: Context): DaylineRoomRepository =
            instance ?: synchronized(this) {
                instance ?: DaylineRoomRepository(context.applicationContext)
                    .also { instance = it }
            }
    }
}
