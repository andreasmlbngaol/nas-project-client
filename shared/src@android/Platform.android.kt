package id.andreasmlbngaol.nas_project.data

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/**
 * Public tunnel to the backend. Using the same URL on every platform keeps one
 * build working on desktop and Android (the emulator's `10.0.2.2` host alias
 * only reaches a backend running on the dev machine's localhost).
 */
actual val defaultBaseUrl: String = "https://3000.booroong.online"

internal actual fun newHttpClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(configure)
