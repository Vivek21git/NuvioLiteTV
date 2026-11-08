package tv.nuvio.lite.addon

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class StremioAddonManager(context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nuvio_addons_prefs", Context.MODE_PRIVATE)

    private val defaultCinemeta = "https://v3-cinemeta.strem.io/manifest.json"

    // Load saved addons
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

    // Save and register a new addon URL (supports raw manifest URL or base URL)
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

                val array = JSONArray()
                current.forEach { addon ->
                    val obj = JSONObject().apply {
                        put("id", addon.id)
                        put("name", addon.name)
                        put("transportUrl", addon.transportUrl)
                        put("description", addon.description)
                    }
                    array.put(obj)
                }
                prefs.edit().putString("addons_list", array.toString()).apply()
                return@withContext true
            }
        } catch (e: Exception) {
            return@withContext false
        }
    }

    fun removeAddon(transportUrl: String) {
        val current = getInstalledAddons().filterNot { it.transportUrl == transportUrl }
        val array = JSONArray()
        current.forEach { addon ->
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
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string().orEmpty())
                        val metas = json.optJSONArray("metas") ?: return@use
                        for (i in 0 until metas.length()) {
                            val m = metas.getJSONObject(i)
                            allItems.add(
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
            } catch (_: Exception) {
                // Skip faulty addon and continue
            }
        }
        allItems.distinctBy { it.id }
    }
}
