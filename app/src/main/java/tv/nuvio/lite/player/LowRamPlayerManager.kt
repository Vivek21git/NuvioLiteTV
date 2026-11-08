package tv.nuvio.lite.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.upstream.DefaultAllocator

@OptIn(UnstableApi::class)
class LowRamPlayerManager(context: Context, isLowRam: Boolean) {

    private val player: ExoPlayer

    init {
        val allocator = DefaultAllocator(true, 64 * 1024)
        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(allocator)
            .setBufferDurationsMs(
                if (isLowRam) 4_000 else 12_000,
                if (isLowRam) 10_000 else 25_000,
                1_000,
                2_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setTargetBufferBytes(if (isLowRam) 10 * 1024 * 1024 else 30 * 1024 * 1024)
            .build()

        player = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build().apply {
                playWhenReady = true
            }
    }

    fun getPlayer(): ExoPlayer = player

    fun play(url: String) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
    }

    fun stopAndRelease() {
        player.stop()
        player.clearMediaItems()
        player.release()
    }
}
