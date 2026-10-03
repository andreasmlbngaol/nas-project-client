package id.andreasmlbngaol.nas_project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import id.andreasmlbngaol.nas_project.ui.AuthScreen
import id.andreasmlbngaol.nas_project.ui.BrowserScreen
import id.andreasmlbngaol.nas_project.ui.NasController
import id.andreasmlbngaol.nas_project.ui.NasTheme
import id.andreasmlbngaol.nas_project.ui.PublicScreen
import id.andreasmlbngaol.nas_project.ui.Screen

@Composable
fun App(controller: NasController = remember { NasController() }) {
    val state by controller.state.collectAsState()

    LaunchedEffect(Unit) { controller.restore() }

    NasTheme {
        // A full-size Surface paints the theme background edge to edge, so the
        // transparent status/nav bars on Android show the app's surface color
        // instead of the window's white. safeDrawingPadding then keeps the
        // actual content clear of those bars (a no-op on desktop).
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                when (state.screen) {
                    Screen.Auth -> AuthScreen(controller, state)
                    Screen.Browser -> BrowserScreen(controller, state)
                    Screen.Public -> PublicScreen(controller, state)
                }
            }
        }
    }
}
