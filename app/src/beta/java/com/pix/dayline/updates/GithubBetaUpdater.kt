package com.pix.dayline.updates

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.pix.dayline.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * GitHub beta update channel.
 *
 * This code is only surfaced by the beta flavor. The Play flavor has no INTERNET or
 * REQUEST_INSTALL_PACKAGES permission and therefore remains Play-managed.
 */
object GithubBetaUpdater {
    private const val RELEASES_API = "https://api.github.com/repos/0xpix/dayline/releases?per_page=20"
    private const val USER_AGENT = "Dayline-Beta-Updater/${BuildConfig.VERSION_NAME}"

    data class Release(
        val tag: String,
        val name: String,
        val notes: String,
        val htmlUrl: String,
        val apkName: String,
        val apkUrl: String,
        val checksumUrl: String?
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

    suspend fun check(): CheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val releases = JSONArray(getText(RELEASES_API))
            val current = BuildConfig.VERSION_NAME

            for (i in 0 until releases.length()) {
                val release = releases.getJSONObject(i)
                if (release.optBoolean("draft", false)) continue

                val tag = release.optString("tag_name")
                val isBeta = release.optBoolean("prerelease", false) ||
                    tag.contains("beta", ignoreCase = true)
                if (!isBeta) continue

                val assets = release.optJSONArray("assets") ?: continue
                var apkName: String? = null
                var apkUrl: String? = null
                var checksumUrl: String? = null

                for (j in 0 until assets.length()) {
                    val asset = assets.getJSONObject(j)
                    val name = asset.optString("name")
                    val url = asset.optString("browser_download_url")
                    if (name.endsWith(".apk", ignoreCase = true) &&
                        (name.contains("beta", ignoreCase = true) || apkName == null)
                    ) {
                        apkName = name
                        apkUrl = url
                    }
                }

                if (apkName == null || apkUrl == null) continue

                for (j in 0 until assets.length()) {
                    val asset = assets.getJSONObject(j)
                    val name = asset.optString("name")
                    if (name == "$apkName.sha256") {
                        checksumUrl = asset.optString("browser_download_url")
                        break
                    }
                }

                val parsed = Release(
                    tag = tag,
                    name = release.optString("name").ifBlank { tag },
                    notes = release.optString("body"),
                    htmlUrl = release.optString("html_url"),
                    apkName = apkName,
                    apkUrl = apkUrl,
                    checksumUrl = checksumUrl
                )

                return@runCatching if (isNewer(parsed.displayVersion, current)) {
                    CheckResult.Available(parsed)
                } else {
                    CheckResult.UpToDate
                }
            }
            CheckResult.NoBetaRelease
        }.getOrElse { error ->
            CheckResult.Error(error.message ?: "Could not reach GitHub")
        }
    }

    suspend fun download(context: Context, release: Release): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
            updateDir.listFiles()?.forEach { old -> if (old.isFile) old.delete() }
            val apk = File(updateDir, safeFileName(release.apkName))
            downloadTo(release.apkUrl, apk)

            release.checksumUrl?.let { checksumUrl ->
                val expected = getText(checksumUrl)
                    .trim()
                    .substringBefore(' ')
                    .lowercase()
                val actual = sha256(apk)
                check(expected.length == 64 && expected == actual) {
                    "Downloaded APK checksum did not match the GitHub release"
                }
            }

            verifyApk(context, apk)
            apk
        }
    }

    fun install(context: Context, apk: File): InstallResult {
        if (!apk.isFile) return InstallResult.Error("The downloaded APK is missing")
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(settingsIntent)
                return InstallResult.PermissionRequested
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.updates",
                apk
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            InstallResult.Started
        }.getOrElse { InstallResult.Error(it.message ?: "Could not open the Android installer") }
    }

    fun openRelease(context: Context, release: Release) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun verifyApk(context: Context, apk: File) {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(apk.absolutePath, PackageManager.PackageInfoFlags.of(0))
        } else {
            pm.getPackageArchiveInfo(apk.absolutePath, 0)
        } ?: error("Downloaded file is not a valid Android APK")

        check(info.packageName == context.packageName) {
            "Downloaded APK belongs to ${info.packageName}, not ${context.packageName}"
        }

        val downloadedCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            info.versionCode.toLong()
        }
        check(downloadedCode > BuildConfig.VERSION_CODE.toLong()) {
            "Downloaded APK is not newer than this Dayline beta"
        }
    }

    private fun getText(url: String): String {
        val connection = open(url)
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadTo(url: String, destination: File) {
        val connection = open(url)
        try {
            connection.inputStream.use { input ->
                destination.outputStream().buffered().use { output -> input.copyTo(output) }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        connection.connect()
        check(connection.responseCode in 200..299) {
            "GitHub returned HTTP ${connection.responseCode}"
        }
        return connection
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun safeFileName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "dayline-beta.apk" }

    /** Numeric semantic-ish comparison that also handles tags such as 0.12.2-beta.1. */
    internal fun isNewer(candidate: String, current: String): Boolean {
        val candidateParts = versionParts(candidate)
        val currentParts = versionParts(current)
        val size = maxOf(candidateParts.size, currentParts.size)
        for (i in 0 until size) {
            val left = candidateParts.getOrElse(i) { 0 }
            val right = currentParts.getOrElse(i) { 0 }
            if (left != right) return left > right
        }
        return false
    }

    private fun versionParts(value: String): List<Int> =
        Regex("\\d+").findAll(value).map { it.value.toIntOrNull() ?: 0 }.toList()
}
