package tv.nuvio.lite.addon

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class WatchlistManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nuvio_watchlist_prefs", Context.MODE_PRIVATE)

    fun getWatchlist(): List<CatalogItem> {
        val raw = prefs.getString("watchlist_items", null) ?: return emptyList()
        val array = JSONArray(raw)
        val list = mutableListOf<CatalogItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                CatalogItem(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    posterUrl = obj.optString("posterUrl", null),
                    type = obj.getString("type"),
                    description = obj.optString("description", null)
                )
            )
        }
        return list
    }

    fun toggleWatchlist(item: CatalogItem): Boolean {
        val current = getWatchlist().toMutableList()
        val exists = current.any { it.id == item.id }
        if (exists) {
            current.removeAll { it.id == item.id }
        } else {
            current.add(0, item)
        }
        val array = JSONArray()
        current.forEach {
            val obj = JSONObject().apply {
                put("id", it.id)
                put("title", it.title)
                put("posterUrl", it.posterUrl)
                put("type", it.type)
                put("description", it.description)
            }
            array.put(obj)
        }
        prefs.edit().putString("watchlist_items", array.toString()).apply()
        return !exists
    }

    fun isInWatchlist(itemId: String): Boolean {
        return getWatchlist().any { it.id == itemId }
    }
}
