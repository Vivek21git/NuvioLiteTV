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

enum class AppTheme(val displayName: String, val bgHex: Long = 0, val surfaceHex: Long = 0, val accentHex: Long = 0) {
    AMOLED_BLACK("AMOLED Black", 0xFF000000, 0xFF141414, 0xFFE50914),
    COSMIC_PURPLE("Cosmic Purple", 0xFF0D0814, 0xFF191224, 0xFFA855F7),
    SPACE_BLUE("Space Blue", 0xFF070B14, 0xFF0F1826, 0xFF38BDF8)
}
