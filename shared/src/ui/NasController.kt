package id.andreasmlbngaol.nas_project.ui

import id.andreasmlbngaol.nas_project.data.ApiException
import id.andreasmlbngaol.nas_project.data.FileItem
import id.andreasmlbngaol.nas_project.data.FolderContents
import id.andreasmlbngaol.nas_project.data.FolderItem
import id.andreasmlbngaol.nas_project.data.NasApi
import id.andreasmlbngaol.nas_project.data.PublicInfo
import id.andreasmlbngaol.nas_project.data.User
import id.andreasmlbngaol.nas_project.data.clearSession
import id.andreasmlbngaol.nas_project.data.defaultBaseUrl
import id.andreasmlbngaol.nas_project.data.pickFile
import id.andreasmlbngaol.nas_project.data.saveDownload
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Screen { Auth, Browser, Public }

/** How the file list is presented. */
enum class LayoutMode { List, Grid }

/** Sortable fields. Folders always sort ahead of files regardless of key. */
enum class SortKey { Name, Size, Updated, Created }

/** One entry in the breadcrumb trail. [id] is null for the root. */
data class Crumb(val id: String?, val name: String)

/** A folder offered as a move destination. [indent] drives indentation in the list. */
data class MoveTarget(val id: String, val label: String, val indent: Int)

/** A folder or file, unified so one list can hold both and be sorted together. */
sealed interface Entry {
    val name: String
    val sortSize: Long
    val sortUpdated: String
    val sortCreated: String

    data class Folder(val item: FolderItem) : Entry {
        override val name get() = item.name
        override val sortSize get() = 0L
        override val sortUpdated get() = item.updatedAt
        override val sortCreated get() = item.createdAt
    }

    data class File(val item: FileItem) : Entry {
        override val name get() = item.name
        override val sortSize get() = item.sizeBytes
        override val sortUpdated get() = item.updatedAt
        override val sortCreated get() = item.createdAt
    }
}

data class UiState(
    val screen: Screen = Screen.Auth,
    val user: User? = null,
    val crumbs: List<Crumb> = listOf(Crumb(null, "Beranda")),
    val contents: FolderContents? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val publicKind: String? = null,
    val publicId: String? = null,
    val publicInfo: PublicInfo? = null,
    val moveTargets: List<MoveTarget>? = null,
    val layout: LayoutMode = LayoutMode.List,
    val sortKey: SortKey = SortKey.Name,
    val sortDescending: Boolean = false,
    /** The item whose detail sheet is open, if any. */
    val detail: Entry? = null,
    /** Decoded image thumbnails, keyed by file id. Populated lazily in grid mode. */
    val thumbnails: Map<String, ImageBitmap> = emptyMap(),
) {
    /** Folders first, then files, each sorted by [sortKey] and [sortDescending]. */
    val entries: List<Entry>
        get() {
            val c = contents ?: return emptyList()
            val folders = c.folders.map { Entry.Folder(it) }
            val files = c.files.map { Entry.File(it) }
            return folders.sortedWith(comparator()) + files.sortedWith(comparator())
        }

    private fun comparator(): Comparator<Entry> {
        val base: Comparator<Entry> = when (sortKey) {
            SortKey.Name -> compareBy { it.name.lowercase() }
            SortKey.Size -> compareBy { it.sortSize }
            SortKey.Updated -> compareBy { it.sortUpdated }
            SortKey.Created -> compareBy { it.sortCreated }
        }
        return if (sortDescending) base.reversed() else base
    }
}

class NasController(
    private val api: NasApi = NasApi(),
    val baseUrl: String = defaultBaseUrl,
    /** A public link to open on startup (deep link / OAuth redirect). */
    private val openLink: String? = null,
) {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main + CoroutineExceptionHandler { _, e ->
            _state.value = _state.value.copy(loading = false, error = e.message ?: "Terjadi kesalahan")
        },
    )
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val currentFolderId: String? get() = _state.value.crumbs.last().id

    /** Called once at startup: opens a deep link if given, else restores a persisted session. */
    fun restore() {
        if (openLink != null) {
            openPublicLink(openLink)
            return
        }
        scope.launch {
            try {
                val user = api.me()
                _state.value = _state.value.copy(screen = Screen.Browser, user = user)
                refresh()
            } catch (_: Throwable) {
                // No valid session — stay on the auth screen. A network failure
                // here is not something the user did, so it stays silent rather
                // than flashing a snackbar on startup.
            }
        }
    }

    // ---- Auth ----

    fun login(email: String, password: String) = auth { api.login(email, password) }

    fun register(email: String, password: String, displayName: String?) =
        auth { api.register(email, password, displayName) }

    private fun auth(block: suspend () -> User) = scope.launch {
        run {
            val user = block()
            _state.value = _state.value.copy(
                screen = Screen.Browser, user = user, crumbs = listOf(Crumb(null, "Beranda")),
            )
            refresh()
        }
    }

    fun logout() = scope.launch {
        runCatching { api.logout() }
        clearSession()
        _state.value = UiState()
    }

    // ---- Browsing ----

    fun refresh() = scope.launch {
        run {
            _state.value = _state.value.copy(contents = api.listFolder(currentFolderId ?: "root"))
            afterContentsLoaded()
        }
    }

    fun openFolder(folder: FolderItem) = scope.launch {
        run {
            val contents = api.listFolder(folder.id)
            _state.value = _state.value.copy(
                crumbs = _state.value.crumbs + Crumb(folder.id, folder.name),
                contents = contents,
            )
            afterContentsLoaded()
        }
    }

    /** Jumps to a breadcrumb index; 0 is the root. */
    fun goTo(index: Int) = scope.launch {
        val crumbs = _state.value.crumbs.take(index + 1)
        run {
            val contents = api.listFolder(crumbs.last().id ?: "root")
            _state.value = _state.value.copy(crumbs = crumbs, contents = contents)
            afterContentsLoaded()
        }
    }

    /** Steps back one folder, if not already at the root. */
    fun goUp() {
        val crumbs = _state.value.crumbs
        if (crumbs.size > 1) goTo(crumbs.size - 2)
    }

    private fun afterContentsLoaded() {
        if (_state.value.layout == LayoutMode.Grid) loadThumbnails()
    }

    // ---- Mutations ----

    fun createFolder(name: String) = scope.launch {
        run {
            api.createFolder(name, currentFolderId)
            _state.value = _state.value.copy(notice = "Folder \"$name\" dibuat")
            refresh()
        }
    }

    fun upload() = scope.launch {
        val picked = pickFile() ?: return@launch
        run {
            val item = api.upload(picked, currentFolderId, null)
            _state.value = _state.value.copy(notice = "\"${item.name}\" diunggah")
            refresh()
        }
    }

    fun renameFolder(folder: FolderItem, newName: String) = scope.launch {
        run { api.renameFolder(folder.id, newName); refresh() }
    }

    fun renameFile(file: FileItem, newName: String) = scope.launch {
        run { api.renameFile(file.id, newName); refresh() }
    }

    fun deleteFolder(folder: FolderItem) = scope.launch {
        run { api.deleteFolder(folder.id); _state.value = _state.value.copy(notice = "Folder dihapus"); refresh() }
    }

    fun deleteFile(file: FileItem) = scope.launch {
        run { api.deleteFile(file.id); _state.value = _state.value.copy(notice = "File dihapus"); refresh() }
    }

    fun toggleFolderVisibility(folder: FolderItem) = scope.launch {
        val next = if (folder.isPublic) "private" else "public"
        run { api.setFolderVisibility(folder.id, next); refresh() }
    }

    fun toggleFileVisibility(file: FileItem) = scope.launch {
        val next = if (file.isPublic) "private" else "public"
        run { api.setFileVisibility(file.id, next); refresh() }
    }

    fun download(file: FileItem) = scope.launch {
        run {
            val download = api.downloadFile(file.id)
            val path = saveDownload(download.name, download.bytes)
            if (path != null) _state.value = _state.value.copy(notice = "Disimpan ke $path")
        }
    }

    // ---- Move ----

    /** Walks the whole tree so the move dialog can list every destination. */
    fun openMoveDialog(item: FileItem?, folder: FolderItem?) = scope.launch {
        run {
            val targets = mutableListOf<MoveTarget>()
            if (folder != null) targets += MoveTarget("root", "Beranda", 0)
            collectFolders("root", 0, targets, excludeId = folder?.id)
            _state.value = _state.value.copy(moveTargets = targets)
        }
    }

    fun dismissMoveDialog() {
        _state.value = _state.value.copy(moveTargets = null)
    }

    fun moveFile(file: FileItem, target: MoveTarget) = scope.launch {
        run { api.moveFile(file.id, target.id); dismissMoveDialog(); refresh() }
    }

    fun moveFolder(folder: FolderItem, target: MoveTarget) = scope.launch {
        val parentId = target.id.takeIf { it != "root" }
        run { api.moveFolder(folder.id, parentId); dismissMoveDialog(); refresh() }
    }

    private suspend fun collectFolders(
        parentId: String,
        depth: Int,
        out: MutableList<MoveTarget>,
        excludeId: String?,
    ) {
        for (f in api.listFolder(parentId).folders) {
            if (f.id == excludeId) continue // can't move a folder into itself
            out += MoveTarget(f.id, f.name, depth)
            collectFolders(f.id, depth + 1, out, excludeId)
        }
    }

    // ---- Public links ----

    /** Accepts a full link (`.../s/files/<id>`) or a bare `files/<id>` / `<kind>/<id>` pair. */
    fun openPublicLink(input: String) = scope.launch {
        val trimmed = input.trim().trimEnd('/')
        val kind = when {
            "/s/files/" in trimmed || trimmed.startsWith("files/") -> "file"
            "/s/folders/" in trimmed || trimmed.startsWith("folders/") -> "folder"
            else -> null
        }
        val id = trimmed.substringAfterLast('/')
        if (kind == null || id.isBlank()) {
            _state.value = _state.value.copy(error = "Link tidak valid. Contoh: /s/files/<id>")
            return@launch
        }
        run {
            val info = api.publicInfo(kind, id)
            _state.value = _state.value.copy(
                screen = Screen.Public, publicKind = kind, publicId = id, publicInfo = info,
            )
        }
    }

    /** The shareable URL for a public file (only valid while [FileItem.publicId] is set). */
    fun publicLinkFor(file: FileItem): String = "$baseUrl/s/files/${file.publicId}"

    fun closePublic() {
        _state.value = _state.value.copy(screen = if (_state.value.user != null) Screen.Browser else Screen.Auth)
    }

    fun downloadPublicFile(fileId: String) = scope.launch {
        val s = _state.value
        val kind = s.publicKind ?: return@launch
        val publicId = s.publicId ?: return@launch
        run {
            val download = if (kind == "file") api.downloadPublicFile(publicId)
            else api.downloadPublic(kind, publicId, fileId)
            val path = saveDownload(download.name, download.bytes)
            if (path != null) _state.value = _state.value.copy(notice = "Disimpan ke $path")
        }
    }

    fun dismissMessages() {
        _state.value = _state.value.copy(error = null, notice = null)
    }

    // ---- Layout & thumbnails ----

    fun setLayout(mode: LayoutMode) {
        _state.value = _state.value.copy(layout = mode)
        if (mode == LayoutMode.Grid) loadThumbnails()
    }

    /** Sets the sort key, flipping direction if the same key is chosen again. */
    fun setSort(key: SortKey) {
        val current = _state.value
        _state.value = if (current.sortKey == key) {
            current.copy(sortDescending = !current.sortDescending)
        } else {
            current.copy(sortKey = key, sortDescending = false)
        }
    }

    fun showDetail(entry: Entry) {
        _state.value = _state.value.copy(detail = entry)
    }

    fun dismissDetail() {
        _state.value = _state.value.copy(detail = null)
    }

    /**
     * Fetches thumbnails for image files in the current folder. The backend has
     * no thumbnail endpoint, so the full file is downloaded and decoded — only
     * images are attempted (a video would mean downloading the whole video).
     */
    private fun loadThumbnails() = scope.launch {
        val files = _state.value.contents?.files.orEmpty()
            .filter { it.mimeType?.startsWith("image/") == true }
        for (file in files) {
            if (_state.value.thumbnails.containsKey(file.id)) continue
            val bitmap = runCatching { api.downloadFile(file.id).bytes.decodeToImageBitmap() }.getOrNull()
                ?: continue
            _state.value = _state.value.copy(thumbnails = _state.value.thumbnails + (file.id to bitmap))
        }
    }

    /** Runs [block], translating failures and 401s into UI state. */
    private suspend inline fun run(block: () -> Unit) {
        _state.value = _state.value.copy(loading = true, error = null)
        try {
            block()
            _state.value = _state.value.copy(loading = false)
        } catch (e: ApiException) {
            if (e.isUnauthorized) {
                clearSession()
                _state.value = UiState(error = "Sesi berakhir, silakan masuk lagi.")
            } else {
                val message = when (e.status) {
                    429 -> "Terlalu banyak percobaan. Coba lagi dalam ${e.retryAfterSeconds ?: 30} detik."
                    else -> e.message ?: "Terjadi kesalahan"
                }
                _state.value = _state.value.copy(loading = false, error = message)
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(loading = false, error = e.message ?: "Kesalahan jaringan")
        }
    }
}
