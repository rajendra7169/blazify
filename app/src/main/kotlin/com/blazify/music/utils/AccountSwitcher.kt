/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0
 */

package com.blazify.music.utils

import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import com.blazify.innertube.YouTube
import com.blazify.innertube.utils.parseCookieString
import com.blazify.music.constants.AccountChannelHandleKey
import com.blazify.music.constants.AccountEmailKey
import com.blazify.music.constants.AccountNameKey
import com.blazify.music.constants.DataSyncIdKey
import com.blazify.music.constants.InnerTubeCookieKey
import com.blazify.music.constants.SavedAccountsKey
import com.blazify.music.constants.VisitorDataKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Everything needed to be signed in to one account again, without typing a password. */
data class SavedAccount(
    val cookie: String,
    val visitorData: String,
    val dataSyncId: String,
    val name: String,
    val email: String,
    val handle: String,
    val photo: String,
) {
    /** One Google account with two channels is two entries, and the same channel is only ever one. */
    val key: String get() = "$email|${dataSyncId.substringBefore("||")}|$name"
}

/**
 * The accounts this phone has been signed in to, kept alongside the one in use.
 *
 * The rest of the app only knows the current account, in the same settings it has always read, so
 * changing account is writing another one there and starting over: every part of the app then comes
 * up as it does after signing in.
 */
object AccountSwitcher {
    suspend fun saved(context: Context): List<SavedAccount> =
        withContext(Dispatchers.IO) {
            read(context.dataStore.data.first()[SavedAccountsKey])
        }

    /** The account in use, added to the list or brought up to date in it. */
    suspend fun rememberCurrent(context: Context, photo: String? = null) {
        withContext(Dispatchers.IO) {
            context.safeDataStoreEdit { settings ->
                val cookie = settings[InnerTubeCookieKey].orEmpty()
                if ("SAPISID" !in parseCookieString(cookie)) return@safeDataStoreEdit
                val list = read(settings[SavedAccountsKey])
                val current =
                    SavedAccount(
                        cookie = cookie,
                        visitorData = settings[VisitorDataKey].orEmpty(),
                        dataSyncId = settings[DataSyncIdKey].orEmpty(),
                        name = settings[AccountNameKey].orEmpty(),
                        email = settings[AccountEmailKey].orEmpty(),
                        handle = settings[AccountChannelHandleKey].orEmpty(),
                        photo = "",
                    )
                val before = list.firstOrNull { it.key == current.key }
                val kept = current.copy(photo = photo ?: before?.photo.orEmpty())
                settings[SavedAccountsKey] = write(list.filterNot { it.key == kept.key } + kept)
            }
        }
    }

    suspend fun forget(context: Context, account: SavedAccount) {
        withContext(Dispatchers.IO) {
            context.safeDataStoreEdit { settings ->
                settings[SavedAccountsKey] =
                    write(read(settings[SavedAccountsKey]).filterNot { it.key == account.key })
            }
        }
    }

    /** Signing out of the account in use also takes it off the list: that is what signing out asks. */
    suspend fun forgetCurrent(context: Context) {
        withContext(Dispatchers.IO) {
            context.safeDataStoreEdit { settings ->
                val email = settings[AccountEmailKey].orEmpty()
                val id = settings[DataSyncIdKey].orEmpty().substringBefore("||")
                val name = settings[AccountNameKey].orEmpty()
                settings[SavedAccountsKey] =
                    write(read(settings[SavedAccountsKey]).filterNot { it.key == "$email|$id|$name" })
            }
        }
    }

    /**
     * Whether this account still answers. Asked before anything is changed, because a sign-in kept
     * for months can have been ended from the Google side, and finding that out after the switch
     * would leave someone signed out of the account they were happily using.
     */
    suspend fun stillSignedIn(account: SavedAccount): Boolean {
        val cookie = YouTube.cookie
        val dataSyncId = YouTube.dataSyncId
        val visitorData = YouTube.visitorData
        YouTube.cookie = account.cookie
        YouTube.dataSyncId = account.dataSyncId.substringBefore("||").ifBlank { null }
        val answer = YouTube.accountInfo().isSuccess
        YouTube.cookie = cookie
        YouTube.dataSyncId = dataSyncId
        YouTube.visitorData = visitorData
        return answer
    }

    /** Makes this the account in use and starts the app over on it. False if it couldn't be saved. */
    suspend fun use(context: Context, account: SavedAccount): Boolean {
        val written =
            withContext(Dispatchers.IO) {
                context.safeDataStoreEdit { settings ->
                    settings[InnerTubeCookieKey] = account.cookie
                    settings[DataSyncIdKey] = account.dataSyncId
                    settings[AccountNameKey] = account.name
                    settings[AccountEmailKey] = account.email
                    settings[AccountChannelHandleKey] = account.handle
                    if (account.visitorData.isBlank()) {
                        settings.remove(VisitorDataKey)
                    } else {
                        settings[VisitorDataKey] = account.visitorData
                    }
                }
            }
        if (!written) return false
        withContext(Dispatchers.Main) {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
            Runtime.getRuntime().exit(0)
        }
        return true
    }

    private fun read(text: String?): List<SavedAccount> =
        runCatching {
            val array = JSONArray(text.orEmpty().ifBlank { "[]" })
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                SavedAccount(
                    cookie = item.getString("cookie"),
                    visitorData = item.optString("visitorData"),
                    dataSyncId = item.optString("dataSyncId"),
                    name = item.optString("name"),
                    email = item.optString("email"),
                    handle = item.optString("handle"),
                    photo = item.optString("photo"),
                )
            }
        }.getOrDefault(emptyList())

    private fun write(accounts: List<SavedAccount>): String =
        JSONArray(
            accounts.map {
                JSONObject()
                    .put("cookie", it.cookie)
                    .put("visitorData", it.visitorData)
                    .put("dataSyncId", it.dataSyncId)
                    .put("name", it.name)
                    .put("email", it.email)
                    .put("handle", it.handle)
                    .put("photo", it.photo)
            },
        ).toString()
}
