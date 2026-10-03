package id.andreasmlbngaol.nas_project.data

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url

/**
 * Cookie storage backed by [saveSession]/[loadSession]. Ktor's built-in
 * AcceptAllCookiesStorage is in-memory, which would log the user out on every
 * restart. Only the backend's session cookie (`id`) is persisted.
 *
 * This is common code — it touches no platform API, so every target shares it.
 */
private const val SESSION_COOKIE = "id"

private class PersistentCookieStorage : CookiesStorage {
    private val cookies = mutableMapOf<String, Cookie>()

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        if (cookie.name == SESSION_COOKIE && cookie.value.isNotEmpty()) {
            cookies[cookie.name] = cookie
            saveSession(cookie.value)
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> {
        if (cookies.isEmpty()) {
            loadSession()?.let { cookies[SESSION_COOKIE] = Cookie(SESSION_COOKIE, it) }
        }
        return cookies.values.toList()
    }

    override fun close() {}
}

fun persistentCookiesStorage(): CookiesStorage = PersistentCookieStorage()
