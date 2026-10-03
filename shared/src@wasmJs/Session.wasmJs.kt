package id.andreasmlbngaol.nas_project.data

import kotlinx.browser.window

// ponytail: plaintext in localStorage, same trade-off as the desktop file. The
// cookie is a bearer token; there's no browser equivalent of a keychain here.
// localStorage can throw (private mode, blocked storage), so every access is
// guarded and the app just behaves as logged-out when it's unavailable.
private const val KEY = "nas.session"

actual suspend fun saveSession(cookieValue: String) {
    runCatching { window.localStorage.setItem(KEY, cookieValue) }
}

actual suspend fun loadSession(): String? =
    runCatching { window.localStorage.getItem(KEY) }.getOrNull()?.takeIf { it.isNotEmpty() }

actual suspend fun clearSession() {
    runCatching { window.localStorage.removeItem(KEY) }
}
