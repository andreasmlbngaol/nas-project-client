package id.andreasmlbngaol.nas_project.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Content width cap so lists/tiles don't stretch absurdly on wide desktop windows. */
val ContentMaxWidth: Dp = 900.dp

/**
 * Centers its content and caps it at [maxWidth]. On a phone this is a no-op
 * (the screen is narrower than the cap); on a wide desktop/foldable it keeps
 * rows and headers aligned to one readable column instead of stretching edge
 * to edge.
 *
 * The inner box wraps its height by default, so a header Surface stays as tall
 * as its row. A scrolling body (which needs a bounded height) passes
 * [fillHeight] = true.
 */
@Composable
fun ContentFrame(
    modifier: Modifier = Modifier,
    maxWidth: Dp = ContentMaxWidth,
    fillHeight: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    // The outer box must span the full width, otherwise it wraps to the capped
    // child and TopCenter has nothing to center within.
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier),
            content = content,
        )
    }
}
