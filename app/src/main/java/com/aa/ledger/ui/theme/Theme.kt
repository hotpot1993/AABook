package com.aa.ledger.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = MontraPrimary,
    onPrimary = Color.White,
    primaryContainer = MontraPrimary.copy(alpha = 0.12f),
    onPrimaryContainer = MontraPrimary,
    secondary = InfoBlue,
    onSecondary = Color.White,
    secondaryContainer = InfoBlue.copy(alpha = 0.12f),
    onSecondaryContainer = InfoBlue,
    tertiary = WarningOrange,
    onTertiary = Color.White,
    tertiaryContainer = WarningOrange.copy(alpha = 0.12f),
    onTertiaryContainer = WarningOrange,
    error = MontraRed,
    onError = Color.White,
    errorContainer = MontraRed.copy(alpha = 0.12f),
    onErrorContainer = MontraRed,
    background = MontraBackground,
    onBackground = MontraTextPrimary,
    surface = MontraSurface,
    onSurface = MontraTextPrimary,
    surfaceVariant = MontraFill,
    onSurfaceVariant = MontraTextSecondary,
    outline = MontraDivider,
    outlineVariant = MontraDivider,
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color(0xFFF5F4F1),
    inversePrimary = MontraPrimary.copy(alpha = 0.7f),
    scrim = Color(0x33000000)
)

private val DarkColorScheme = darkColorScheme(
    primary = MontraPrimaryLight,
    onPrimary = Color(0xFF1A2E20),
    primaryContainer = MontraPrimary.copy(alpha = 0.3f),
    onPrimaryContainer = MontraPrimaryLight,
    secondary = InfoBlue,
    onSecondary = Color(0xFF1A2A3D),
    secondaryContainer = InfoBlue.copy(alpha = 0.3f),
    onSecondaryContainer = Color(0xFF8FBFE8),
    tertiary = WarningOrange,
    onTertiary = Color(0xFF3D2A1C),
    tertiaryContainer = WarningOrange.copy(alpha = 0.3f),
    onTertiaryContainer = Color(0xFFE8B898),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3D1A1A),
    errorContainer = Color(0xFF3D1A1A),
    onErrorContainer = Color(0xFFFF6B6B),
    background = MontraDarkBg,
    onBackground = MontraDarkTextPrimary,
    surface = MontraDarkSurface,
    onSurface = MontraDarkTextPrimary,
    surfaceVariant = MontraDarkFill,
    onSurfaceVariant = MontraDarkTextSec,
    outline = MontraDarkDivider,
    outlineVariant = MontraDarkDivider,
    inverseSurface = Color(0xFFF0EFEC),
    inverseOnSurface = Color(0xFF1A1A18),
    inversePrimary = MontraPrimary.copy(alpha = 0.9f),
    scrim = Color(0x66000000)
)

// iOS SF Pro 风格 Typography — 等宽数字用于金额
val IosTypography = Typography(
    // Large Title
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 41.sp, letterSpacing = 0.37.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    // Title
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp),
    // Body
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    // Labels
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 13.sp)
)

// 等宽数字金额样式
val AmountStyle = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 22.sp)
val AmountLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 48.sp)
val AmountMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp)

// Montra 头部大金额
val MontraAmountHero = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 52.sp, letterSpacing = 0.sp)

@Composable
fun AALedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bgColor = if (darkTheme) MontraDarkBg else MontraBackground
            window.navigationBarColor = android.graphics.Color.rgb(
                (bgColor.red * 255).toInt(),
                (bgColor.green * 255).toInt(),
                (bgColor.blue * 255).toInt()
            )
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = IosTypography, content = content)
}

/** iOS 毛玻璃效果 — API 31+ 真实模糊，低版本降级为半透明 */
fun Modifier.frostedGlass(): Modifier = this.then(
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(20.dp) else Modifier
)
