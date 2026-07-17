/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import com.blazify.music.BuildConfig
import com.blazify.music.ui.screens.CrashActivity
import timber.log.Timber
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class CrashHandler private constructor(
    private val applicationContext: Context
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val crashLog = buildCrashLog(throwable)
            Timber.e(throwable, "App crashed")

            // ALWAYS persist to disk FIRST: when the app crashes in the
            // background (music playing, screen off) Android 10+ silently
            // blocks the activity launch below and the process just dies —
            // without this file those "random crashes" leave no trace at all.
            persistCrashLog(crashLog)

            // Launch crash activity
            val intent = Intent(applicationContext, CrashActivity::class.java).apply {
                putExtra(EXTRA_CRASH_LOG, crashLog)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            applicationContext.startActivity(intent)
            
            // Kill the current process
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(1)
        } catch (e: Exception) {
            // If we fail to handle the crash, fall back to default handler
            Timber.e(e, "Error handling crash")
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Write the crash log to files the developer can actually retrieve later:
     * the app's external files dir (visible over adb/USB at
     * Android/data/com.blazify.music/files/crashlogs) with the internal files
     * dir as fallback. Keeps the 5 most recent logs.
     */
    private fun persistCrashLog(crashLog: String) {
        try {
            val base = applicationContext.getExternalFilesDir(null) ?: applicationContext.filesDir
            val dir = File(base, "crashlogs")
            if (!dir.isDirectory) dir.mkdirs()
            val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            File(dir, "crash_$stamp.txt").writeText(crashLog)
            dir.listFiles()
                ?.sortedByDescending { it.name }
                ?.drop(5)
                ?.forEach { it.delete() }
        } catch (_: Exception) {
            // Persisting must never make crash handling itself crash.
        }
    }

    private fun buildCrashLog(throwable: Throwable): String {
        val stackTrace = StringWriter().apply {
            throwable.printStackTrace(PrintWriter(this))
        }.toString()

        return buildString {
            appendLine("Blazify Crash Report")
            appendLine("=".repeat(50))
            appendLine()
            appendLine("Manufacturer: ${Build.MANUFACTURER}")
            appendLine("Device: ${Build.MODEL}")
            appendLine("Android version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine()
            appendLine("=".repeat(50))
            appendLine("Stacktrace:")
            appendLine("=".repeat(50))
            appendLine()
            append(stackTrace)
        }
    }

    companion object {
        const val EXTRA_CRASH_LOG = "crash_log"

        fun install(context: Context) {
            val handler = CrashHandler(context.applicationContext)
            Thread.setDefaultUncaughtExceptionHandler(handler)
            Timber.d("CrashHandler installed")
        }
    }
}
