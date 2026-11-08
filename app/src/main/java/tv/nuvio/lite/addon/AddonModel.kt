package tv.nuvio.lite.addon

data class CatalogItem(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val type: String,
    val description: String? = null
)
