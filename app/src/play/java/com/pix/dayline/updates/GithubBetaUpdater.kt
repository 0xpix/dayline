package com.pix.dayline.updates

import android.content.Context
import java.io.File

/** Play builds are updated exclusively by Google Play. */
object GithubBetaUpdater {
    data class Release(
        val tag: String = "",
        val name: String = "",
        val notes: String = "",
        val htmlUrl: String = "",
        val apkName: String = "",
        val apkUrl: String = "",
        val checksumUrl: String? = null
    ) {
        val displayVersion: String = tag.removePrefix("v")
    }

    sealed interface CheckResult {
        data class Available(val release: Release) : CheckResult
        data object UpToDate : CheckResult
        data object NoBetaRelease : CheckResult
        data class Error(val message: String) : CheckResult
    }

    sealed interface InstallResult {
        data object Started : InstallResult
        data object PermissionRequested : InstallResult
        data class Error(val message: String) : InstallResult
    }

    suspend fun check(): CheckResult = CheckResult.NoBetaRelease
    suspend fun download(context: Context, release: Release): Result<File> =
        Result.failure(UnsupportedOperationException("Google Play manages updates for this build"))

    fun install(context: Context, apk: File): InstallResult =
        InstallResult.Error("Google Play manages updates for this build")

    fun openRelease(context: Context, release: Release) = Unit
}
