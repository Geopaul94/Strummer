package geo.strummer.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD7A86E),
    onPrimary = Color(0xFF3E2723),
    primaryContainer = Color(0xFF5D4037),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFC8B09A),
    background = Color(0xFF1C1410),
    surface = Color(0xFF1C1410),
    onBackground = Color(0xFFEDE0D4),
    onSurface = Color(0xFFEDE0D4),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6D4C41),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7CCC8),
    secondary = Color(0xFF8D6E63),
    background = Color(0xFFFFF8F0),
    surface = Color(0xFFFFF8F0),
    onBackground = Color(0xFF1C1410),
    onSurface = Color(0xFF1C1410),
)

@Composable
fun StrummerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
