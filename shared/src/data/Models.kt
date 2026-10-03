package id.andreasmlbngaol.nas_project.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class FileItem(
    val id: String,
    @SerialName("folder_id") val folderId: String? = null,
    val name: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("mime_type") val mimeType: String? = null,
    val visibility: String,
    @SerialName("public_id") val publicId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
) {
    val isPublic: Boolean get() = visibility == "public"
}

@Serializable
data class FolderItem(
    val id: String,
    @SerialName("parent_id") val parentId: String? = null,
    val name: String,
    val visibility: String,
    @SerialName("public_id") val publicId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
) {
    val isPublic: Boolean get() = visibility == "public"
}

@Serializable
data class FolderContents(
    val folder: FolderItem? = null,
    val folders: List<FolderItem> = emptyList(),
    val files: List<FileItem> = emptyList(),
)

@Serializable
data class PublicInfo(
    val kind: String,
    val name: String,
    val files: List<FileItem>? = null,
    val folders: List<FolderItem>? = null,
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    @SerialName("display_name") val displayName: String? = null,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class CreateFolderRequest(val name: String, @SerialName("parent_id") val parentId: String? = null)

/** Bytes handed back by a download, with the server-supplied filename. */
data class DownloadedFile(val name: String, val bytes: ByteArray)
