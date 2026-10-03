@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package id.andreasmlbngaol.nas_project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AuthScreen(controller: NasController, state: UiState) {
    var isRegister by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showLinkPrompt by remember { mutableStateOf(false) }

    val scheme = MaterialTheme.colorScheme
    val passwordsMatch = !isRegister || password == confirmPassword
    val canSubmit = email.isNotBlank() && password.isNotBlank() && passwordsMatch

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(scheme.primaryContainer, scheme.surface))),
    ) {
        // One scrollable column holds everything, so the link button can never
        // overlap the card on a short viewport.
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 400.dp),
                shape = RoundedCornerShape(32.dp),
                color = scheme.surfaceContainerLow,
                shadowElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = scheme.primary,
                        modifier = Modifier.size(72.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            ItemGlyph(isFolder = true, isPublic = false, tint = scheme.onPrimary)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("NAS Project", style = MaterialTheme.typography.headlineMediumEmphasized)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isRegister) "Buat akun baru" else "Masuk ke penyimpananmu",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    PasswordField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Kata sandi",
                        visible = showPassword,
                        onToggleVisible = { showPassword = !showPassword },
                    )
                    if (isRegister) {
                        Spacer(Modifier.height(12.dp))
                        PasswordField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = "Konfirmasi kata sandi",
                            visible = showPassword,
                            onToggleVisible = { showPassword = !showPassword },
                            isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                            supporting = if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                                "Kata sandi tidak sama"
                            } else {
                                null
                            },
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Nama tampilan (opsional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(24.dp))

                    Box(Modifier.fillMaxWidth().height(48.dp), Alignment.Center) {
                        if (state.loading) {
                            LoadingIndicator(Modifier.size(32.dp))
                        } else {
                            Button(
                                onClick = {
                                    if (isRegister) controller.register(email, password, displayName)
                                    else controller.login(email, password)
                                },
                                enabled = canSubmit,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (isRegister) "Daftar" else "Masuk")
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        isRegister = !isRegister
                        // Each mode starts clean; carrying a login email into the
                        // register form (or vice versa) is confusing.
                        email = ""
                        password = ""
                        confirmPassword = ""
                        displayName = ""
                        showPassword = false
                    }) {
                        Text(if (isRegister) "Sudah punya akun? Masuk" else "Belum punya akun? Daftar")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { showLinkPrompt = true }) { Text("Buka link publik") }
        }
        NasSnackbarHost(
            state = state,
            onDismiss = controller::dismissMessages,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
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
}

/** Password field with a show/hide eye toggle in its trailing slot. */
@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    isError: Boolean = false,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = supporting?.let { { Text(it) } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisible) {
                Icon(
                    if (visible) NasIcons.EyeOff else NasIcons.Eye,
                    contentDescription = if (visible) "Sembunyikan kata sandi" else "Tampilkan kata sandi",
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
