package tv.nuvio.lite.addon

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class StremioAddonManager(context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nuvio_addons_prefs", Context.MODE_PRIVATE)

    fun getInstalledAddons(): List<InstalledAddon> {
        val rawJson = prefs.getString("addons_list", null) ?: return listOf(
            InstalledAddon("cinemeta", "Cinemeta Official", "https://v3-cinemeta.strem.io")
        )
        val array = JSONArray(rawJson)
        val list = mutableListOf<InstalledAddon>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                InstalledAddon(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    transportUrl = obj.getString("transportUrl"),
                    description = obj.optString("description")
                )
            )
        }
        return list
    }

    suspend fun addAddonByUrl(rawUrl: String): Boolean = withContext(Dispatchers.IO) {
        val cleanUrl = rawUrl.trim().trimEnd('/')
        val manifestUrl = if (cleanUrl.endsWith("/manifest.json")) cleanUrl else "$cleanUrl/manifest.json"
        val baseUrl = manifestUrl.removeSuffix("/manifest.json")

        try {
            val request = Request.Builder().url(manifestUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val json = JSONObject(response.body?.string().orEmpty())
                val id = json.optString("id", "addon_${System.currentTimeMillis()}")
                val name = json.optString("name", "Custom Addon")
                val desc = json.optString("description", "")

                val current = getInstalledAddons().toMutableList()
                current.removeAll { it.transportUrl == baseUrl }
                current.add(InstalledAddon(id, name, baseUrl, desc))

                saveAddons(current)
                return@withContext true
            }
        } catch (e: Exception) {
            return@withContext false
        }
    }

    fun removeAddon(transportUrl: String) {
        val current = getInstalledAddons().filterNot { it.transportUrl == transportUrl }
        saveAddons(current)
    }

    private fun saveAddons(addons: List<InstalledAddon>) {
        val array = JSONArray()
        addons.forEach { addon ->
            val obj = JSONObject().apply {
                put("id", addon.id)
                put("name", addon.name)
                put("transportUrl", addon.transportUrl)
                put("description", addon.description)
            }
            array.put(obj)
        }
        prefs.edit().putString("addons_list", array.toString()).apply()
    }

    suspend fun getCatalog(type: String = "movie"): List<CatalogItem> = withContext(Dispatchers.IO) {
        val addons = getInstalledAddons()
        val allItems = mutableListOf<CatalogItem>()

        for (addon in addons) {
            val url = "${addon.transportUrl}/catalog/$type/top.json"
            parseMetasFromUrl(url, type, allItems)
        }
        allItems.distinctBy { it.id }
    }

    suspend fun searchCatalog(query: String): List<CatalogItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val addons = getInstalledAddons()
        val results = mutableListOf<CatalogItem>()

        for (addon in addons) {
            listOf("movie", "series").forEach { type ->
                val url = "${addon.transportUrl}/catalog/$type/top/search=$encodedQuery.json"
                parseMetasFromUrl(url, type, results)
            }
        }
        results.distinctBy { it.id }
    }

    private fun parseMetasFromUrl(url: String, type: String, outList: MutableList<CatalogItem>) {
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string().orEmpty())
                    val metas = json.optJSONArray("metas") ?: return
                    for (i in 0 until metas.length()) {
                        val m = metas.getJSONObject(i)
                        outList.add(
                            CatalogItem(
                                id = m.getString("id"),
                                title = m.optString("name", "Untitled"),
                                posterUrl = m.optString("poster"),
                                type = type,
                                description = m.optString("description")
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun resolveStreams(type: String, id: String): List<StreamLink> = withContext(Dispatchers.IO) {
        val addons = getInstalledAddons()
        val streamLinks = mutableListOf<StreamLink>()

        for (addon in addons) {
            val url = "${addon.transportUrl}/stream/$type/$id.json"
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val json = JSONObject(response.body?.string().orEmpty())
                    val streams = json.optJSONArray("streams") ?: return@use

                    for (i in 0 until streams.length()) {
                        val s = streams.getJSONObject(i)
                        val streamUrl = s.optString("url")
                        if (streamUrl.isNotBlank()) {
                            streamLinks.add(
                                StreamLink(
                                    addonName = addon.name,
                                    title = s.optString("title", s.optString("name", "Stream #${i + 1}")),
                                    url = streamUrl
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        streamLinks
    }
}
