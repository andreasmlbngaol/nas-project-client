package id.andreasmlbngaol.nas_project.data

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Android's document picker is callback-based and its launchers must be
 * registered with the Activity before it starts, so MainActivity registers them
 * here and the suspend [pickFile]/[saveDownload] bridge the callback back into
 * a coroutine.
 */
object AndroidFilePicker {
    var openLauncher: ActivityResultLauncher<Array<String>>? = null
    var createLauncher: ActivityResultLauncher<String>? = null

    // Only one picker is ever open at a time, so a single pending slot is enough.
    var onOpenResult: ((Uri?) -> Unit)? = null
    var onCreateResult: ((Uri?) -> Unit)? = null
}

actual suspend fun pickFile(): PickedFile? {
    val uri = awaitOpen() ?: return null
    return withContext(Dispatchers.IO) {
        val ctx = requireContext()
        val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@withContext null
        PickedFile(displayName(uri), bytes)
    }
}

actual suspend fun saveDownload(name: String, bytes: ByteArray): String? {
    val uri = awaitCreate(name) ?: return null
    return withContext(Dispatchers.IO) {
        val ctx = requireContext()
        ctx.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        uri.toString()
    }
}

private suspend fun awaitOpen(): Uri? = suspendCancellableCoroutine { cont ->
    val launcher = AndroidFilePicker.openLauncher
    if (launcher == null) {
        cont.resume(null)
        return@suspendCancellableCoroutine
    }
    AndroidFilePicker.onOpenResult = { uri -> if (cont.isActive) cont.resume(uri) }
    launcher.launch(arrayOf("*/*"))
}

private suspend fun awaitCreate(name: String): Uri? = suspendCancellableCoroutine { cont ->
    val launcher = AndroidFilePicker.createLauncher
    if (launcher == null) {
        cont.resume(null)
        return@suspendCancellableCoroutine
    }
    AndroidFilePicker.onCreateResult = { uri -> if (cont.isActive) cont.resume(uri) }
    launcher.launch(name)
}

/** The human-readable filename behind a content Uri, falling back to the last path segment. */
private fun displayName(uri: Uri): String {
    val ctx = requireContext()
    ctx.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) {
            cursor.getString(index)?.let { return it }
        }
    }
    return uri.lastPathSegment ?: "file"
}
