package id.andreasmlbngaol.nas_project.data

import java.io.File

// ponytail: plaintext file, no encryption. The cookie is a bearer token; swap
// for an OS keychain (e.g. java-keyring) if the threat model demands it.
// Overridable via -Dnas.sessionFile so tests never touch the real session.
private val sessionFile: File
    get() = System.getProperty("nas.sessionFile")?.let(::File)
        ?: File(System.getProperty("user.home"), ".nas-project/session")

actual suspend fun saveSession(cookieValue: String) {
    sessionFile.parentFile?.mkdirs()
    sessionFile.writeText(cookieValue)
}

actual suspend fun loadSession(): String? =
    sessionFile.takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.isNotEmpty() }

actual suspend fun clearSession() {
    sessionFile.delete()
}
