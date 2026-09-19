package com.blazify.music.ui.component

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blazify.music.LocalDatabase
import com.blazify.music.R
import com.blazify.music.utils.SpotifyImport
import com.blazify.music.utils.SpotifyPlaylist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

/** Spotify's green, used only where their own mark is drawn. */
private val SpotifyGreen = Color(0xFF1ED760)

/**
 * Paste a Spotify link, get the playlist here.
 *
 * The work is a search per track, so it takes a while: while it runs, songs
 * travel from Spotify's mark to Blazify's, and the count says how far along it
 * is. At the end it says plainly how many were found and how many were not,
 * rather than leaving somebody to count.
 */
@Composable
fun SpotifyImportDialog(
    onDismiss: () -> Unit,
    onImported: (String) -> Unit,
) {
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    var link by remember { mutableStateOf(TextFieldValue("")) }
    var running by remember { mutableStateOf(false) }
    var done by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var outcome by remember { mutableStateOf<SpotifyImport.Outcome?>(null) }
    var failed by remember { mutableStateOf(false) }

    val result = outcome
    val looksRight = SpotifyPlaylist.parseLink(link.text) != null

    DefaultDialog(
        onDismiss = { if (!running) onDismiss() },
        buttons = {
            if (result != null) {
                TextButton(onClick = { onImported(result.playlistId) }) {
                    Text(stringResource(R.string.open_playlist))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
            } else {
                TextButton(enabled = !running, onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    enabled = looksRight && !running,
                    onClick = {
                        running = true
                        failed = false
                        scope.launch(Dispatchers.IO) {
                            SpotifyImport
                                .import(link.text, database) { progress ->
                                    done = progress.done
                                    total = progress.total
                                }.onSuccess { outcome = it }
                                .onFailure { failed = true }
                            running = false
                        }
                    },
                ) {
                    Text(stringResource(R.string.import_action))
                }
            }
        },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (landscape) 8.dp else 14.dp),
            // Sideways there is little height; the contents scroll rather than push the
            // buttons off the screen.
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            TravellingSongs(
                running = running,
                finished = result != null,
                fraction = if (total > 0) done.toFloat() / total else 0f,
                compact = landscape,
            )

            Text(
                text = stringResource(R.string.import_spotify),
                style = if (landscape) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            when {
                result != null -> {
                    Text(
                        text = stringResource(R.string.import_spotify_done, result.matched, result.total),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    if (result.missing.isNotEmpty()) {
                        Note(stringResource(R.string.import_spotify_missing, result.missing.take(5).joinToString(", ")))
                    }
                    if (result.mayHaveMore) {
                        Note(stringResource(R.string.import_spotify_only_hundred))
                    }
                }

                running -> {
                    Text(
                        text =
                            if (total == 0) {
                                stringResource(R.string.import_spotify_reading)
                            } else {
                                stringResource(R.string.import_spotify_progress, done, total)
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    if (total == 0) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().clip(CircleShape),
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { done.toFloat() / total },
                            modifier = Modifier.fillMaxWidth().clip(CircleShape),
                        )
                    }
                }

                else -> {
                    TextField(
                        value = link,
                        onValueChange = {
                            link = it
                            failed = false
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        // The same field the app's other dialogs use.
                        colors = OutlinedTextFieldDefaults.colors(),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.link),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        label = { Text(stringResource(R.string.import_spotify_link)) },
                        isError = link.text.isNotBlank() && !looksRight,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (failed) {
                        Text(
                            text = stringResource(R.string.import_spotify_failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Note(stringResource(R.string.import_spotify_hint))
                    }
                }
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/**
 * Spotify on one side, Blazify on the other, and the songs crossing between them.
 *
 * Spotify starts as the larger of the two, since that is where the music is. As songs arrive the
 * weight shifts: Blazify grows, Spotify settles back, and both glow while the work is going on.
 * When it finishes the glow stops, which is how the picture says "done" without a word.
 */
@Composable
private fun TravellingSongs(
    running: Boolean,
    finished: Boolean,
    fraction: Float,
    compact: Boolean,
) {
    val base = if (compact) 40.dp else 56.dp
    val accent = MaterialTheme.colorScheme.primary

    // They start level. As songs arrive the weight shifts across: Blazify grows, Spotify
    // settles back, and it stays that way once everything is here.
    val shift by animateFloatAsState(
        targetValue = if (finished) 1f else fraction,
        animationSpec = tween(durationMillis = 600),
        label = "shift",
    )
    val spotifySize = base * lerp(1f, 0.82f, shift)
    val blazifySize = base * lerp(1f, 1.2f, shift)

    val pulse by rememberInfiniteTransition(label = "import").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulse",
    )
    val flight by rememberInfiniteTransition(label = "flight").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "travel",
    )
    // The glow belongs to the work: it fades in when it starts and out when it is over.
    val glow by animateFloatAsState(
        targetValue = if (running) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "glow",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(base * 1.6f).padding(top = 4.dp),
    ) {
        Badge(
            size = spotifySize,
            glow = glow * (0.45f + 0.55f * pulse),
            glowColor = SpotifyGreen,
        ) {
            Icon(
                painter = painterResource(R.drawable.spotify),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(spotifySize),
            )
        }

        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(base)
                .padding(horizontal = 6.dp),
        ) {
            val y = size.height / 2f
            // Nothing is drawn between the two marks but the songs on their way across.
            if (!running) return@Canvas

            val notes = 5
            repeat(notes) { index ->
                // Each note starts a fifth of a lap after the one before it, so they
                // arrive one at a time rather than in a clump.
                val position = (flight + index / notes.toFloat()) % 1f
                val lift = sin(position * Math.PI).toFloat()
                val radius = (2.5f + 2f * lift) * density
                val fade = (1f - kotlin.math.abs(position - 0.5f) * 1.6f).coerceIn(0f, 1f)
                drawCircle(
                    color = if (index % 2 == 0) accent else SpotifyGreen,
                    radius = radius,
                    center = Offset(position * size.width, y - lift * size.height * 0.22f * seed(index)),
                    alpha = fade,
                )
            }
        }

        Badge(
            size = blazifySize,
            glow = glow * (0.45f + 0.55f * (1f - pulse)),
            glowColor = accent,
        ) {
            Image(
                painter = painterResource(R.drawable.blaze_logo),
                contentDescription = null,
                // The flame sits inside its own margin, so it is drawn a little larger to
                // stand level with Spotify's mark rather than looking smaller than it.
                modifier = Modifier.size(blazifySize * 1.22f),
            )
        }
    }
}

/** A fixed wobble per note, so they do not all fly the same arc. */
private fun seed(index: Int) = Random(index).nextFloat() * 0.8f + 0.6f

@Composable
private fun Badge(
    size: Dp,
    glow: Float,
    glowColor: Color,
    content: @Composable () -> Unit,
) {
    Box(contentAlignment = Alignment.Center) {
        if (glow > 0.01f) {
            Box(
                modifier = Modifier
                    .size(size * 1.55f)
                    .background(
                        Brush.radialGradient(
                            listOf(glowColor.copy(alpha = 0.38f * glow), Color.Transparent),
                        ),
                        CircleShape,
                    ),
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size),
        ) {
            content()
        }
    }
}
