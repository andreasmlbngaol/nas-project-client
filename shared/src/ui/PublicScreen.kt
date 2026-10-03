@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package id.andreasmlbngaol.nas_project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun PublicScreen(controller: NasController, state: UiState) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            HeroHeader(
                title = "Tampilan publik",
                subtitle = state.publicInfo?.name ?: "Memuat…",
                trailing = {
                    FilledTonalIconButton(onClick = controller::closePublic, shapes = IconButtonDefaults.shapes()) {
                        Icon(NasIcons.Close, "Tutup")
                    }
                },
            )
            ContentFrame(Modifier.fillMaxSize(), fillHeight = true) {
            Column(Modifier.fillMaxSize()) {
                val info = state.publicInfo
                if (info == null) {
                    NasLoadingIndicator()
                    return@Column
                }

                if (info.kind == "file") {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(96.dp),
                        ) {
                            Box(Modifier.fillMaxSize(), Alignment.Center) { ItemGlyph(isFolder = false, isPublic = true) }
                        }
                        Spacer(Modifier.padding(8.dp))
                        Text(info.name, style = MaterialTheme.typography.headlineSmallEmphasized, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.padding(4.dp))
                        Button(onClick = { state.publicId?.let(controller::downloadPublicFile) }) { Text("Unduh") }
                    }
                    return@Column
                }

                val folders = info.folders.orEmpty()
                val files = info.files.orEmpty()
                if (folders.isEmpty() && files.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Folder ini kosong") }
                    return@Column
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(folders, key = { it.id }) { folder ->
                        // ponytail: a public folder link shows its top level only; nested
                        // browsing of public folders is skipped.
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(44.dp),
                                ) {
                                    Box(Modifier.fillMaxSize(), Alignment.Center) { ItemGlyph(isFolder = true, isPublic = true) }
                                }
                                Spacer(Modifier.width(14.dp))
                                Text(folder.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    items(files, key = { it.id }) { file ->
                        Surface(
                            onClick = { controller.downloadPublicFile(file.id) },
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.size(44.dp),
                                ) {
                                    Box(Modifier.fillMaxSize(), Alignment.Center) { ItemGlyph(isFolder = false, isPublic = true) }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${file.sizeBytes} bytes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text("Unduh", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
        }
        NasSnackbarHost(
            state = state,
            onDismiss = controller::dismissMessages,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }
}
