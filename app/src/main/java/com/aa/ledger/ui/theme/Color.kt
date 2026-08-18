package com.aa.ledger.ui.theme

import androidx.compose.ui.graphics.Color

// ═══ iOS System Colors ═══
val SystemBlue = Color(0xFF007AFF)
val SystemRed = Color(0xFFFF3B30)
val SystemGreen = Color(0xFF34C759)
val SystemOrange = Color(0xFFFF9500)
val SystemYellow = Color(0xFFFFCC00)
val SystemPurple = Color(0xFFAF52DE)
val SystemTeal = Color(0xFF5AC8FA)
val SystemIndigo = Color(0xFF5856D6)

// ═══ iOS 背景 ═══
val IosBg = Color(0xFFF2F2F7)
val CardBg = Color(0xFFFFFFFF)
val NavBg = Color(0xD9F2F2F7)
val NavGlass = Color(0xD9FFFFFF)

// ═══ iOS Label Colors ═══
val IosLabel = Color(0xFF000000)
val IosSecondary = Color(0xFF8E8E93)
val IosTertiary = Color(0xFFC7C7CC)
val IosSeparator = Color(0xFFE5E5EA)
val IosFill = Color(0xFFE9E9EF)

// ═══ New Design System (Pixso AAui-v3) ═══
// Must come BEFORE category/chart maps that reference these
val MontraBackground    = Color(0xFFF5F4F1)
val MontraSurface       = Color(0xFFFFFFFF)
val MontraPrimary       = Color(0xFF3D8A5A)
val MontraPrimaryLight  = Color(0xFF4DAD6E)
val MontraPrimaryGradientEnd = Color(0xFF52B878)
val MontraRed           = Color(0xFFFD3C4A)
val MontraTextPrimary   = Color(0xFF1A1918)
val MontraTextSecondary = Color(0xFF6D6C6A)
val MontraTextTertiary  = Color(0xFF9C9B99)
val MontraTextDisabled  = Color(0xFFA8A7A5)
val MontraDivider       = Color(0xFFE5E4E1)
val MontraFill          = Color(0xFFEDECEA)
val MontraBorder        = Color(0xFFD1D0CD)

// Semantic colors
val WarningOrange       = Color(0xFFD89575)
val WarningOrangeBg     = Color(0xFFFFF0E8)
val WarningOrangeLight  = Color(0xFFFFF3EE)
val InfoBlue            = Color(0xFF5B9BD5)
val InfoBlueBg          = Color(0xFFE8F4FF)
val Gold                = Color(0xFFD4A64A)

// Green tints
val GreenLight          = Color(0xFFC8F0D8)
val GreenBg             = Color(0xFFE8FFF0)
val GreenMuted          = Color(0xFFE8F5EC)

// Ledger header / cover colours
val CoverGreen  = Color(0xFF3D8A5A)
val CoverBlue   = Color(0xFF5B9BD5)
val CoverOrange = Color(0xFFD89575)
val CoverGold   = Color(0xFFD4A64A)

val ledgerCoverColors = mapOf(
    "green"  to CoverGreen,
    "blue"   to CoverBlue,
    "orange" to CoverOrange,
    "gold"   to CoverGold
)
fun ledgerCoverColor(coverType: String): Color = ledgerCoverColors[coverType] ?: CoverGreen

// ═══ Category pastel backgrounds ═══
val CatFood          = Color(0xFFFFF0E8)
val CatHousing       = Color(0xFFEDE0FF)
val CatTransport     = Color(0xFFE8F4FF)
val CatShopping      = Color(0xFFFFF4E0)
val CatEntertainment = Color(0xFFFFE0EA)
val CatMedical       = Color(0xFFE0FFF0)
val CatSocial        = Color(0xFFFFE0D0)
val CatEducation     = Color(0xFFE0E8FF)
val CatInsurance     = Color(0xFFF0E8FF)
val CatOther         = Color(0xFFEDECEA)

val categoryColors = mapOf(
    "餐饮" to WarningOrange,
    "住宿" to Color(0xFF8B6FC0),
    "交通" to InfoBlue,
    "购物" to Color(0xFFE8A840),
    "娱乐" to MontraRed,
    "医疗" to Color(0xFF3DAA7A),
    "人情" to Color(0xFFD8705A),
    "教育" to Color(0xFF5A7ED8),
    "保险" to Color(0xFF8B5ACF),
    "其他" to Color(0xFF707070)
)

val categoryBgColors = mapOf(
    "餐饮" to CatFood,
    "住宿" to CatHousing,
    "交通" to CatTransport,
    "购物" to CatShopping,
    "娱乐" to CatEntertainment,
    "医疗" to CatMedical,
    "人情" to CatSocial,
    "教育" to CatEducation,
    "保险" to CatInsurance,
    "其他" to CatOther
)

// ═══ Dark Mode Colors ═══
val MontraDarkBg          = Color(0xFF1A1A18)
val MontraDarkSurface     = Color(0xFF242422)
val MontraDarkFill        = Color(0xFF2E2E2C)
val MontraDarkDivider     = Color(0xFF3A3A38)
val MontraDarkTextPrimary = Color(0xFFF0EFEC)
val MontraDarkTextSec     = Color(0xFFA8A7A4)
val MontraDarkTextTert    = Color(0xFF6E6D6A)

val WarningOrangeBgDark   = Color(0xFF3D2A1C)
val InfoBlueBgDark        = Color(0xFF1C2D3D)
val GreenBgDark           = Color(0xFF1C2D20)
val CatFoodDark           = Color(0xFF3D2A1C)
val CatTransportDark      = Color(0xFF1C2D3D)
val CatHotelDark          = Color(0xFF2A1C3D)
val CatShoppingDark       = Color(0xFF3D301C)
val CatEntertainmentDark  = Color(0xFF3D1C2A)
val CatOtherDark          = Color(0xFF2E2E2C)

// ═══ Chart ═══
val chartColors = listOf(
    MontraPrimary, WarningOrange, InfoBlue, Color(0xFF8B6FC0),
    Color(0xFFE8A840), MontraRed, Color(0xFF52B878), Color(0xFF707070)
)
