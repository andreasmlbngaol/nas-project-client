@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package id.andreasmlbngaol.nas_project.ui

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * M3 Expressive loading indicator: an indeterminate indicator that morphs between
 * the built-in shape sequence.
 */
@Composable
fun NasLoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), Alignment.Center) {
        LoadingIndicator(modifier = Modifier.size(48.dp))
    }
}

/** Expressive spatial spring — for size/position changes (mirrors real motion physics). */
fun <T> spatialSpring(): FiniteAnimationSpec<T> =
    spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
