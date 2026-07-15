/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.ui.screens.library

import androidx.compose.runtime.Composable
import com.blazify.music.LocalNavController

@Composable
fun LibraryScreen() {
    val navController = LocalNavController.current
    BlazeLibraryHome(navController)
}
