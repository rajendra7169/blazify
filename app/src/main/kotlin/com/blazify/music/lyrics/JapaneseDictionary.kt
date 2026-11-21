/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See NOTICE for contributors
 */

package com.blazify.music.lyrics

import android.content.Context
import com.atilika.kuromoji.dict.CharacterDefinitions
import com.atilika.kuromoji.dict.ConnectionCosts
import com.atilika.kuromoji.dict.InsertedDictionary
import com.atilika.kuromoji.dict.TokenInfoDictionary
import com.atilika.kuromoji.dict.UnknownDictionary
import com.atilika.kuromoji.ipadic.Tokenizer
import com.atilika.kuromoji.trie.DoubleArrayTrie
import com.atilika.kuromoji.util.ResourceResolver
import com.blazify.music.utils.SecureHttp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * The Japanese reading dictionary that turns Japanese lyrics into English letters.
 *
 * It is 13 MB, half of the app, and only people who read Japanese lyrics need it, so it is
 * no longer packed into the APK. The first time it would be used, Blazify asks, then downloads
 * the official kuromoji-ipadic 0.9.0 jar from Maven Central, checks its SHA-256 and keeps the
 * dictionary files in app storage.
 */
object JapaneseDictionary {
    sealed interface State {
        data object NotInstalled : State
        data class Downloading(val progress: Float) : State
        data object Installed : State
        data object Failed : State
    }

    /** Download size shown to the user, in MB. */
    const val SIZE_MB = 13

    private const val VERSION = "0.9.0"
    private const val JAR = "kuromoji-ipadic-$VERSION.jar"
    private val SOURCES = listOf(
        "https://repo1.maven.org/maven2/com/atilika/kuromoji/kuromoji-ipadic/$VERSION/$JAR",
        "https://repo.maven.apache.org/maven2/com/atilika/kuromoji/kuromoji-ipadic/$VERSION/$JAR",
    )
    private const val SHA256 = "24909fd751c0b439f7af5131b080eb65bc85062f0c4977ca4a71c76abe74e0b6"
    private const val ENTRY_PREFIX = "com/atilika/kuromoji/ipadic/"
    private val FILES = listOf(
        DoubleArrayTrie.DOUBLE_ARRAY_TRIE_FILENAME,
        ConnectionCosts.CONNECTION_COSTS_FILENAME,
        TokenInfoDictionary.TOKEN_INFO_DICTIONARY_FILENAME,
        TokenInfoDictionary.FEATURE_MAP_FILENAME,
        TokenInfoDictionary.POS_MAP_FILENAME,
        TokenInfoDictionary.TARGETMAP_FILENAME,
        CharacterDefinitions.CHARACTER_DEFINITIONS_FILENAME,
        UnknownDictionary.UNKNOWN_DICTIONARY_FILENAME,
    )

    private lateinit var dir: File
    private lateinit var cacheDir: File

    private val _state = MutableStateFlow<State>(State.NotInstalled)
    val state: StateFlow<State> = _state.asStateFlow()

    /** Turns true when Japanese lyrics could have been romanized but the dictionary is missing. */
    private val _wanted = MutableStateFlow(false)
    val wanted: StateFlow<Boolean> = _wanted.asStateFlow()

    @Volatile private var tokenizer: Tokenizer? = null

    fun init(context: Context) {
        dir = File(context.filesDir, "kuromoji-ipadic-$VERSION")
        cacheDir = context.cacheDir
        _state.value = if (isInstalled()) State.Installed else State.NotInstalled
    }

    private fun isInstalled() = ::dir.isInitialized && File(dir, "complete").exists()

    /** The tokenizer, or null (and a note that it was wanted) when the dictionary isn't downloaded. */
    fun tokenizer(): Tokenizer? {
        tokenizer?.let { return it }
        if (!isInstalled()) {
            _wanted.value = true
            return null
        }
        return synchronized(this) {
            tokenizer ?: runCatching { FileTokenizerBuilder(dir).build() }
                .onFailure {
                    // A damaged copy: drop it so the user can download it again
                    Timber.e(it, "Japanese dictionary could not be loaded")
                    delete()
                }
                .getOrNull()
                .also { tokenizer = it }
        }
    }

    // Downloads outlive the screen that started them
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun startDownload() {
        if (_state.value is State.Downloading || isInstalled()) return
        scope.launch { download() }
    }

    private suspend fun download(): Boolean = withContext(Dispatchers.IO) {
        if (isInstalled()) return@withContext true
        _state.value = State.Downloading(0f)
        val jar = File(cacheDir, JAR)
        try {
            val fetched = SOURCES.any { url -> fetch(url, jar) }
            check(fetched) { "No source answered" }
            check(sha256(jar) == SHA256) { "The download does not match the official file" }

            val staging = File(dir.parentFile, dir.name + ".part").apply { deleteRecursively(); mkdirs() }
            ZipFile(jar).use { zip ->
                for (name in FILES) {
                    val entry = checkNotNull(zip.getEntry(ENTRY_PREFIX + name)) { "Missing $name" }
                    zip.getInputStream(entry).use { input ->
                        File(staging, name).outputStream().use { input.copyTo(it) }
                    }
                }
            }
            File(staging, "complete").writeText(VERSION)
            dir.deleteRecursively()
            check(staging.renameTo(dir)) { "Could not move the dictionary into place" }
            _state.value = State.Installed
            _wanted.value = false
            true
        } catch (e: Exception) {
            Timber.e(e, "Japanese dictionary download failed")
            _state.value = State.Failed
            false
        } finally {
            jar.delete()
        }
    }

    fun delete() {
        tokenizer = null
        if (::dir.isInitialized) dir.deleteRecursively()
        _state.value = State.NotInstalled
    }

    private fun fetch(url: String, target: File): Boolean = runCatching {
        SecureHttp.client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (response.code != 200) return@runCatching false
            val body = response.body ?: return@runCatching false
            val total = body.contentLength().takeIf { it > 0 }
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total != null) _state.value = State.Downloading(done.toFloat() / total)
                    }
                }
            }
            true
        }
    }.getOrElse {
        Timber.w(it, "Japanese dictionary source failed: %s", url)
        false
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * kuromoji's ipadic builder reads its files from the classpath. This one does exactly the
     * same loading (same defaults as Tokenizer.Builder 0.9.0) but from the downloaded folder.
     */
    private class FileTokenizerBuilder(private val dir: File) : Tokenizer.Builder() {
        override fun loadDictionaries() {
            penalties = arrayListOf(2, 3000, 7, 1700)
            resolver = ResourceResolver { name -> File(dir, name).inputStream().buffered() }
            doubleArrayTrie = DoubleArrayTrie.newInstance(resolver)
            connectionCosts = ConnectionCosts.newInstance(resolver)
            tokenInfoDictionary = TokenInfoDictionary.newInstance(resolver)
            characterDefinitions = CharacterDefinitions.newInstance(resolver)
            unknownDictionary = UnknownDictionary.newInstance(resolver, characterDefinitions, totalFeatures)
            insertedDictionary = InsertedDictionary(totalFeatures)
        }
    }
}
