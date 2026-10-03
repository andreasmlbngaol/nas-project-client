package id.andreasmlbngaol.nas_project.data

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/**
 * Where the NAS backend lives for this platform. The Android emulator reaches
 * the host machine's `localhost` through the special address `10.0.2.2`.
 */
expect val defaultBaseUrl: String

/**
 * Builds the shared HTTP client. The browser needs `credentials: 'include'` to
 * carry the session cookie cross-origin, and that option only exists on the JS
 * engine, so each platform supplies its own engine setup here.
 */
internal expect fun newHttpClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient
