@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package id.andreasmlbngaol.nas_project.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.andreasmlbngaol.nas_project.data.FileItem
import id.andreasmlbngaol.nas_project.data.FolderItem

private sealed interface Target {
    data class Folder(val item: FolderItem) : Target
    data class File(val item: FileItem) : Target
}

/** All callbacks an item view needs, so the list and grid variants share one shape. */
private class ItemActions(
    val open: () -> Unit,
    val detail: () -> Unit,
    val rename: () -> Unit,
    val move: () -> Unit,
    val toggleVisibility: () -> Unit,
    val delete: () -> Unit,
    val copyLink: (() -> Unit)? = null,
)

@Composable
fun BrowserScreen(controller: NasController, state: UiState) {
    var showNewFolder by remember { mutableStateOf(false) }
    var showLinkPrompt by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Target?>(null) }
    var deleteTarget by remember { mutableStateOf<Target?>(null) }
    var linkTarget by remember { mutableStateOf<Target.File?>(null) }
    var moveSubject by remember { mutableStateOf<Target?>(null) }

    fun folderActions(folder: FolderItem) = ItemActions(
        open = { controller.openFolder(folder) },
        detail = { controller.showDetail(Entry.Folder(folder)) },
        rename = { renameTarget = Target.Folder(folder) },
        move = { moveSubject = Target.Folder(folder); controller.openMoveDialog(null, folder) },
        toggleVisibility = { controller.toggleFolderVisibility(folder) },
        delete = { deleteTarget = Target.Folder(folder) },
    )

    fun fileActions(file: FileItem) = ItemActions(
        open = { controller.download(file) },
        detail = { controller.showDetail(Entry.File(file)) },
        rename = { renameTarget = Target.File(file) },
        move = { moveSubject = Target.File(file); controller.openMoveDialog(file, null) },
        toggleVisibility = { controller.toggleFileVisibility(file) },
        delete = { deleteTarget = Target.File(file) },
        copyLink = { linkTarget = Target.File(file) },
    )

    Scaffold(
        topBar = {
            Column {
                HeroHeader(
                    title = "NAS Project",
                    leading = if (state.crumbs.size <= 1) null else {
                        {
                            FilledTonalIconButton(onClick = controller::goUp, shapes = IconButtonDefaults.shapes()) {
                                Icon(NasIcons.ArrowBack, "Kembali")
                            }
                        }
                    },
                    trailing = { AvatarChip(email = state.user?.email ?: "?", onLogout = controller::logout) },
                )
                ContentFrame {
                    Column {
                        ActionBar(
                            layout = state.layout,
                            sortKey = state.sortKey,
                            sortDescending = state.sortDescending,
                            onRefresh = controller::refresh,
                            onLayout = controller::setLayout,
                            onSort = controller::setSort,
                        )
                        Breadcrumbs(state.crumbs, controller::goTo)
                    }
                }
            }
        },
        floatingActionButton = {
            ActionFab(
                onNewFolder = { showNewFolder = true },
                onUpload = controller::upload,
                onOpenLink = { showLinkPrompt = true },
            )
        },
        // Scaffold anchors it above the FAB and keeps it clear of the content.
        snackbarHost = { NasSnackbarHost(state, controller::dismissMessages) },
    ) { inner ->
        ContentFrame(Modifier.fillMaxSize().padding(inner), fillHeight = true) {
            val entries = state.entries
            when {
                state.contents == null -> NasLoadingIndicator()
                entries.isEmpty() ->
                    EmptyState(onNewFolder = { showNewFolder = true }, onUpload = controller::upload)

                state.layout == LayoutMode.Grid -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(entries, key = { it.key }) { entry ->
                        when (entry) {
                            is Entry.Folder -> FolderTile(entry.item, folderActions(entry.item))
                            is Entry.File -> FileTile(entry.item, state.thumbnails[entry.item.id], fileActions(entry.item))
                        }
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(entries, key = { it.key }) { entry ->
                        when (entry) {
                            is Entry.Folder -> FolderRow(entry.item, folderActions(entry.item))
                            is Entry.File -> FileRow(entry.item, state.thumbnails[entry.item.id], fileActions(entry.item))
                        }
                    }
                }
            }
        }
    }

    if (showNewFolder) {
        TextPromptDialog(
            title = "Folder baru",
            confirmLabel = "Buat",
            onConfirm = { controller.createFolder(it); showNewFolder = false },
            onDismiss = { showNewFolder = false },
        )
    }
    if (showLinkPrompt) {
        TextPromptDialog(
            title = "Buka link publik",
            confirmLabel = "Buka",
            onConfirm = { controller.openPublicLink(it); showLinkPrompt = false },
            onDismiss = { showLinkPrompt = false },
        )
    }
    renameTarget?.let { target ->
        val current = when (target) {
            is Target.Folder -> target.item.name
            is Target.File -> target.item.name
        }
        TextPromptDialog(
            title = "Ganti nama",
            initial = current,
            onConfirm = { newName ->
                when (target) {
                    is Target.Folder -> controller.renameFolder(target.item, newName)
                    is Target.File -> controller.renameFile(target.item, newName)
                }
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }
    deleteTarget?.let { target ->
        val (title, text) = when (target) {
            is Target.Folder -> "Hapus folder" to
                "\"${target.item.name}\" dan SEMUA isinya akan dihapus permanen. Lanjutkan?"
            is Target.File -> "Hapus file" to "\"${target.item.name}\" akan dihapus permanen. Lanjutkan?"
        }
        ConfirmDialog(
            title = title,
            text = text,
            onConfirm = {
                when (target) {
                    is Target.Folder -> controller.deleteFolder(target.item)
                    is Target.File -> controller.deleteFile(target.item)
                }
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
    linkTarget?.let { target ->
        PublicLinkDialog(link = controller.publicLinkFor(target.item), onDismiss = { linkTarget = null })
    }
    state.detail?.let { entry ->
        DetailDialog(
            entry = entry,
            publicLink = (entry as? Entry.File)?.takeIf { it.item.isPublic }?.let { controller.publicLinkFor(it.item) },
            onDismiss = controller::dismissDetail,
        )
    }
    state.moveTargets?.let { targets ->
        MoveDialog(
            targets = targets,
            onPick = { target ->
                when (val moving = moveSubject) {
                    is Target.Folder -> controller.moveFolder(moving.item, target)
                    is Target.File -> controller.moveFile(moving.item, target)
                    null -> controller.dismissMoveDialog()
                }
                moveSubject = null
            },
            onDismiss = { moveSubject = null; controller.dismissMoveDialog() },
        )
    }
}

private val Entry.key: String
    get() = when (this) {
        is Entry.Folder -> "f:${item.id}"
        is Entry.File -> "x:${item.id}"
    }

// ---- Action bar ----

@Composable
private fun ActionBar(
    layout: LayoutMode,
    sortKey: SortKey,
    sortDescending: Boolean,
    onRefresh: () -> Unit,
    onLayout: (LayoutMode) -> Unit,
    onSort: (SortKey) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) {
        SortMenu(sortKey = sortKey, sortDescending = sortDescending, onSort = onSort)

        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = layout == LayoutMode.List,
                onClick = { onLayout(LayoutMode.List) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) {
                Icon(NasIcons.ViewList, "Daftar", Modifier.size(18.dp))
            }
            SegmentedButton(
                selected = layout == LayoutMode.Grid,
                onClick = { onLayout(LayoutMode.Grid) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) {
                Icon(NasIcons.GridView, "Kotak", Modifier.size(18.dp))
            }
        }

        FilledTonalIconButton(onClick = onRefresh, shapes = IconButtonDefaults.shapes()) {
            Icon(NasIcons.Refresh, "Segarkan")
        }
    }
}

/**
 * The primary create actions, per M3 Expressive: an icon-only toggle FAB that
 * morphs + → × and opens the secondary actions above it.
 */
@Composable
private fun ActionFab(
    onNewFolder: () -> Unit,
    onUpload: () -> Unit,
    onOpenLink: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    FloatingActionButtonMenu(
        expanded = expanded,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = { expanded = it },
            ) {
                // The scope's progress drives the + → × morph; icons cross-fade.
                val plusAlpha by animateFloatAsState(1f - checkedProgress)
                val closeAlpha by animateFloatAsState(checkedProgress)
                Box(Modifier.size(24.dp), Alignment.Center) {
                    Icon(
                        NasIcons.Add,
                        contentDescription = if (expanded) "Tutup menu" else "Menu tambah",
                        modifier = Modifier.alpha(plusAlpha),
                    )
                    Icon(
                        NasIcons.Close,
                        contentDescription = null,
                        modifier = Modifier.alpha(closeAlpha),
                    )
                }
            }
        },
    ) {
        FloatingActionButtonMenuItem(
            onClick = { expanded = false; onUpload() },
            icon = { Icon(NasIcons.Upload, null) },
            text = { Text("Unggah") },
        )
        FloatingActionButtonMenuItem(
            onClick = { expanded = false; onNewFolder() },
            icon = { Icon(NasIcons.CreateNewFolder, null) },
            text = { Text("Folder baru") },
        )
        FloatingActionButtonMenuItem(
            onClick = { expanded = false; onOpenLink() },
            icon = { Icon(NasIcons.Link, null) },
            text = { Text("Buka link") },
        )
    }
}

@Composable
private fun SortMenu(sortKey: SortKey, sortDescending: Boolean, onSort: (SortKey) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val labels = listOf(
        SortKey.Name to "Nama",
        SortKey.Size to "Ukuran",
        SortKey.Updated to "Diperbarui",
        SortKey.Created to "Dibuat",
    )
    Box {
        FilledTonalIconButton(onClick = { expanded = true }, shapes = IconButtonDefaults.shapes()) {
            Icon(NasIcons.Sort, "Urutkan")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            labels.forEach { (key, label) ->
                val active = key == sortKey
                DropdownMenuItem(
                    text = { Text(label) },
                    trailingIcon = {
                        if (active) {
                            Icon(
                                if (sortDescending) NasIcons.ArrowDownward else NasIcons.ArrowUpward,
                                contentDescription = if (sortDescending) "Menurun" else "Menaik",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                    onClick = { onSort(key) },
                )
            }
        }
    }
}

@Composable
private fun Breadcrumbs(crumbs: List<Crumb>, onGoTo: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        crumbs.forEachIndexed { index, crumb ->
            val isLast = index == crumbs.lastIndex
            if (isLast) {
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                    Text(
                        crumb.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
            } else {
                TextButton(onClick = { onGoTo(index) }) { Text(crumb.name) }
                Icon(
                    NasIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ---- List rows ----

@Composable
private fun FolderRow(folder: FolderItem, actions: ItemActions) {
    Surface(
        onClick = actions.open,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlyphTile(isFolder = true, isPublic = folder.isPublic, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                folder.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (folder.isPublic) PublicGlobe()
            RowMenu(items = folderMenu(folder, actions))
        }
    }
}

@Composable
private fun FileRow(file: FileItem, thumbnail: ImageBitmap?, actions: ItemActions) {
    Surface(
        onClick = actions.open,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilePreview(file, thumbnail, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    formatSize(file.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (file.isPublic) PublicGlobe()
            RowMenu(items = fileMenu(file, actions))
        }
    }
}

/** Thumbnail if we have one, else the item glyph tile. */
@Composable
private fun FilePreview(file: FileItem, thumbnail: ImageBitmap?, tileSize: Dp) {
    if (thumbnail != null) {
        Image(
            bitmap = thumbnail,
            contentDescription = file.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(tileSize).clip(RoundedCornerShape(12.dp)),
        )
    } else {
        GlyphTile(isFolder = false, isPublic = file.isPublic, size = tileSize)
    }
}

/** A colored tile behind the glyph — layered color, per the expressive guidance. */
@Composable
private fun GlyphTile(isFolder: Boolean, isPublic: Boolean, size: Dp = 40.dp) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isFolder) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.size(size),
    ) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { ItemGlyph(isFolder = isFolder, isPublic = isPublic) }
    }
}

/** One row in an item's overflow menu, with its leading icon. */
private data class RowAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

private fun folderMenu(folder: FolderItem, actions: ItemActions): List<RowAction> = buildList {
    add(RowAction("Detail", NasIcons.Info, actions.detail))
    add(RowAction("Ganti nama", NasIcons.Edit, actions.rename))
    add(RowAction("Pindah", NasIcons.DriveFileMove, actions.move))
    add(
        RowAction(
            if (folder.isPublic) "Jadikan privat" else "Jadikan publik",
            if (folder.isPublic) NasIcons.Lock else NasIcons.Globe,
            actions.toggleVisibility,
        ),
    )
    add(RowAction("Hapus", NasIcons.Delete, actions.delete))
}

private fun fileMenu(file: FileItem, actions: ItemActions): List<RowAction> = buildList {
    add(RowAction("Detail", NasIcons.Info, actions.detail))
    add(RowAction("Unduh", NasIcons.Download, actions.open))
    add(RowAction("Ganti nama", NasIcons.Edit, actions.rename))
    add(RowAction("Pindah", NasIcons.DriveFileMove, actions.move))
    add(
        RowAction(
            if (file.isPublic) "Jadikan privat" else "Jadikan publik",
            if (file.isPublic) NasIcons.Lock else NasIcons.Globe,
            actions.toggleVisibility,
        ),
    )
    if (file.isPublic && actions.copyLink != null) add(RowAction("Salin link", NasIcons.Link, actions.copyLink))
    add(RowAction("Hapus", NasIcons.Delete, actions.delete))
}

// ---- Grid tiles ----

@Composable
private fun FolderTile(folder: FolderItem, actions: ItemActions) {
    Surface(
        onClick = actions.open,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(10.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(1.5f), Alignment.Center) {
                GlyphTile(isFolder = true, isPublic = folder.isPublic, size = 52.dp)
                if (folder.isPublic) PublicGlobe(Modifier.align(Alignment.TopEnd))
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    folder.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                RowMenu(items = folderMenu(folder, actions))
            }
        }
    }
}

@Composable
private fun FileTile(file: FileItem, thumbnail: ImageBitmap?, actions: ItemActions) {
    Surface(
        onClick = actions.open,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(10.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(1.5f), Alignment.Center) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail,
                        contentDescription = file.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    )
                } else {
                    GlyphTile(isFolder = false, isPublic = file.isPublic, size = 52.dp)
                }
                if (file.isPublic) PublicGlobe(Modifier.align(Alignment.TopEnd))
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(file.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        formatSize(file.sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RowMenu(items = fileMenu(file, actions))
            }
        }
    }
}

@Composable
private fun EmptyState(onNewFolder: () -> Unit, onUpload: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(96.dp),
        ) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { ItemGlyph(isFolder = true, isPublic = false) }
        }
        Spacer(Modifier.height(12.dp))
        Text("Masih kosong", style = MaterialTheme.typography.headlineSmallEmphasized)
        Text(
            "Unggah file atau buat folder untuk mulai.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(onClick = onUpload, shapes = ButtonDefaults.shapes()) { Text("Unggah") }
            OutlinedButton(onClick = onNewFolder, shapes = ButtonDefaults.shapes()) { Text("Folder baru") }
        }
    }
}

@Composable
private fun RowMenu(items: List<RowAction>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Icon(NasIcons.MoreVert, "Menu") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    leadingIcon = { Icon(item.icon, null, Modifier.size(20.dp)) },
                    onClick = { expanded = false; item.onClick() },
                )
            }
        }
    }
}

// ---- Dialogs ----

@Composable
private fun DetailDialog(
    entry: Entry,
    publicLink: String?,
    onDismiss: () -> Unit,
) {
    val isFolder = entry is Entry.Folder
    val file = (entry as? Entry.File)?.item
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                DetailRow("Jenis", if (isFolder) "Folder" else (file?.mimeType ?: "File"))
                if (file != null) DetailRow("Ukuran", formatSize(file.sizeBytes))
                DetailRow("Visibilitas", if (entry.isPublic) "Publik" else "Privat")
                DetailRow("Dibuat", formatDate(entry.sortCreated))
                DetailRow("Diperbarui", formatDate(entry.sortUpdated))
                DetailRow("ID", entry.id)
                if (publicLink != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        publicLink,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            label,
            modifier = Modifier.width(96.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MoveDialog(targets: List<MoveTarget>, onPick: (MoveTarget) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pindahkan ke") },
        text = {
            if (targets.isEmpty()) {
                Text("Tidak ada folder tujuan.")
            } else {
                LazyColumn {
                    items(targets) { target ->
                        Text(
                            text = target.label,
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onPick(target) }
                                .padding(start = (16 * target.indent).dp, top = 10.dp, bottom = 10.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

private val Entry.isPublic: Boolean
    get() = when (this) {
        is Entry.Folder -> item.isPublic
        is Entry.File -> item.isPublic
    }

private val Entry.id: String
    get() = when (this) {
        is Entry.Folder -> item.id
        is Entry.File -> item.id
    }

private fun formatSize(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    if (unit == 0) return "$bytes ${units[0]}"
    // String.format is JVM-only; round to one decimal by hand so this is common.
    val rounded = kotlin.math.round(value * 10).toLong()
    return "${rounded / 10}.${rounded % 10} ${units[unit]}"
}

/** Renders an RFC-3339 timestamp as `dd MMM yyyy, HH:mm`; falls back to the raw string. */
private fun formatDate(raw: String): String {
    val date = raw.substringBefore('T')
    val time = raw.substringAfter('T', "").take(5)
    return if (time.isNotEmpty()) "$date, $time" else date
}
