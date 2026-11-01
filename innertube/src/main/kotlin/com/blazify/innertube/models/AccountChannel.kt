package com.blazify.innertube.models

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/** One channel a Google account can act as. [id] is what the requests are made on behalf of. */
data class AccountChannel(
    val name: String,
    val handle: String?,
    val thumbnailUrl: String?,
    val id: String,
    val isSelected: Boolean,
) {
    companion object {
        fun from(item: JsonObject): AccountChannel? {
            val name = item.text("accountName") ?: return null
            val tokens =
                ((item["serviceEndpoint"] as? JsonObject)
                    ?.get("selectActiveIdentityEndpoint") as? JsonObject)
                    ?.get("supportedTokens") as? JsonArray
            val syncToken =
                tokens?.firstNotNullOfOrNull { token ->
                    ((token as? JsonObject)?.get("datasyncIdToken") as? JsonObject)
                        ?.get("datasyncIdToken")?.jsonPrimitive?.content
                }
            val pageId =
                tokens?.firstNotNullOfOrNull { token ->
                    ((token as? JsonObject)?.get("pageIdToken") as? JsonObject)
                        ?.get("pageId")?.jsonPrimitive?.content
                }
            val id = syncToken?.substringBefore("||") ?: pageId ?: return null
            val photo =
                ((item["accountPhoto"] as? JsonObject)?.get("thumbnails") as? JsonArray)
                    ?.lastOrNull()
                    ?.let { (it as? JsonObject)?.get("url")?.jsonPrimitive?.content }
            return AccountChannel(
                name = name,
                handle = item.text("channelHandle") ?: item.text("accountByline"),
                thumbnailUrl = photo,
                id = id,
                isSelected = (item["isSelected"] as? kotlinx.serialization.json.JsonPrimitive)?.content == "true",
            )
        }

        private fun JsonObject.text(key: String): String? {
            val value = this[key] as? JsonObject ?: return null
            return value["simpleText"]?.jsonPrimitive?.content
                ?: (value["runs"] as? JsonArray)
                    ?.joinToString("") { ((it as? JsonObject)?.get("text"))?.jsonPrimitive?.content.orEmpty() }
                    ?.takeIf { it.isNotEmpty() }
        }
    }
}
