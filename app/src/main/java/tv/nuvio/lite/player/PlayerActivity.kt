package tv.nuvio.lite.player

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

@OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {

    private var playerManager: LowRamPlayerManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val streamUrl = intent.getStringExtra("EXTRA_URL") ?: run {
            finish()
            return
        }
        val mediaTitle = intent.getStringExtra("EXTRA_TITLE") ?: "Video Playback"

        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val isLowRam = am.isLowRamDevice

        val manager = LowRamPlayerManager(this, isLowRam).apply {
            play(streamUrl)
        }
        playerManager = manager

        setContent {
            ProfessionalPlayerScreen(
                title = mediaTitle,
                manager = manager,
                onBack = { finish() }
            )
        }
    }

    override fun onStop() {
        super.onStop()
        playerManager?.stopAndRelease()
        playerManager = null
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                finish()
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                playerManager?.seekRelative(-10_000L)
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                playerManager?.seekRelative(10_000L)
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_DPAD_CENTER -> {
                playerManager?.getPlayer()?.let {
                    if (it.isPlaying) it.pause() else it.play()
                }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    @Composable
    fun ProfessionalPlayerScreen(
        title: String,
        manager: LowRamPlayerManager,
        onBack: () -> Unit
    ) {
        val player = manager.getPlayer()

        var isPlaying by remember { mutableStateOf(player.isPlaying) }
        var currentPos by remember { mutableLongStateOf(0L) }
        var duration by remember { mutableLongStateOf(0L) }
        var isDragging by remember { mutableStateOf(false) }
        var dragPosition by remember { mutableFloatStateOf(0f) }

        var controlsVisible by remember { mutableStateOf(true) }
        var showAudioDialog by remember { mutableStateOf(false) }
        var showSubtitleDialog by remember { mutableStateOf(false) }
        var showSpeedDialog by remember { mutableStateOf(false) }
        var currentSpeed by remember { mutableFloatStateOf(1.0f) }

        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

        LaunchedEffect(Unit) {
            while (isActive) {
                isPlaying = player.isPlaying
                if (!isDragging) {
                    currentPos = player.currentPosition
                }
                duration = if (player.duration > 0) player.duration else 0L
                delay(350)
            }
        }

        LaunchedEffect(controlsVisible) {
            if (controlsVisible) {
                delay(4500)
                controlsVisible = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    controlsVisible = !controlsVisible
                }
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.9f)
                                )
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "←",
                                color = Color.White,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .clickable { onBack() }
                                    .padding(end = 16.dp)
                            )
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PlayerTopChip("${currentSpeed}x") { showSpeedDialog = true }
                            PlayerTopChip("Audio") { showAudioDialog = true }
                            PlayerTopChip("Subtitles") { showSubtitleDialog = true }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 40.dp, vertical = 28.dp)
                    ) {
                        val progressFraction = if (duration > 0) {
                            (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        Slider(
                            value = if (isDragging) dragPosition else progressFraction,
                            onValueChange = {
                                isDragging = true
                                dragPosition = it
                            },
                            onValueChangeFinished = {
                                val target = (dragPosition * duration).toLong()
                                manager.seekTo(target)
                                currentPos = target
                                isDragging = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color(0x44FFFFFF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(if (isDragging) (dragPosition * duration).toLong() else currentPos),
                                color = Color(0xCCFFFFFF),
                                fontSize = 13.sp
                            )
                            Text(
                                text = formatTime(duration),
                                color = Color(0x88FFFFFF),
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "|◁",
                                color = Color(0xAAFFFFFF),
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .clickable { manager.seekTo(0) }
                                    .padding(12.dp)
                            )

                            Spacer(modifier = Modifier.width(20.dp))

                            Text(
                                text = "↺ 10",
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .clickable { manager.seekRelative(-10_000L) }
                                    .padding(10.dp)
                            )

                            Spacer(modifier = Modifier.width(24.dp))

                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FFFFFF))
                                    .clickable {
                                        if (player.isPlaying) player.pause() else player.play()
                                        isPlaying = player.isPlaying
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isPlaying) "❚❚" else "▶",
                                    color = Color.White,
                                    fontSize = 17.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(24.dp))

                            Text(
                                text = "10 ↻",
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .clickable { manager.seekRelative(10_000L) }
                                    .padding(10.dp)
                            )

                            Spacer(modifier = Modifier.width(20.dp))

                            Text(
                                text = "▷|",
                                color = Color(0xAAFFFFFF),
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .clickable { manager.seekTo(duration) }
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            }

            if (showSpeedDialog) {
                SelectionListDialog(
                    title = "Playback Speed",
                    items = speeds.map { "${it}x" },
                    selectedIndex = speeds.indexOf(currentSpeed),
                    onDismiss = { showSpeedDialog = false },
                    onSelect = { index ->
                        currentSpeed = speeds[index]
                        manager.setSpeed(currentSpeed)
                        showSpeedDialog = false
                    }
                )
            }

            if (showAudioDialog) {
                val audioTracks = manager.getAudioTracks()
                TrackSelectionDialog(
                    title = "Audio Tracks",
                    options = audioTracks,
                    onDismiss = { showAudioDialog = false },
                    onSelect = { option ->
                        manager.selectTrack(option)
                        showAudioDialog = false
                    }
                )
            }

            if (showSubtitleDialog) {
                val subtitleTracks = manager.getSubtitleTracks()
                TrackSelectionDialog(
                    title = "Subtitles",
                    options = subtitleTracks,
                    allowDisable = true,
                    onDismiss = { showSubtitleDialog = false },
                    onSelect = { option ->
                        if (option != null) manager.selectTrack(option) else manager.disableSubtitles()
                        showSubtitleDialog = false
                    }
                )
            }
        }
    }

    @Composable
    fun PlayerTopChip(label: String, onClick: () -> Unit) {
        Surface(
            color = Color(0x33FFFFFF),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.clickable { onClick() }
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }

    @Composable
    fun TrackSelectionDialog(
        title: String,
        options: List<MediaTrackOption>,
        allowDisable: Boolean = false,
        onDismiss: () -> Unit,
        onSelect: (MediaTrackOption?) -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color(0xFF161520),
            title = { Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    if (allowDisable) {
                        item {
                            Text(
                                text = "Off (Disabled)",
                                color = Color.LightGray,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(null) }
                                    .padding(vertical = 10.dp)
                            )
                        }
                    }
                    if (options.isEmpty()) {
                        item {
                            Text("No tracks available for this stream.", color = Color.Gray, fontSize = 13.sp)
                        }
                    } else {
                        items(options) { opt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(opt) }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = opt.label,
                                    color = if (opt.isSelected) Color(0xFF38BDF8) else Color.White,
                                    fontSize = 14.sp
                                )
                                if (opt.isSelected) Text("✓", color = Color(0xFF38BDF8), fontSize = 14.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text("Close", color = Color.Gray) } }
        )
    }

    @Composable
    fun SelectionListDialog(
        title: String,
        items: List<String>,
        selectedIndex: Int,
        onDismiss: () -> Unit,
        onSelect: (Int) -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color(0xFF161520),
            title = { Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(items.size) { index ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(index) }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = items[index],
                                color = if (index == selectedIndex) Color(0xFF38BDF8) else Color.White,
                                fontSize = 14.sp
                            )
                            if (index == selectedIndex) Text("✓", color = Color(0xFF38BDF8), fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text("Close", color = Color.Gray) } }
        )
    }

    private fun formatTime(ms: Long): String {
        val totalSecs = (ms / 1000).coerceAtLeast(0)
        val hrs = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return if (hrs > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }
}
