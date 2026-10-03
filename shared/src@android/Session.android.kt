package id.andreasmlbngaol.nas_project.data

import android.content.Context

/**
 * Session storage backed by SharedPreferences. The Android actual needs a
 * Context, which only exists once the app starts — [initAndroid] supplies it.
 */
private const val PREFS = "nas-project"
private const val KEY = "session"

actual suspend fun saveSession(cookieValue: String) {
    prefs().edit().putString(KEY, cookieValue).apply()
}

actual suspend fun loadSession(): String? =
    prefs().getString(KEY, null)?.trim()?.takeIf { it.isNotEmpty() }

actual suspend fun clearSession() {
    prefs().edit().remove(KEY).apply()
}

private fun prefs() = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
