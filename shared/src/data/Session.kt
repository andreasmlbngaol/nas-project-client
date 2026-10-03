package id.andreasmlbngaol.nas_project.data

/**
 * Persists the session cookie across app restarts. The backend's session is an
 * opaque cookie named `id`, so that single string is all we store.
 */
expect suspend fun saveSession(cookieValue: String)
expect suspend fun loadSession(): String?
expect suspend fun clearSession()
