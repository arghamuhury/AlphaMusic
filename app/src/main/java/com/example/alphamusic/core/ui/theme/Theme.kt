package com.example.alphamusic.core.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberDominantColor(imageUrl: String?, defaultColor: Color = White): State<Color> {
    val context = LocalContext.current
    val dominantColor = remember { mutableStateOf(defaultColor) }

    LaunchedEffect(imageUrl) {
        if (imageUrl != null) {
            withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.drawable.toBitmap()
                    Palette.from(bitmap).generate { palette ->
                        palette?.dominantSwatch?.rgb?.let { colorValue ->
                            dominantColor.value = Color(colorValue)
                        } ?: palette?.vibrantSwatch?.rgb?.let { colorValue ->
                            dominantColor.value = Color(colorValue)
                        } ?: palette?.mutedSwatch?.rgb?.let { colorValue ->
                            dominantColor.value = Color(colorValue)
                        }
                    }
                }
            }
        } else {
            dominantColor.value = defaultColor
        }
    }
    return dominantColor
}

private val MonochromeColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    primaryContainer = SurfaceDark,
    onPrimaryContainer = White,
    secondary = TextSecondary,
    onSecondary = Black,
    secondaryContainer = SurfaceDark,
    onSecondaryContainer = White,
    tertiary = White,
    onTertiary = Black,
    background = Black,
    onBackground = White,
    surface = SurfaceDark,
    onSurface = White,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    error = White,
    onError = Black,
    errorContainer = SurfaceDark,
    onErrorContainer = White
)

@Composable
fun AlphaMusicTheme(
    dominantColor: Color? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = remember(dominantColor) {
        if (dominantColor != null && dominantColor != White) {
            darkColorScheme(
                primary = dominantColor,
                onPrimary = Black,
                primaryContainer = SurfaceDark,
                onPrimaryContainer = dominantColor,
                secondary = dominantColor.copy(alpha = 0.7f),
                onSecondary = Black,
                secondaryContainer = SurfaceDark,
                onSecondaryContainer = dominantColor,
                tertiary = dominantColor,
                onTertiary = Black,
                background = Black,
                onBackground = White,
                surface = SurfaceDark,
                onSurface = White,
                surfaceVariant = SurfaceDark,
                onSurfaceVariant = TextSecondary,
                outline = DividerColor,
                error = White,
                onError = Black,
                errorContainer = SurfaceDark,
                onErrorContainer = White
            )
        } else {
            MonochromeColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Black.toArgb()
            window.navigationBarColor = Black.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
