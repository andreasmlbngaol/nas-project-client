package id.andreasmlbngaol.nas_project.data

/** Any non-2xx response, carrying the backend's `{"error": "..."}` message. */
class ApiException(val status: Int, message: String) : Exception(message) {

    /** For 429: seconds to wait before retrying, from the `Retry-After` header. */
    var retryAfterSeconds: Int? = null

    val isUnauthorized: Boolean get() = status == 401
}
