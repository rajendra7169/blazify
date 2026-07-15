package com.blazify.innertube.models.body

import com.blazify.innertube.models.Context
import com.blazify.innertube.models.Continuation
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String?,
    val continuation: String?
)
