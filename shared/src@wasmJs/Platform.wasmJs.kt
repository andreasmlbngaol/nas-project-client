package id.andreasmlbngaol.nas_project.data

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.Js
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toJsString

/**
 * The web build talks to the backend through its public tunnel, same as the
 * other targets. Browsers enforce CORS here, so the backend must allow this
 * origin with credentials.
 */
actual val defaultBaseUrl: String = "https://3000.booroong.online"

/**
 * In a browser the session cookie lives in the browser's own jar — JS can't
 * read `Set-Cookie` or set `Cookie`, so Ktor's HttpCookies plugin is useless
 * here. Instead we tell fetch to send credentials; the browser then attaches
 * and stores the cookie itself. This requires the backend to answer with
 * `Access-Control-Allow-Credentials: true` and a SameSite=None cookie.
 */
@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun newHttpClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Js) {
        configure()
        engine {
            configureRequest {
                credentials = "include".toJsString()
            }
        }
    }
