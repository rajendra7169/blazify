/**
 * Blazify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.blazify.music.utils

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import com.blazify.music.R

/**
 * Where music is currently coming out, and how to let the user change it.
 *
 * Apps cannot move media audio to an arbitrary device themselves — routing is the
 * system's call. So switching hands off to the platform output switcher, and this
 * file's job is only to name the current route and open that dialog.
 */
object AudioOutput {

    /** Friendly name of the device currently playing media, or null if unknown. */
    fun currentDeviceName(context: Context): String? {
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return null

        // API 31+ can report the device actually chosen for media playback; below
        // that, fall back to picking the most likely one from the output list.
        val device: AudioDeviceInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            runCatching { audioManager.getAudioDevicesForAttributes(attrs).firstOrNull() }.getOrNull()
        } else {
            null
        } ?: bestGuessDevice(audioManager)

        return device?.let { nameFor(context, it) }
    }

    /**
     * Opens the system media output switcher, which lists the phone speaker,
     * connected Bluetooth devices and any cast targets. Falls back to Bluetooth
     * settings where that panel does not exist.
     *
     * @return false if nothing could be opened.
     */
    fun openOutputSwitcher(context: Context): Boolean {
        // The output switcher has no public Intent constant, and OEMs disagree on
        // which action reaches it, so try the known ones before giving up and
        // showing Bluetooth settings — which at least gets the user to a pairing
        // list on devices where the panel is missing.
        val candidates = listOf(
            Intent("com.android.settings.panel.action.MEDIA_OUTPUT"),
            Intent("android.settings.MEDIA_OUTPUT"),
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
        )
        for (intent in candidates) {
            intent.putExtra(EXTRA_PACKAGE_NAME, context.packageName)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                return runCatching { context.startActivity(intent) }.isSuccess
            }
        }
        return false
    }

    private const val EXTRA_PACKAGE_NAME = "com.android.settings.panel.extra.PACKAGE_NAME"

    /** Highest-priority connected output, mirroring how Android itself routes media. */
    private fun bestGuessDevice(audioManager: AudioManager): AudioDeviceInfo? {
        val outputs = runCatching {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        }.getOrDefault(emptyList())
        val priority = listOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
        )
        return priority.firstNotNullOfOrNull { type -> outputs.firstOrNull { it.type == type } }
            ?: outputs.firstOrNull()
    }

    fun nameFor(context: Context, device: AudioDeviceInfo): String {
        @StringRes val fallback = when (device.type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> R.string.output_phone_speaker
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            -> R.string.output_wired_headphones
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            -> R.string.output_usb
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            -> R.string.output_bluetooth
            else -> R.string.output_unknown
        }
        // Bluetooth devices carry their own name; built-in ones report the phone
        // model, which is noise next to a plain "Phone speaker".
        val product = device.productName?.toString()?.trim().orEmpty()
        val isNamed = device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            device.type == AudioDeviceInfo.TYPE_USB_DEVICE
        return if (isNamed && product.isNotEmpty()) product else context.getString(fallback)
    }
}

/** An output the user can pick, paired with the platform handle needed to route to it. */
data class AudioOutputDevice(
    val id: Int,
    val name: String,
    val type: Int,
    val device: AudioDeviceInfo,
)

/**
 * Outputs currently connected and usable for media.
 *
 * The platform reports several entries per physical device (one per encoding), so
 * these are collapsed by name and type — otherwise a single pair of headphones
 * shows up three times.
 */
fun availableAudioOutputs(context: Context): List<AudioOutputDevice> {
    val audioManager = context.getSystemService(AudioManager::class.java) ?: return emptyList()
    val interesting = setOf(
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_HEARING_AID,
    )
    return runCatching { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList() }
        .getOrDefault(emptyList())
        .filter { it.type in interesting }
        .map { AudioOutputDevice(it.id, AudioOutput.nameFor(context, it), it.type, it) }
        .distinctBy { it.name to it.type }
}
