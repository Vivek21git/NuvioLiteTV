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
