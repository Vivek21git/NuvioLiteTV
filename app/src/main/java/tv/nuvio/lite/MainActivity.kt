package tv.nuvio.lite

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import tv.nuvio.lite.addon.CatalogItem
import tv.nuvio.lite.addon.InstalledAddon
import tv.nuvio.lite.addon.StremioAddonManager
import tv.nuvio.lite.player.PlayerActivity
import tv.nuvio.lite.ui.TvHomeScreen

class MainActivity : ComponentActivity() {

    private lateinit var addonManager: StremioAddonManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as LowRamApp
        addonManager = StremioAddonManager(this)

        var movies by mutableStateOf(emptyList<CatalogItem>())
        var series by mutableStateOf(emptyList<CatalogItem>())
        var addons by mutableStateOf(emptyList<InstalledAddon>())
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
                installedAddons = addons,
                isLoading = isLoading,
                isLowRam = app.isLowRamDevice,
                onAddAddon = { url ->
                    lifecycleScope.launch {
                        val success = addonManager.addAddonByUrl(url)
                        if (success) {
                            Toast.makeText(this@MainActivity, "Addon installed successfully", Toast.LENGTH_SHORT).show()
                            loadCatalogs()
                        } else {
                            Toast.makeText(this@MainActivity, "Failed to load manifest from URL", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onRemoveAddon = { url ->
                    addonManager.removeAddon(url)
                    loadCatalogs()
                },
                onItemSelected = { item ->
                    playStreamDirect(item)
                }
            )
        }
    }

    private fun playStreamDirect(item: CatalogItem) {
        val sampleHls = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("EXTRA_URL", sampleHls)
        }
        startActivity(intent)
    }
}
