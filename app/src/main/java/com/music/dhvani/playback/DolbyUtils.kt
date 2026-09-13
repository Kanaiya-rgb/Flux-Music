package com.music.dhvani.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast

object DolbyUtils {

    /**
     * Checks if the device has a genuine OEM Dolby Atmos control panel / package.
     * Stub AudioEffect descriptors in generic ROMs are ignored so non-Dolby phones
     * are correctly detected as not supported.
     */
    fun isDolbyAtmosAvailable(context: Context): Boolean {
        return isDolbyPanelAvailable(context)
    }

    /**
     * Checks whether an OEM Dolby Atmos settings panel activity can be resolved.
     * Prevents Android's fallback ResolverActivity ("Open with...") from returning a false positive.
     */
    fun isDolbyPanelAvailable(context: Context): Boolean {
        val pm = context.packageManager ?: return false
        return getDolbyIntents().any { intent ->
            runCatching {
                val resolved = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    pm.resolveActivity(intent, android.content.pm.PackageManager.ResolveInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.resolveActivity(intent, 0)
                }
                if (resolved != null && resolved.activityInfo != null) {
                    val pkg = resolved.activityInfo.packageName.lowercase()
                    val cls = resolved.activityInfo.name.lowercase()
                    // Filter out Android system fallback ResolverActivity
                    pkg != "android" && !pkg.contains("resolveractivity") &&
                        (pkg.contains("dolby") || cls.contains("dolby"))
                } else {
                    false
                }
            }.getOrDefault(false)
        }
    }

    /**
     * Launches the system Dolby Atmos or device equalizer panel.
     */
    fun openDolbyAtmos(context: Context) {
        for (intent in getDolbyIntents()) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {
            }
        }
        Toast.makeText(
            context,
            "No OEM Dolby Atmos panel found on this device. Dhvani's built-in 3D Spatial Audio is active!",
            Toast.LENGTH_LONG,
        ).show()
    }

    private fun getDolbyIntents(): List<Intent> = listOf(
        // Generic Dolby Atmos DAX applications (Motorola, Lenovo, Xiaomi Dolby, ZTE, Nokia)
        Intent().setComponent(ComponentName("com.dolby.daxappui", "com.dolby.daxappui.MainActivity")),
        Intent().setComponent(ComponentName("com.dolby.daxappui", "com.dolby.daxappui.Main")),
        Intent().setComponent(ComponentName("com.dolby.daxappui", "com.dolby.daxappui.ui.MainActivity")),
        Intent().setComponent(ComponentName("com.dolby.spatialaudio", "com.dolby.spatialaudio.MainActivity")),
        // Motorola Dolby Atmos dedicated UI
        Intent().setComponent(ComponentName("com.motorola.dolby.dolbyui", "com.motorola.dolby.dolbyui.ui.MainActivity")),
        Intent().setComponent(ComponentName("com.motorola.dolby.dolbyui", "com.motorola.dolby.dolbyui.MainActivity")),
        // OnePlus / Oppo / Realme Dolby Atmos dedicated activities
        Intent().setComponent(ComponentName("com.oneplus.sound", "com.oneplus.sound.dolby.DolbySettingsActivity")),
        Intent().setComponent(ComponentName("com.oplus.sound", "com.oplus.sound.dolby.DolbySettingsActivity")),
        Intent().setComponent(ComponentName("com.oneplus.sound.dolby", "com.oneplus.sound.dolby.MainActivity")),
        // Lenovo dedicated Dolby
        Intent().setComponent(ComponentName("com.lenovo.dolby", "com.lenovo.dolby.MainActivity")),
    )
}
