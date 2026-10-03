package id.andreasmlbngaol.nas_project.data

/** A file chosen by the user, read fully into memory for upload. */
class PickedFile(val name: String, val bytes: ByteArray)

/** Opens the platform's native file chooser; null if the user cancels. */
expect suspend fun pickFile(): PickedFile?

/** Writes downloaded bytes to a user-chosen location; returns the saved path, or null if cancelled. */
expect suspend fun saveDownload(name: String, bytes: ByteArray): String?
