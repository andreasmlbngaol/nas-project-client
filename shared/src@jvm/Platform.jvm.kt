package id.andreasmlbngaol.nas_project.data

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

actual val defaultBaseUrl: String = "http://localhost:3000"

internal actual fun newHttpClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(configure)
