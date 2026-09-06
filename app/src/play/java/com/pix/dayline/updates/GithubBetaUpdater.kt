package com.pix.dayline.updates

import android.content.Context
import com.pix.dayline.data.UpdateStatus
import com.pix.dayline.data.UpdateUiState
import com.pix.dayline.data.BetaRelease
import java.io.File

/** Play builds are updated exclusively by Google Play. */
object GithubBetaUpdater {
    sealed interface InstallResult {
        data object Started : InstallResult
        data object PermissionRequested : InstallResult
        data class Error(val message: String) : InstallResult
    }

    suspend fun check(currentVersion: String): UpdateUiState = UpdateUiState(
        status = UpdateStatus.UP_TO_DATE,
        checkedAtMillis = System.currentTimeMillis()
    )

    suspend fun download(context: Context, release: BetaRelease): Result<File> =
        Result.failure(UnsupportedOperationException("Google Play manages updates for this build"))

    fun install(context: Context, apk: File): InstallResult =
        InstallResult.Error("Google Play manages updates for this build")

    fun openRelease(context: Context, release: BetaRelease) = Unit
}
