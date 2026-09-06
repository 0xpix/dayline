package com.pix.dayline.data

import com.pix.dayline.updates.GithubBetaUpdater
import java.time.Instant

enum class UpdateStatus {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    AVAILABLE,
    ERROR
}

data class BetaRelease(
    val tagName: String,
    val versionName: String,
    val title: String,
    val notes: String,
    val publishedAt: Instant?,
    val htmlUrl: String,
    val apkName: String? = null,
    val apkUrl: String? = null,
    val checksumUrl: String? = null
)

data class UpdateUiState(
    val status: UpdateStatus = UpdateStatus.IDLE,
    val release: BetaRelease? = null,
    val error: String? = null,
    val checkedAtMillis: Long? = null
)

/**
 * Common update-check entry point used by Compose and the once-daily scheduler.
 *
 * The actual implementation is supplied by the distribution source set:
 * beta -> GitHub Releases network client, play -> offline no-op stub.
 */
object BetaUpdateChecker {
    suspend fun check(currentVersion: String): UpdateUiState =
        GithubBetaUpdater.check(currentVersion)
}
