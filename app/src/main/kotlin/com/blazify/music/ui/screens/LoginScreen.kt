/**
 * Blazify Project (C) 2026
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.blazify.innertube.YouTube
import com.blazify.music.LocalPlayerAwareWindowInsets
import com.blazify.music.R
import com.blazify.music.constants.AccountChannelHandleKey
import com.blazify.music.constants.AccountEmailKey
import com.blazify.music.constants.AccountNameKey
import com.blazify.music.constants.DataSyncIdKey
import com.blazify.music.constants.InnerTubeCookieKey
import com.blazify.music.constants.VisitorDataKey
import com.blazify.music.ui.component.IconButton
import com.blazify.music.ui.utils.backToMain
import com.blazify.music.utils.reportException
import com.blazify.music.utils.safeDataStoreEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCompletingLogin by remember { mutableStateOf(false) }

    var webView: WebView? = null
    var visitorDataFromWeb by remember { mutableStateOf("") }
    var dataSyncIdFromWeb by remember { mutableStateOf("") }
    val accountsViewModel: com.blazify.music.viewmodels.AccountSettingsViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
    var channelChoice by remember {
        mutableStateOf<Pair<List<com.blazify.innertube.models.AccountChannel>, kotlinx.coroutines.CompletableDeferred<com.blazify.innertube.models.AccountChannel>>?>(null)
    }

    fun completeLogin(onClose: () -> Unit) {
        if (isCompletingLogin) return

        isCompletingLogin = true
        coroutineScope.launch {
            val currentCookie = CookieManager.getInstance().getCookie("https://music.youtube.com").orEmpty()
            if (currentCookie.isBlank()) {
                Timber.d("Login: No YouTube Music cookie found on close, leaving login screen")
                isCompletingLogin = false
                onClose()
                return@launch
            }

            // Put back if this comes to nothing, so backing out of adding an
            // account leaves the one in use working.
            val previousCookie = YouTube.cookie

            // The cookie first, so anything fetched below belongs to the
            // account that was just signed in to rather than to nobody.
            YouTube.cookie = currentCookie

            // The page is asked for these through JavaScript, and JavaScript
            // does not answer in the same breath as the question. This used to
            // read them the instant the page finished — before the answer had
            // come back — and save two empty strings over the top of whatever
            // was there. An empty identity is refused by the catalogue on
            // every play and every download, which is why signing in broke
            // playing and signing out fixed it.
            var waited = 0
            while (visitorDataFromWeb.isBlank() && waited < 4000) {
                delay(200)
                waited += 200
            }

            val savedVisitorData = visitorDataFromWeb.trim()
                .takeIf { it.isNotEmpty() && it != "null" && it != "undefined" }
                // Never nothing: if the page would not say, the catalogue is
                // asked directly, and asked while holding the cookie so the
                // answer belongs to this account.
                ?: YouTube.visitorData().getOrNull()

            val savedDataSyncId = dataSyncIdFromWeb.trim()
                .takeIf { it.isNotEmpty() && it != "null" && it != "undefined" }

            YouTube.dataSyncId = savedDataSyncId
            YouTube.visitorData = savedVisitorData

            Timber.d("Login: Manual close detected, validating selected account...")

            YouTube
                .accountInfo()
                .onSuccess { info ->
                    Timber.d("Login: Successfully logged in as ${info.name}, restarting app...")

                    // Clean up WebView
                    webView?.apply {
                        stopLoading()
                        clearHistory()
                        clearCache(true)
                        clearFormData()
                    }

                    // More than one channel on this account: which one is a real question,
                    // since someone's playlists can all be on the one Google didn't pick.
                    val channels = YouTube.accountChannels().getOrDefault(emptyList())
                    val chosen =
                        if (channels.size > 1) {
                            val answer = kotlinx.coroutines.CompletableDeferred<com.blazify.innertube.models.AccountChannel>()
                            channelChoice = channels to answer
                            answer.await().also { channelChoice = null }
                        } else {
                            null
                        }

                    // Adding an account: keep the one being left on the list, and take
                    // what was synced for it out of the library so the two don't mix.
                    if (!previousCookie.isNullOrBlank() && previousCookie != currentCookie) {
                        com.blazify.music.utils.AccountSwitcher.rememberCurrent(context)
                        accountsViewModel.clearSyncedContent()
                    }

                    // Save ALL credentials atomically to DataStore, then restart the app.
                    // This replicates the exact token login pattern (saveTokenAndRestart) that
                    // works reliably: write atomically, then start the
                    // launch intent and exit the process so all services reinitialize cleanly.
                    val saved = withContext(Dispatchers.IO) {
                        context.safeDataStoreEdit { settings ->
                            settings[InnerTubeCookieKey] = currentCookie
                            // Written only where there is something to write.
                            // A key holding an empty string is read back as a
                            // value, and read back as a value it is used.
                            savedVisitorData?.let { settings[VisitorDataKey] = it }
                                ?: settings.remove(VisitorDataKey)
                            (chosen?.id ?: savedDataSyncId)?.let { settings[DataSyncIdKey] = it }
                                ?: settings.remove(DataSyncIdKey)
                            settings[AccountNameKey] = chosen?.name ?: info.name
                            settings[AccountEmailKey] = info.email.orEmpty()
                            settings[AccountChannelHandleKey] = chosen?.handle ?: info.channelHandle.orEmpty()
                        }
                    }

                    if (!saved) {
                        Timber.e("Login: Failed to persist account data")
                        isCompletingLogin = false
                        return@onSuccess
                    }

                    com.blazify.music.utils.AccountSwitcher.rememberCurrent(context, chosen?.thumbnailUrl ?: info.thumbnailUrl)

                    withContext(Dispatchers.Main) {
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        context.startActivity(intent)
                        Runtime.getRuntime().exit(0)
                    }
                }.onFailure {
                    Timber.e(it, "Login: Authentication validation failed after manual close")
                    YouTube.cookie = previousCookie
                    reportException(it)
                    isCompletingLogin = false
                    onClose()
                }
        }
    }

    AndroidView(
        modifier =
            Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .fillMaxSize(),
        factory = { webViewContext ->
            WebView(webViewContext).apply {
                webViewClient =
                    object : WebViewClient() {
                        override fun onPageFinished(
                            view: WebView,
                            url: String?,
                        ) {
                            loadUrl("javascript:Android.onRetrieveVisitorData(window.yt.config_.VISITOR_DATA)")
                            loadUrl("javascript:Android.onRetrieveDataSyncId(window.yt.config_.DATASYNC_ID)")

                            // Auto-detect login completion: when the WebView lands on
                            // music.youtube.com with a valid cookie, complete the login.
                            if (url?.contains("music.youtube.com") == true &&
                                !isCompletingLogin &&
                                CookieManager.getInstance().getCookie("https://music.youtube.com").orEmpty()
                                    .isNotBlank()
                            ) {
                                Timber.d("Login: Detected authenticated session on music.youtube.com, completing login...")
                                completeLogin(navController::navigateUp)
                            }
                        }
                    }
                settings.apply {
                    javaScriptEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                }
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onRetrieveVisitorData(newVisitorData: String?) {
                            if (newVisitorData != null) {
                                visitorDataFromWeb = newVisitorData
                            }
                        }

                        @JavascriptInterface
                        fun onRetrieveDataSyncId(newDataSyncId: String?) {
                            if (newDataSyncId != null) {
                                dataSyncIdFromWeb = newDataSyncId.substringBefore("||")
                            }
                        }
                    },
                    "Android",
                )
                webView = this
                loadUrl("https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com")
            }
        },
    )

    TopAppBar(
        title = { Text(stringResource(R.string.login)) },
        navigationIcon = {
            IconButton(
                onClick = { completeLogin(navController::navigateUp) },
                onLongClick = { completeLogin(navController::backToMain) },
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )

    channelChoice?.let { (channels, answer) ->
        val active = com.blazify.music.ui.screens.settings.activeChannel(channels, dataSyncIdFromWeb)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { active?.let(answer::complete) },
            title = { Text(androidx.compose.ui.res.stringResource(R.string.accounts_choose_channel)) },
            text = {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                ) {
                    Text(androidx.compose.ui.res.stringResource(R.string.accounts_choose_channel_note))
                    channels.forEach { channel ->
                        com.blazify.music.ui.screens.settings.AccountRow(
                            name = channel.name,
                            detail = channel.handle.orEmpty(),
                            photo = channel.thumbnailUrl,
                            selected = channel.id == active?.id,
                            onClick = { answer.complete(channel) },
                        )
                    }
                }
            },
            confirmButton = {},
        )
    }

    BackHandler {
        val currentWebView = webView
        if (currentWebView?.canGoBack() == true) {
            currentWebView.goBack()
        } else {
            completeLogin(navController::navigateUp)
        }
    }
}
