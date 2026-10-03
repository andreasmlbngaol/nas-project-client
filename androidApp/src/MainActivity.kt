package id.andreasmlbngaol.nas_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import id.andreasmlbngaol.nas_project.data.AndroidFilePicker
import id.andreasmlbngaol.nas_project.data.initAndroid
import id.andreasmlbngaol.nas_project.ui.NasController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Platform code (session storage, file dialogs) needs a Context.
        initAndroid(this)

        // Register the pickers before the Activity starts, then hand the
        // launchers to the shared code that drives them.
        AndroidFilePicker.openLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri -> AndroidFilePicker.onOpenResult?.invoke(uri) }

        AndroidFilePicker.createLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream"),
        ) { uri -> AndroidFilePicker.onCreateResult?.invoke(uri) }

        setContent {
            App(NasController())
        }
    }
}
