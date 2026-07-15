/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blazify.music.R
import com.blazify.music.constants.DarkModeKey
import com.blazify.music.ui.screens.settings.DarkMode
import com.blazify.music.ui.theme.BlazeGradientEnd
import com.blazify.music.ui.theme.BlazeThemeColor
import com.blazify.music.utils.rememberEnumPreference
import java.util.Calendar

/**
 * Blazify home header, ported from the original Flutter app:
 * top row (account | logo + wordmark | settings), greeting card with the
 * hero image overflowing above the card, and a rounded search bar.
 */
@Composable
fun BlazeHomeHeader(
    userName: String = "Music Lover",
    onAccountClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onMicClick: () -> Unit = {},
) {
    val darkMode by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val isDark = if (darkMode == DarkMode.AUTO) isSystemInDarkTheme() else darkMode == DarkMode.ON
    val iconTint = if (isDark) Color.White else Color(0xDE000000)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header row: account | logo + "Blazify" | settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
        ) {
            IconButton(onClick = onAccountClick) {
                Icon(
                    painter = painterResource(R.drawable.person),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Image(
                    painter = painterResource(
                        if (isDark) R.drawable.blaze_logo_white else R.drawable.blaze_logo,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Blazify",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = if (isDark) {
                            Brush.linearGradient(listOf(Color.White, Color.White))
                        } else {
                            Brush.linearGradient(listOf(BlazeThemeColor, BlazeGradientEnd))
                        },
                    ),
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = painterResource(R.drawable.settings),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Greeting card: gradient background, text left, hero image overflowing the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(160.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (isDark) {
                                listOf(Color(0xFFFFA726), Color(0xFFFF8F00))
                            } else {
                                listOf(Color(0xFFFFA726), Color(0xFFFF7043))
                            },
                        ),
                    ),
            )

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(0.62f)
                    .padding(start = 20.dp),
            ) {
                Text(
                    text = greeting(),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                    lineHeight = 27.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = userName,
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Enjoy the music 🎵",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.2.sp,
                )
            }

            // Hero image: 200x240, bottom-aligned with the card, spilling 80dp above it
            Image(
                painter = painterResource(
                    if (isDark) R.drawable.blaze_home_dark else R.drawable.blaze_home_light,
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .requiredWidth(200.dp)
                    .requiredHeight(240.dp)
                    // requiredHeight overflows evenly (40dp top and bottom);
                    // shift up so the bottom edge sits flush with the card
                    .offset(y = (-40).dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }

        Spacer(Modifier.height(8.dp))

        // Search bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFEEEEEE))
                .clickable(onClick = onSearchClick)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.search),
                contentDescription = null,
                tint = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0x8A000000),
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Search songs, albums, artists...",
                color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0x8A000000),
                fontSize = 15.sp,
                modifier = Modifier.weight(1f),
            )
            // Song recognition
            Icon(
                painter = painterResource(R.drawable.mic),
                contentDescription = null,
                tint = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0x8A000000),
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onMicClick),
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..11 -> "Good\nMorning 🌅"
        hour in 12..16 -> "Good\nAfternoon ☀️"
        hour in 17..20 -> "Good\nEvening 🌆"
        else -> "Good\nNight 🌙"
    }
}
