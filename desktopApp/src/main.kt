package id.andreasmlbngaol.nas_project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import id.andreasmlbngaol.nas_project.data.registerAppFrame
import id.andreasmlbngaol.nas_project.ui.NasController
import java.awt.Frame

fun main(args: Array<String>) = application {
    // A URL passed on the command line opens straight into the public viewer —
    // also the hook a Google-OAuth redirect / deep link would use.
    val controller = NasController(openLink = args.firstOrNull { it.startsWith("http") })
    Window(onCloseRequest = ::exitApplication, title = "nas-project") {
        // Parent the AWT file dialogs to this window so they stay on top of it.
        (window as? Frame)?.let(::registerAppFrame)
        App(controller)
    }
}
