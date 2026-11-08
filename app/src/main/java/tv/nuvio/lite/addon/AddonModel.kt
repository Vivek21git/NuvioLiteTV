package tv.nuvio.lite.addon

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

enum class AppTheme(val displayName: String, val bgHex: Long, val surfaceHex: Long, val accentHex: Long) {
    AMOLED_BLACK("AMOLED Black", 0xFF000000, 0xFF121212, 0xFFE50914),
    COSMIC_PURPLE("Cosmic Purple", 0xFF0F0B18, 0xFF1A1429, 0xFFA855F7),
    SPACE_BLUE("Space Blue", 0xFF070B14, 0xFF0F1829, 0xFF38BDF8)
}
