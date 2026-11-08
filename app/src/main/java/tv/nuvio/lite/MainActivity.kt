package tv.nuvio.lite

import android.content.Context
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
import tv.nuvio.lite.addon.AppTheme
import tv.nuvio.lite.addon.CatalogItem
import tv.nuvio.lite.addon.InstalledAddon
import tv.nuvio.lite.addon.StreamLink
import tv.nuvio.lite.addon.StremioAddonManager
import tv.nuvio.lite.addon.WatchlistManager
import tv.nuvio.lite.player.PlayerActivity
import tv.nuvio.lite.ui.TvHomeScreen

class MainActivity : ComponentActivity() {

    private lateinit var addonManager: StremioAddonManager
    private lateinit var watchlistManager: WatchlistManager
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as LowRamApp
        addonManager = StremioAddonManager(this)
        watchlistManager = WatchlistManager(this)

        val themePrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val initialThemeName = themePrefs.getString("selected_theme", AppTheme.AMOLED_BLACK.name) ?: AppTheme.AMOLED_BLACK.name
        val initialTheme = try { AppTheme.valueOf(initialThemeName) } catch (e: Exception) { AppTheme.AMOLED_BLACK }

        var currentTheme by mutableStateOf(initialTheme)
        var movies by mutableStateOf(emptyList<CatalogItem>())
        var series by mutableStateOf(emptyList<CatalogItem>())
        var watchlist by mutableStateOf(watchlistManager.getWatchlist())
        var searchResults by mutableStateOf(emptyList<CatalogItem>())
        var addons by mutableStateOf(emptyList<InstalledAddon>())
        var selectedItem by mutableStateOf<CatalogItem?>(null)
        var streams by mutableStateOf(emptyList<StreamLink>())
        var isStreamsLoading by mutableStateOf(false)
        var isLoading by mutableStateOf(true)

        fun loadCatalogs() {
            isLoading = true
            addons = addonManager.getInstalledAddons()
            watchlist = watchlistManager.getWatchlist()
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
            val isItemSaved = selectedItem?.let { watchlistManager.isInWatchlist(it.id) } ?: false

            TvHomeScreen(
                movies = movies,
                series = series,
                watchlist = watchlist,
                searchResults = searchResults,
                installedAddons = addons,
                selectedItem = selectedItem,
                availableStreams = streams,
                isStreamsLoading = isStreamsLoading,
                isItemInWatchlist = isItemSaved,
                isLoading = isLoading,
                isLowRam = app.isLowRamDevice,
                currentTheme = currentTheme,
                onSelectTheme = { newTheme ->
                    currentTheme = newTheme
                    themePrefs.edit().putString("selected_theme", newTheme.name).apply()
                },
                onToggleWatchlist = { item ->
                    watchlistManager.toggleWatchlist(item)
                    watchlist = watchlistManager.getWatchlist()
                },
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
                            Toast.makeText(this@MainActivity, "Add-on installed", Toast.LENGTH_SHORT).show()
                            loadCatalogs()
                        } else {
                            Toast.makeText(this@MainActivity, "Failed to load manifest", Toast.LENGTH_LONG).show()
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
                onPlayInternal = { streamUrl, title ->
                    val intent = Intent(this@MainActivity, PlayerActivity::class.java).apply {
                        putExtra("EXTRA_URL", streamUrl)
                        putExtra("EXTRA_TITLE", title)
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
            Toast.makeText(this, "No external video player found", Toast.LENGTH_SHORT).show()
        }
    }
}
