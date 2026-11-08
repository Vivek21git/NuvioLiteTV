package tv.nuvio.lite

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import tv.nuvio.lite.addon.CatalogItem
import tv.nuvio.lite.addon.StremioAddonManager
import tv.nuvio.lite.player.PlayerActivity
import tv.nuvio.lite.ui.TvHomeScreen

class MainActivity : ComponentActivity() {

    private val addonManager = StremioAddonManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as LowRamApp
        var movies by mutableStateOf(emptyList<CatalogItem>())
        var series by mutableStateOf(emptyList<CatalogItem>())
        var isLoading by mutableStateOf(true)

        lifecycleScope.launch {
            val moviesDeferred = async { addonManager.getCatalog("movie") }
            val seriesDeferred = async { addonManager.getCatalog("series") }

            movies = moviesDeferred.await()
            series = seriesDeferred.await()
            isLoading = false
        }

        setContent {
            TvHomeScreen(
                movies = movies,
                series = series,
                isLoading = isLoading,
                isLowRam = app.isLowRamDevice,
                onItemSelected = { item ->
                    playStreamDirect(item)
                }
            )
        }
    }

    private fun playStreamDirect(item: CatalogItem) {
        lifecycleScope.launch {
            val sampleHls = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
            val intent = Intent(this@MainActivity, PlayerActivity::class.java).apply {
                putExtra("EXTRA_URL", sampleHls)
            }
            startActivity(intent)
        }
    }
}
