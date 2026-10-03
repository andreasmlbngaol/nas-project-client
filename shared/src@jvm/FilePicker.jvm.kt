package id.andreasmlbngaol.nas_project.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * The app's main window, so AWT dialogs open as its children (modal, on top)
 * instead of floating free and getting buried behind it.
 */
private var appFrame: Frame? = null

/** Called from the desktop entry point once the window exists. */
fun registerAppFrame(frame: Frame) {
    appFrame = frame
}

actual suspend fun pickFile(): PickedFile? = withContext(Dispatchers.IO) {
    val dialog = FileDialog(appFrame, "Pilih file untuk diunggah", FileDialog.LOAD)
    dialog.isVisible = true
    val dir = dialog.directory ?: return@withContext null
    val name = dialog.file ?: return@withContext null
    val file = File(dir, name)
    if (!file.isFile) return@withContext null
    PickedFile(file.name, file.readBytes())
}

actual suspend fun saveDownload(name: String, bytes: ByteArray): String? = withContext(Dispatchers.IO) {
    val dialog = FileDialog(appFrame, "Simpan file", FileDialog.SAVE)
    dialog.file = name
    dialog.isVisible = true
    val dir = dialog.directory ?: return@withContext null
    val chosen = dialog.file ?: return@withContext null
    val target = File(dir, chosen)
    target.writeBytes(bytes)
    target.absolutePath
}
