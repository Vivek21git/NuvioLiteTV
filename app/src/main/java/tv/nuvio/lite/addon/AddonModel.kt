package tv.nuvio.lite.addon

import androidx.compose.ui.graphics.Color

data class CatalogItem(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val type: String,
    val description: String? = null
)

data class InstalledAddon(
    val id: String,
    val name: String,
    val transportUrl: String,
    val description: String? = null
)

data class StreamLink(
    val addonName: String,
    val title: String,
    val url: String
)

enum class NavSection(val title: String) {
    MOVIES("Movies"),
    SERIES("Series"),
    WATCHLIST("Watchlist"),
    SETTINGS("Settings")
}

enum class AppTheme(val displayName: String, val bg: Color, val surface: Color, val accent: Color) {
    AMOLED_BLACK("AMOLED Black", Color(0xFF000000), Color(0xFF121212), Color(0xFFE50914)),
    COSMIC_PURPLE("Cosmic Purple", Color(0xFF0F0B18), Color(0xFF1A1429), Color(0xFFA855F7)),
    SPACE_BLUE("Space Blue", Color(0xFF070B14), Color(0xFF0F1829), Color(0xFF38BDF8))
}
