/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blazify.music.App
import com.blazify.music.constants.AccountChannelHandleKey
import com.blazify.music.constants.AccountEmailKey
import com.blazify.music.constants.AccountNameKey
import com.blazify.music.constants.DataSyncIdKey
import com.blazify.music.constants.InnerTubeCookieKey
import com.blazify.music.constants.VisitorDataKey
import com.blazify.music.utils.SyncUtils
import com.blazify.music.utils.dataStore
import com.blazify.music.utils.safeDataStoreEdit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val syncUtils: SyncUtils,
) : ViewModel() {

    /**
     * Logout user and clear all synced content to prevent data mixing between accounts
     */
    fun logoutAndClearSyncedContent(context: Context, onCookieChange: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            // Clear all YouTube Music synced content first
            syncUtils.clearAllSyncedContent()

            // Then clear account preferences
            App.forgetAccount(context)

            // Clear cookie in UI
            onCookieChange("")
        }
    }

    /**
     * Clear all library data including songs, albums, artists, playlists, podcasts.
     */
    suspend fun clearAllLibraryData() {
        Timber.d("[LOGOUT_CLEAR] ViewModel: clearAllLibraryData called")
        syncUtils.clearAllLibraryData()
        Timber.d("[LOGOUT_CLEAR] ViewModel: clearAllLibraryData completed")
    }

    /**
     * Forget the account FIRST (clearing auth so all background syncs skip),
     * THEN clear all library data. This prevents sync operations that are
     * triggered by the database becoming empty from re-adding songs.
     */
    suspend fun logoutAndClearLibraryData(context: Context) {
        Timber.d("[LOGOUT_CLEAR] ViewModel: logoutAndClearLibraryData called")
        withContext(Dispatchers.IO) {
            // Forget account first — clears cookie/auth from DataStore.
            // Once isLoggedIn() returns false, ALL sync operations will skip.
            App.forgetAccount(context)

            // Now clear the local database. Any sync coroutines that observe
            // the empty state will check isLoggedIn() and skip silently.
            syncUtils.clearAllLibraryData()
        }
        Timber.d("[LOGOUT_CLEAR] ViewModel: logoutAndClearLibraryData completed")
    }

    /**
     * Just logout without clearing library data
     */
    suspend fun logoutKeepData(context: Context, onCookieChange: (String) -> Unit) {
        Timber.d("[LOGOUT_KEEP] ViewModel: logoutKeepData called")
        withContext(Dispatchers.IO) {
            App.forgetAccount(context)
        }
        Timber.d("[LOGOUT_KEEP] ViewModel: Account forgotten, clearing cookie in UI")
        onCookieChange("")
    }

    /**
     * Save token credentials atomically to DataStore, then restart the app.
     * This ensures all writes complete before the process is killed,
     * preventing the race condition where Runtime.exit(0) kills the process
     * before async DataStore coroutines finish writing.
     */
    fun saveTokenAndRestart(
        context: Context,
        cookie: String,
        visitorData: String,
        dataSyncId: String,
        accountName: String,
        accountEmail: String,
        accountChannelHandle: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val saved = context.safeDataStoreEdit { settings ->
                settings[InnerTubeCookieKey] = cookie
                settings[VisitorDataKey] = visitorData
                settings[DataSyncIdKey] = dataSyncId
                settings[AccountNameKey] = accountName
                settings[AccountEmailKey] = accountEmail
                settings[AccountChannelHandleKey] = accountChannelHandle
            }
            if (!saved) {
                Timber.e("saveTokenAndRestart: DataStore write failed — skipping restart to avoid losing credentials")
                return@launch
            }
            withContext(Dispatchers.Main) {
                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
                Runtime.getRuntime().exit(0)
            }
        }
    }
}
