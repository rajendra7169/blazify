/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.utils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.OutputStream
import java.security.MessageDigest

/**
 * Fetching a new version and handing it to the system installer.
 *
 * Two things here are easy to get wrong and expensive to get wrong.
 *
 * The first is where the file goes. The obvious place is the app's own storage,
 * and it is the one place it must not go: when a build is signed with a
 * different key the system refuses to install it over the old one, so the old
 * one has to be removed first — and removing it deletes the app's storage,
 * taking the downloaded file with it. Somebody follows the instruction and finds
 * they have uninstalled their music player with nothing to put back. So it is
 * written to the shared Downloads folder, which outlives the app that put it
 * there.
 *
 * The second is knowing which situation you are in. A same-key build installs
 * straight over the top and keeps everything; a different-key build cannot.
 * Telling somebody to uninstall when they did not have to would cost them their
 * library for nothing, so the certificate inside the downloaded file is compared
 * with the one already installed and the instruction given is the true one.
 */
object UpdateInstaller {

    private const val TAG = "UpdateInstaller"
    private const val APK_MIME = "application/vnd.android.package-archive"

    /**
     * The same HTTP stack the player already streams through.
     *
     * Not an arbitrary preference: measured on the phone this was built for, the
     * other client could not complete this download at all — it reported a
     * connect timeout against the host GitHub redirects release downloads to,
     * with sixty seconds allowed and the name resolving perfectly well. This one
     * fetches audio from a CDN on the same device all day.
     *
     * Timeouts are per-socket rather than overall, because the only sane limit
     * on tens of megabytes over an unknown connection is the connection going
     * quiet, not how long the whole thing takes.
     */
    private val http by lazy {
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /** Where a fetched build ended up, and what has to happen to it now. */
    data class Fetched(
        val uri: Uri,
        val displayName: String,
        /**
         * Whether the installed version has to be removed by hand first,
         * because this build carries a different signing certificate. Android
         * treats that as a different application whatever the name says, and
         * nothing in this app can work around it.
         */
        val needsUninstallFirst: Boolean,
    )

    /**
     * Download a release asset into the shared Downloads folder.
     *
     * [onProgress] receives bytes so far and the total, which is null when the
     * server does not declare one.
     */
    suspend fun fetch(
        context: Context,
        url: String,
        displayName: String,
        onProgress: (downloaded: Long, total: Long?) -> Unit,
    ): Result<Fetched> = withContext(Dispatchers.IO) {
        runCatching {
            val sink = openDownloadsSink(context, displayName)
            try {
                val request = okhttp3.Request.Builder().url(url).build()
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("HTTP ${response.code}")
                    val body = response.body ?: error("The server sent nothing")
                    val total = body.contentLength().takeIf { it > 0 }
                    sink.stream.use { out ->
                        val buffer = ByteArray(64 * 1024)
                        var done = 0L
                        body.byteStream().use { input ->
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                out.write(buffer, 0, read)
                                done += read
                                onProgress(done, total)
                            }
                        }
                        out.flush()
                    }
                }
                sink.publish()
            } catch (t: Throwable) {
                sink.abandon()
                throw t
            }

            val needsUninstall = !signedLikeInstalled(context, sink.uri)
            Timber.tag(TAG).d("Fetched $displayName; uninstall first: $needsUninstall")
            Fetched(sink.uri, displayName, needsUninstall)
        }.onFailure { Timber.tag(TAG).w(it, "Could not fetch the update") }
    }

    /**
     * Whether the system will let this build install over the one already here.
     *
     * A build whose certificate cannot be read counts as "it will not", which is
     * the safe way round: the worst outcome is an instruction to uninstall that
     * was not strictly needed, rather than an install that fails on a screen
     * offering nothing to do about it.
     */
    private fun signedLikeInstalled(context: Context, apk: Uri): Boolean = runCatching {
        val local = copyForInspection(context, apk) ?: return false
        try {
            val installed = certificatesOf(context, path = null)
            val incoming = certificatesOf(context, path = local.absolutePath)
            installed.isNotEmpty() && incoming.isNotEmpty() &&
                installed.intersect(incoming).isNotEmpty()
        } finally {
            local.delete()
        }
    }.getOrDefault(false)

    /** SHA-256 of every signing certificate, for the installed app or for a file. */
    @SuppressLint("PackageManagerGetSignatures")
    private fun certificatesOf(context: Context, path: String?): Set<String> {
        val pm = context.packageManager
        val flags = PackageManager.GET_SIGNING_CERTIFICATES
        val info = if (path == null) {
            pm.getPackageInfo(context.packageName, flags)
        } else {
            pm.getPackageArchiveInfo(path, flags)
        } ?: return emptySet()

        val signers = info.signingInfo?.let {
            if (it.hasMultipleSigners()) it.apkContentsSigners else it.signingCertificateHistory
        } ?: return emptySet()

        val digest = MessageDigest.getInstance("SHA-256")
        return signers.filterNotNull()
            .map { digest.digest(it.toByteArray()).joinToString("") { b -> "%02x".format(b) } }
            .toSet()
    }

    /**
     * The archive parser wants a real path and a MediaStore entry is not one, so
     * a copy is made purely to be read and deleted again.
     */
    private fun copyForInspection(context: Context, uri: Uri): File? = runCatching {
        val out = File(context.cacheDir, "update-inspect.apk")
        context.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { input.copyTo(it) }
        } ?: return null
        out
    }.getOrNull()

    /** Open the system installer on a fetched build. */
    fun install(context: Context, fetched: Fetched) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fetched.uri, APK_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { Timber.tag(TAG).w(it, "No installer would open the file") }
    }

    /**
     * Whether this app may ask for an install at all. Android grants that
     * per-app, and without it the installer opens on a screen that refuses
     * rather than on the install button.
     */
    fun mayInstall(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    /** Send somebody to the screen where that permission is granted. */
    fun requestInstallPermission(context: Context) {
        val direct = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(direct) }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    // --- writing into shared Downloads -------------------------------------

    private class Sink(
        val uri: Uri,
        val stream: OutputStream,
        val publish: () -> Unit,
        val abandon: () -> Unit,
    )

    private fun openDownloadsSink(context: Context, displayName: String): Sink =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val uri = resolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, APK_MIME)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                },
            ) ?: error("The Downloads folder would not accept the file")

            Sink(
                uri = uri,
                stream = resolver.openOutputStream(uri) ?: error("Could not write to Downloads"),
                publish = {
                    resolver.update(
                        uri,
                        ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                        null,
                        null,
                    )
                },
                abandon = { runCatching { resolver.delete(uri, null, null) } },
            )
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            val file = File(dir, displayName)
            Sink(
                uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.FileProvider",
                    file,
                ),
                stream = file.outputStream(),
                publish = {},
                abandon = { file.delete() },
            )
        }
}
