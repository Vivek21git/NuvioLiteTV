package tv.nuvio.lite.addon

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class StremioAddonManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private val cinemetaBase = "https://v3-cinemeta.strem.io"
    private val memoryCache = mutableMapOf<String, List<CatalogItem>>()

    suspend fun getCatalog(type: String = "movie"): List<CatalogItem> = withContext(Dispatchers.IO) {
        memoryCache[type]?.let { return@withContext it }

        val url = "$cinemetaBase/catalog/$type/top.json"
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val json = JSONObject(response.body?.string().orEmpty())
                val metas = json.getJSONArray("metas")
                val items = mutableListOf<CatalogItem>()

                for (i in 0 until metas.length()) {
                    val m = metas.getJSONObject(i)
                    items.add(
                        CatalogItem(
                            id = m.getString("id"),
                            title = m.optString("name", "Untitled"),
                            posterUrl = m.optString("poster"),
                            type = type,
                            description = m.optString("description")
                        )
                    )
                }
                memoryCache[type] = items
                items
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
