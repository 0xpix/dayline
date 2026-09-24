package com.pix.dayline.data.room

import android.content.Context
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.DaylineSpace
import java.util.concurrent.Executors

/**
 * Process-wide compatibility bridge between the current synchronous DaylineStore
 * API and Room.
 *
 * The repository performs database work on one dedicated thread. Callers remain
 * synchronous for this first migration step; later UI work can move individual
 * mutations to suspend APIs without changing the database schema.
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
        legacySpaces: List<DaylineSpace>
    ) {
        if (initialized) return

        synchronized(initializeLock) {
            if (initialized) return

            val snapshot = io.submit<Pair<List<DaylineItem>, List<DaylineSpace>>> {
                val plan = planLegacyRoomMigration(
                    alreadyImported = metadata.getBoolean(KEY_LEGACY_IMPORTED, false),
                    roomItemCount = itemDao.count(),
                    roomSpaceCount = spaceDao.count(),
                    legacyItemCount = legacyItems.size,
                    legacySpaceCount = legacySpaces.size
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
                if (plan.markComplete) {
                    check(
                        metadata.edit()
                            .putBoolean(KEY_LEGACY_IMPORTED, true)
                            .commit()
                    ) {
                        "Could not mark Dayline Room migration complete"
                    }
                }

                itemDao.loadAll().map(DaylineItemEntity::toModel) to
                    spaceDao.loadAll().map(DaylineSpaceEntity::toModel)
            }.get()

            itemSnapshot = snapshot.first
            spaceSnapshot = snapshot.second
            initialized = true
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
        io.submit {
            itemDao.replaceAll(
                snapshot.mapIndexed { index, item ->
                    DaylineItemEntity.fromModel(item, position = index)
                }
            )
        }.get()
        itemSnapshot = snapshot
    }

    fun replaceSpaces(spaces: List<DaylineSpace>) {
        check(initialized) { "Dayline Room repository has not been initialized" }
        val snapshot = spaces.toList()
        io.submit {
            spaceDao.replaceAll(
                snapshot.mapIndexed { index, space ->
                    DaylineSpaceEntity.fromModel(space, position = index)
                }
            )
        }.get()
        spaceSnapshot = snapshot
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
