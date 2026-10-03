@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package id.andreasmlbngaol.nas_project.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Carries the error/success tone through to the host, which picks the colors. */
private class NasSnackbarVisuals(
    override val message: String,
    val isError: Boolean,
) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = true
    override val duration: SnackbarDuration = SnackbarDuration.Short
}

/**
 * Bottom snackbar host for [UiState.error] / [UiState.notice]. An overlay, so it
 * never moves the screen's layout — unlike the old inline strip. Shows one
 * message at a time and clears it in the controller once the snackbar goes away,
 * so it doesn't reappear on recomposition.
 */
@Composable
fun NasSnackbarHost(state: UiState, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val message = state.error ?: state.notice
    val isError = state.error != null
    val hostState = remember { SnackbarHostState() }
    // Keyed on both, so a new message restarts rather than inheriting the old timer.
    LaunchedEffect(message, isError) {
        if (message == null) return@LaunchedEffect
        hostState.showSnackbar(NasSnackbarVisuals(message, isError))
        onDismiss()
    }
    SnackbarHost(hostState, modifier) { data ->
        val error = (data.visuals as? NasSnackbarVisuals)?.isError == true
        Snackbar(
            snackbarData = data,
            containerColor = if (error) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = if (error) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

/**
 * Flat header bar. Deliberately restrained — a solid tonal surface, no gradient
 * or decoration.
 */
@Composable
fun HeroHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        ContentFrame {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineSmallEmphasized)
                if (!subtitle.isNullOrEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
            trailing?.invoke()
            }
        }
    }
}

/** A round avatar chip showing the user's initial; opens a menu on press. */
@Composable
fun AvatarChip(email: String, onLogout: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp),
            onClick = { expanded = true },
        ) {
            Box(Modifier.fillMaxWidth(), Alignment.Center) {
                Text(
                    email.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(email, style = MaterialTheme.typography.bodySmall) },
                enabled = false,
                onClick = {},
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Keluar") },
                leadingIcon = { Icon(NasIcons.Logout, null) },
                onClick = { expanded = false; onLogout() },
            )
        }
    }
}

/**
 * Folder/file glyph. Uses an expressive shape-morph: corners spring between an
 * idle radius and a rounder "public" state, and a dot fades in — per the
 * M3 Expressive shape-morph guidance.
 */
@Composable
fun ItemGlyph(isFolder: Boolean, isPublic: Boolean, modifier: Modifier = Modifier, tint: Color? = null) {
    val color = tint
        ?: if (isFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val morph by animateFloatAsState(if (isPublic) 1f else 0f, spatialSpring(), label = "glyphMorph")
    val badgeColor by animateColorAsState(
        if (isPublic) MaterialTheme.colorScheme.tertiary else Color.Transparent, label = "badge",
    )

    Canvas(modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        if (isFolder) {
            val r = CornerRadius(w * (0.12f + 0.06f * morph))
            drawRoundRect(color, Offset(0f, h * 0.10f), Size(w * 0.45f, h * 0.22f), r)
            drawRoundRect(color, Offset(0f, h * 0.26f), Size(w, h * 0.62f), r)
        } else {
            val inset = w * (0.16f - 0.04f * morph)
            drawRoundRect(
                color,
                Offset(inset, 0f),
                Size(w - inset * 2, h),
                CornerRadius(w * (0.12f + 0.10f * morph)),
            )
            drawRect(Color.White.copy(alpha = 0.8f), Offset(w * 0.32f, h * 0.30f), Size(w * 0.36f, h * 0.06f))
            drawRect(Color.White.copy(alpha = 0.8f), Offset(w * 0.32f, h * 0.48f), Size(w * 0.36f, h * 0.06f))
        }
        if (isPublic) drawCircle(badgeColor, radius = w * 0.19f, center = Offset(w * 0.84f, h * 0.84f))
    }
}

/** The public marker: a small globe icon in a tonal circle. */
@Composable
fun PublicGlobe(modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier.size(24.dp),
    ) {
        Box(Modifier.fillMaxWidth(), Alignment.Center) {
            Icon(
                NasIcons.Globe,
                contentDescription = "Publik",
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
