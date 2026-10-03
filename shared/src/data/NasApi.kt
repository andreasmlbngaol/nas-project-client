package id.andreasmlbngaol.nas_project.data

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * The whole backend surface in one class. No repository layer — there is only
 * one implementation, so an interface would be ceremony.
 */
class NasApi(private val baseUrl: String = defaultBaseUrl) {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = newHttpClient {
        install(HttpCookies) { storage = persistentCookiesStorage() }
        install(ContentNegotiation) { json(json) }
    }

    // ---- Auth ----

    suspend fun register(email: String, password: String, displayName: String?): User =
        client.post("$baseUrl/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, password, displayName?.ifBlank { null }))
        }.decoded()

    suspend fun login(email: String, password: String): User =
        client.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }.decoded()

    suspend fun me(): User = client.get("$baseUrl/auth/me").decoded()

    suspend fun logout() {
        client.post("$baseUrl/auth/logout").ensureOk()
    }

    // ---- Folders ----

    /** [id] may be the literal `"root"` for the top level. */
    suspend fun listFolder(id: String): FolderContents =
        client.get("$baseUrl/folders/$id").decoded()

    suspend fun createFolder(name: String, parentId: String?): FolderItem =
        client.post("$baseUrl/folders") {
            contentType(ContentType.Application.Json)
            setBody(CreateFolderRequest(name, parentId))
        }.decoded()

    suspend fun renameFolder(id: String, name: String): FolderItem =
        patchFolder(id, body("name" to JsonPrimitive(name)))

    /** [parentId] null moves the folder to the root. */
    suspend fun moveFolder(id: String, parentId: String?): FolderItem =
        patchFolder(id, body("parent_id" to (parentId?.let(::JsonPrimitive) ?: JsonNull)))

    suspend fun setFolderVisibility(id: String, visibility: String): FolderItem =
        patchFolder(id, body("visibility" to JsonPrimitive(visibility)))

    suspend fun deleteFolder(id: String) {
        client.delete("$baseUrl/folders/$id").ensureOk()
    }

    private suspend fun patchFolder(id: String, body: JsonObject): FolderItem =
        client.patch("$baseUrl/folders/$id") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.decoded()

    // ---- Files ----

    suspend fun upload(file: PickedFile, folderId: String?, name: String?): FileItem =
        client.submitFormWithBinaryData(
            url = "$baseUrl/files/upload",
            formData = formData {
                // Text fields must precede the file part.
                folderId?.let { append("folder_id", it) }
                name?.let { append("name", it) }
                append(
                    key = "file",
                    value = file.bytes,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, "application/octet-stream")
                        append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                    },
                )
            },
        ).decoded()

    suspend fun renameFile(id: String, name: String): FileItem =
        patchFile(id, body("name" to JsonPrimitive(name)))

    /** The backend treats a null folder_id as "no change", so [folderId] is required. */
    suspend fun moveFile(id: String, folderId: String): FileItem =
        patchFile(id, body("folder_id" to JsonPrimitive(folderId)))

    suspend fun setFileVisibility(id: String, visibility: String): FileItem =
        patchFile(id, body("visibility" to JsonPrimitive(visibility)))

    suspend fun deleteFile(id: String) {
        client.delete("$baseUrl/files/$id").ensureOk()
    }

    suspend fun downloadFile(id: String): DownloadedFile =
        client.get("$baseUrl/files/$id/download").asDownload()

    private suspend fun patchFile(id: String, body: JsonObject): FileItem =
        client.patch("$baseUrl/files/$id") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.decoded()

    // ---- Public links (no login) ----

    /** [kind] is `"file"` or `"folder"`. */
    suspend fun publicInfo(kind: String, publicId: String): PublicInfo =
        client.get("$baseUrl/s/${kind}s/$publicId").decoded()

    suspend fun downloadPublic(kind: String, publicId: String, fileId: String): DownloadedFile =
        client.get("$baseUrl/s/${kind}s/$publicId/files/$fileId/download").asDownload()

    /** Download a single public file by its own public id. */
    suspend fun downloadPublicFile(publicId: String): DownloadedFile =
        client.get("$baseUrl/s/files/$publicId/download").asDownload()

    // ---- Plumbing ----

    private fun body(vararg pairs: Pair<String, JsonElement?>): JsonObject =
        buildJsonObject { pairs.forEach { (k, v) -> if (v != null) put(k, v) } }

    private suspend inline fun <reified T> HttpResponse.decoded(): T {
        ensureOk()
        return body()
    }

    private suspend fun HttpResponse.ensureOk() {
        if (status.isSuccess()) return
        val text = runCatching { bodyAsText() }.getOrDefault("")
        val message = runCatching { json.parseToJsonElement(text).jsonObject["error"]?.jsonPrimitive?.content }
            .getOrNull()
            ?: "HTTP ${status.value}"
        throw ApiException(status.value, message).also {
            it.retryAfterSeconds = headers[HttpHeaders.RetryAfter]?.toIntOrNull()
        }
    }

    private suspend fun HttpResponse.asDownload(): DownloadedFile {
        ensureOk()
        val disposition = headers[HttpHeaders.ContentDisposition]
        val name = disposition
            ?.substringAfter("filename=", "")
            ?.trim()
            ?.trim('"')
            ?.takeIf { it.isNotEmpty() }
            ?: "download"
        return DownloadedFile(name, bodyAsBytes())
    }
}
