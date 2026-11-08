package tv.nuvio.lite

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tv.nuvio.lite.addon.CatalogItem
import tv.nuvio.lite.addon.InstalledAddon
import tv.nuvio.lite.addon.StreamLink
import tv.nuvio.lite.addon.StremioAddonManager
import tv.nuvio.lite.player.PlayerActivity
import tv.nuvio.lite.ui.TvHomeScreen

class MainActivity : ComponentActivity() {

    private lateinit var addonManager: StremioAddonManager
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as LowRamApp
        addonManager = StremioAddonManager(this)

        var movies by mutableStateOf(emptyList<CatalogItem>())
        var series by mutableStateOf(emptyList<CatalogItem>())
        var searchResults by mutableStateOf(emptyList<CatalogItem>())
        var addons by mutableStateOf(emptyList<InstalledAddon>())
        var selectedItem by mutableStateOf<CatalogItem?>(null)
        var streams by mutableStateOf(emptyList<StreamLink>())
        var isStreamsLoading by mutableStateOf(false)
        var isLoading by mutableStateOf(true)

        fun loadCatalogs() {
            isLoading = true
            addons = addonManager.getInstalledAddons()
            lifecycleScope.launch {
                val moviesDeferred = async { addonManager.getCatalog("movie") }
                val seriesDeferred = async { addonManager.getCatalog("series") }

                movies = moviesDeferred.await()
                series = seriesDeferred.await()
                isLoading = false
            }
        }

        loadCatalogs()

        setContent {
            TvHomeScreen(
                movies = movies,
                series = series,
                searchResults = searchResults,
                installedAddons = addons,
                selectedItem = selectedItem,
                availableStreams = streams,
                isStreamsLoading = isStreamsLoading,
                isLoading = isLoading,
                isLowRam = app.isLowRamDevice,
                onSearch = { query ->
                    searchJob?.cancel()
                    searchJob = lifecycleScope.launch {
                        delay(400)
                        searchResults = addonManager.searchCatalog(query)
                    }
                },
                onClearSearch = {
                    searchJob?.cancel()
                    searchResults = emptyList()
                },
                onAddAddon = { url ->
                    lifecycleScope.launch {
                        val success = addonManager.addAddonByUrl(url)
                        if (success) {
                            Toast.makeText(this@MainActivity, "Addon installed", Toast.LENGTH_SHORT).show()
                            loadCatalogs()
                        } else {
                            Toast.makeText(this@MainActivity, "Invalid addon manifest", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onRemoveAddon = { url ->
                    addonManager.removeAddon(url)
                    loadCatalogs()
                },
                onSelectContent = { item ->
                    selectedItem = item
                    isStreamsLoading = true
                    streams = emptyList()
                    lifecycleScope.launch {
                        streams = addonManager.resolveStreams(item.type, item.id)
                        isStreamsLoading = false
                    }
                },
                onCloseDetails = {
                    selectedItem = null
                    streams = emptyList()
                },
                onPlayInternal = { streamUrl ->
                    val intent = Intent(this@MainActivity, PlayerActivity::class.java).apply {
                        putExtra("EXTRA_URL", streamUrl)
                    }
                    startActivity(intent)
                },
                onPlayExternal = { streamUrl, title ->
                    playInExternalPlayer(streamUrl, title)
                }
            )
        }
    }

    private fun playInExternalPlayer(streamUrl: String, title: String) {
        try {
            val uri = Uri.parse(streamUrl)
            val mimeType = if (streamUrl.contains(".m3u8", ignoreCase = true)) {
                "application/x-mpegURL"
            } else if (streamUrl.contains(".mpd", ignoreCase = true)) {
                "application/dash+xml"
            } else {
                "video/*"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                putExtra("title", title)
                putExtra("video_title", title)
            }

            val chooser = Intent.createChooser(intent, "Play with...")
            startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(this, "No compatible video player found installed", Toast.LENGTH_SHORT).show()
        }
    }
}
