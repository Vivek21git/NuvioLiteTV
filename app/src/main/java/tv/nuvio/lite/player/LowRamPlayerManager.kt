package tv.nuvio.lite.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
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
                if (isLowRam) 4_000 else 12_000,
                if (isLowRam) 10_000 else 25_000,
                1_000,
                2_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setTargetBufferBytes(if (isLowRam) 10 * 1024 * 1024 else 30 * 1024 * 1024)
            .build()

        player = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
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

    fun seekRelative(offsetMs: Long) {
        val target = (player.currentPosition + offsetMs).coerceIn(0, player.duration.coerceAtLeast(0))
        player.seekTo(target)
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceIn(0, player.duration.coerceAtLeast(0)))
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
                    val selected = group.isTrackSelected(trackIndex)
                    result.add(
                        AudioTrackOption(
                            groupIndex = groupIndex,
                            trackIndex = trackIndex,
                            label = label,
                            isSelected = selected
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
        val format = player.videoFormat ?: return "Detecting..."
        val width = format.width
        val height = format.height
        val fps = if (format.frameRate > 0) " @ ${format.frameRate.toInt()}fps" else ""
        return if (width > 0 && height > 0) "${width}x${height}$fps" else "Standard Stream"
    }

    fun stopAndRelease() {
        player.stop()
        player.clearMediaItems()
        player.release()
    }
}
