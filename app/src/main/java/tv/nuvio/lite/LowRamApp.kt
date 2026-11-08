package tv.nuvio.lite.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultAllocator

data class AudioTrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean
)

@OptIn(UnstableApi::class)
class LowRamPlayerManager(context: Context, isLowRam: Boolean) {

    private val trackSelector: DefaultTrackSelector = DefaultTrackSelector(context)
    private val player: ExoPlayer

    init {
        val allocator = DefaultAllocator(true, 64 * 1024)
        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(allocator)
            .setBufferDurationsMs(
                if (isLowRam) 6_000 else 15_000,
                if (isLowRam) 18_000 else 30_000,
                1_000,
                2_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setTargetBufferBytes(if (isLowRam) 12 * 1024 * 1024 else 35 * 1024 * 1024)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(8_000)
            .setReadTimeoutMs(10_000)
            .setUserAgent("NuvioTV/2.0")

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        player = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .build().apply {
                playWhenReady = true
            }
    }

    fun getPlayer(): ExoPlayer = player

    fun play(url: String) {
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    fun seekRelative(offsetMs: Long) {
        val current = player.currentPosition
        val duration = if (player.duration > 0) player.duration else Long.MAX_VALUE
        val target = (current + offsetMs).coerceIn(0L, duration)
        player.seekTo(target)
    }

    fun seekTo(positionMs: Long) {
        val duration = if (player.duration > 0) player.duration else Long.MAX_VALUE
        player.seekTo(positionMs.coerceIn(0L, duration))
    }

    fun getAvailableAudioTracks(): List<AudioTrackOption> {
        val tracks = player.currentTracks
        val result = mutableListOf<AudioTrackOption>()
        for (groupIndex in 0 until tracks.groups.size) {
            val group = tracks.groups[groupIndex]
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (trackIndex in 0 until group.length) {
                    val format = group.getTrackFormat(trackIndex)
                    val lang = format.language?.uppercase() ?: "Undetermined"
                    val label = format.label ?: "$lang (${format.sampleMimeType?.substringAfter('/') ?: "audio"})"
                    result.add(
                        AudioTrackOption(
                            groupIndex = groupIndex,
                            trackIndex = trackIndex,
                            label = label,
                            isSelected = group.isTrackSelected(trackIndex)
                        )
                    )
                }
            }
        }
        return result
    }

    fun selectAudioTrack(option: AudioTrackOption) {
        val tracks = player.currentTracks
        if (option.groupIndex < tracks.groups.size) {
            val group = tracks.groups[option.groupIndex].mediaTrackGroup
            val override = TrackSelectionOverride(group, listOf(option.trackIndex))
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setOverrideForType(override)
                .build()
        }
    }

    fun getVideoQualityInfo(): String {
        val format = player.videoFormat ?: return "HD"
        val height = format.height
        return when {
            height >= 2160 -> "4K UHD"
            height >= 1080 -> "1080p"
            height >= 720 -> "720p"
            height > 0 -> "${height}p"
            else -> "HD"
        }
    }

    fun stopAndRelease() {
        player.stop()
        player.clearMediaItems()
        player.release()
    }
}
