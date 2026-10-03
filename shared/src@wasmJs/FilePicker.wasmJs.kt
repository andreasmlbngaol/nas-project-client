package id.andreasmlbngaol.nas_project.data

import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlin.coroutines.resume
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.toJsArray

/**
 * Web file I/O. Upload uses a hidden `<input type="file">`; download builds a
 * Blob URL and clicks an anchor. Neither can surface a real filesystem path —
 * the browser decides where the file lands — so [saveDownload] reports the
 * filename it was given rather than a path.
 */
@OptIn(ExperimentalWasmJsInterop::class)
actual suspend fun pickFile(): PickedFile? = suspendCancellableCoroutine { cont ->
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.style.display = "none"

    var settled = false
    fun finish(result: PickedFile?) {
        if (settled) return
        settled = true
        input.remove()
        cont.resume(result)
    }

    input.addEventListener("change", { _: Event ->
        val file = input.files?.get(0)
        if (file == null) {
            finish(null)
        } else {
            val reader = FileReader()
            reader.onload = {
                val buffer = reader.result
                if (buffer == null) {
                    finish(null)
                } else {
                    // FileReader.result is a JS ArrayBuffer; base64 is the one
                    // interchange format both sides understand without typed-array
                    // interop (which would need an ExperimentalWasmJsInterop cast).
                    finish(PickedFile(file.name, decodeBase64(arrayBufferToBase64(buffer))))
                }
            }
            reader.onerror = { finish(null) }
            reader.readAsArrayBuffer(file)
        }
    })

    document.body?.appendChild(input)
    input.click()
}

@OptIn(ExperimentalWasmJsInterop::class)
actual suspend fun saveDownload(name: String, bytes: ByteArray): String? {
    val base64 = encodeBase64(bytes)
    val blob = Blob(listOf(base64ToJsBytes(base64)).toJsArray(), BlobPropertyBag(type = "application/octet-stream"))
    val url = URL.createObjectURL(blob)
    val anchor = document.createElement("a") as HTMLAnchorElement
    anchor.href = url
    anchor.download = name
    anchor.style.display = "none"
    document.body?.appendChild(anchor)
    anchor.click()
    anchor.remove()
    URL.revokeObjectURL(url)
    return name
}

// ---- byte <-> JS bridges (JS intrinsics; the wasm stdlib has no public helper) ----

@OptIn(ExperimentalWasmJsInterop::class)
private fun arrayBufferToBase64(buffer: JsAny?): String =
    js("btoa(String.fromCharCode(...new Uint8Array(buffer)))")

@OptIn(ExperimentalWasmJsInterop::class)
private fun base64ToJsBytes(base64: String): JsAny =
    js("Uint8Array.from(atob(base64), c => c.charCodeAt(0))")

private fun encodeBase64(bytes: ByteArray): String = kotlin.io.encoding.Base64.encode(bytes)
private fun decodeBase64(text: String): ByteArray = kotlin.io.encoding.Base64.decode(text)
