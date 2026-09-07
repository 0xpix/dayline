package com.pix.dayline.updates

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.pix.dayline.BuildConfig
import com.pix.dayline.data.BetaRelease
import com.pix.dayline.data.DaylineVersion
import com.pix.dayline.data.UpdateStatus
import com.pix.dayline.data.UpdateUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.security.MessageDigest
import java.time.Instant

/**
 * GitHub beta update channel.
 *
 * Only the beta flavor gets this network/download implementation. The Play flavor
 * supplies an offline stub and does not request INTERNET or install-package access.
 */
object GithubBetaUpdater {
    private const val RELEASES_API = "https://api.github.com/repos/0xpix/dayline/releases?per_page=30"
    private const val USER_AGENT_PREFIX = "Dayline-Beta-Updater/"

    sealed interface InstallResult {
        data object Started : InstallResult
        data object PermissionRequested : InstallResult
        data class Error(val message: String) : InstallResult
    }

    suspend fun check(currentVersion: String = BuildConfig.VERSION_NAME): UpdateUiState =
        withContext(Dispatchers.IO) {
            val checkedAt = System.currentTimeMillis()
            runCatching {
                val releases = JSONArray(getText(RELEASES_API, currentVersion))
                val parsed = buildList {
                    for (index in 0 until releases.length()) {
                        val json = releases.optJSONObject(index) ?: continue
                        if (json.optBoolean("draft", false)) continue

                        val tag = json.optString("tag_name").trim()
                        val version = tag.removePrefix("v").removePrefix("V")
                        if (version.isBlank() || !DaylineVersion.hasNumericVersion(version)) continue

                        val assets = json.optJSONArray("assets") ?: JSONArray()
                        var apkName: String? = null
                        var apkUrl: String? = null
                        var checksumUrl: String? = null

                        val apkCandidates = buildList {
                            for (assetIndex in 0 until assets.length()) {
                                val asset = assets.optJSONObject(assetIndex) ?: continue
                                val name = asset.optString("name")
                                val url = asset.optString("browser_download_url")
                                if (name.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) {
                                    add(name to url)
                                }
                            }
                        }
                        val selected = apkCandidates.firstOrNull {
                            it.first.contains(tag, ignoreCase = true)
                        } ?: apkCandidates.firstOrNull {
                            it.first.contains("beta", ignoreCase = true)
                        } ?: apkCandidates.firstOrNull()

                        if (selected != null) {
                            apkName = selected.first
                            apkUrl = selected.second
                            for (assetIndex in 0 until assets.length()) {
                                val asset = assets.optJSONObject(assetIndex) ?: continue
                                if (asset.optString("name") == "$apkName.sha256") {
                                    checksumUrl = asset.optString("browser_download_url")
                                        .takeIf { it.isNotBlank() }
                                    break
                                }
                            }
                        }

                        add(
                            BetaRelease(
                                tagName = tag,
                                versionName = version,
                                title = json.optString("name").ifBlank { "Dayline $tag" },
                                notes = cleanNotes(json.optString("body")),
                                publishedAt = json.optString("published_at")
                                    .takeIf { it.isNotBlank() }
                                    ?.let { runCatching { Instant.parse(it) }.getOrNull() },
                                htmlUrl = json.optString("html_url"),
                                apkName = apkName,
                                apkUrl = apkUrl,
                                checksumUrl = checksumUrl
                            )
                        )
                    }
                }

                // A release must be newer in BOTH human version and Android
                // versionCode terms. This prevents the UI from offering a tag whose
                // APK Android would reject as the same/older installed build.
                val newest = parsed
                    .filter {
                        DaylineVersion.isInstallableUpdate(
                            candidateVersion = it.versionName,
                            currentVersion = currentVersion,
                            currentVersionCode = BuildConfig.VERSION_CODE.toLong()
                        )
                    }
                    .maxWithOrNull(
                        Comparator { left, right ->
                            DaylineVersion.compare(left.versionName, right.versionName)
                        }
                    )

                if (newest == null) {
                    UpdateUiState(
                        status = UpdateStatus.UP_TO_DATE,
                        checkedAtMillis = checkedAt
                    )
                } else {
                    UpdateUiState(
                        status = UpdateStatus.AVAILABLE,
                        release = newest,
                        checkedAtMillis = checkedAt
                    )
                }
            }.getOrElse { error ->
                UpdateUiState(
                    status = UpdateStatus.ERROR,
                    error = friendlyError(error),
                    checkedAtMillis = checkedAt
                )
            }
        }

    suspend fun download(context: Context, release: BetaRelease): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                check(
                    DaylineVersion.isInstallableUpdate(
                        candidateVersion = release.versionName,
                        currentVersion = BuildConfig.VERSION_NAME,
                        currentVersionCode = BuildConfig.VERSION_CODE.toLong()
                    )
                ) {
                    "This Dayline beta is already installed or is not newer than your current build."
                }

                val url = release.apkUrl ?: error("This release does not contain an APK")
                val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
                updateDir.listFiles()?.forEach { old -> if (old.isFile) old.delete() }
                val apk = File(
                    updateDir,
                    safeFileName(release.apkName ?: "dayline-${release.tagName}.apk")
                )
                downloadTo(url, apk, release.versionName)

                release.checksumUrl?.let { checksumUrl ->
                    val expected = getText(checksumUrl, release.versionName)
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
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return InstallResult.PermissionRequested
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.updates",
                apk
            )
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            InstallResult.Started
        }.getOrElse {
            InstallResult.Error(it.message ?: "Could not open the Android installer")
        }
    }

    fun openRelease(context: Context, release: BetaRelease) {
        val url = release.htmlUrl.takeIf { it.isNotBlank() } ?: return
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
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
            "This Dayline beta is already installed or the downloaded APK has an older versionCode."
        }
    }

    private fun cleanNotes(value: String): String {
        if (value.isBlank()) return "Bug fixes and Dayline polish."
        return value
            .lineSequence()
            .map { line ->
                val trimmed = line.trim()
                    .removePrefix("### ")
                    .removePrefix("## ")
                    .removePrefix("# ")
                    .removePrefix("- ")
                    .removePrefix("* ")
                if (trimmed.startsWith("Full Changelog", ignoreCase = true)) {
                    "Full changelog available on GitHub."
                } else {
                    trimmed
                }
            }
            .filter { it.isNotBlank() && !it.startsWith("http://") && !it.startsWith("https://") }
            .distinct()
            .take(12)
            .joinToString("\n")
            .take(1_500)
    }

    private fun friendlyError(error: Throwable): String = when (error) {
        is UnknownHostException -> "Couldn't reach GitHub. Check your connection."
        is SocketTimeoutException -> "GitHub took too long to respond."
        else -> error.message?.takeIf { it.isNotBlank() }
            ?: "Couldn't check for updates."
    }

    private fun getText(url: String, version: String): String {
        val connection = open(url, version)
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadTo(url: String, destination: File, version: String) {
        val connection = open(url, version)
        try {
            connection.inputStream.use { input ->
                destination.outputStream().buffered().use { output -> input.copyTo(output) }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String, version: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "$USER_AGENT_PREFIX$version")
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        connection.connect()
        val code = connection.responseCode
        if (code !in 200..299) {
            val message = when (code) {
                404 -> "GitHub releases are not publicly reachable yet. Make the Dayline repository public to use anonymous beta updates."
                403 -> "GitHub rate limit reached. Try again later."
                else -> "GitHub returned HTTP $code"
            }
            connection.disconnect()
            error(message)
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
}
