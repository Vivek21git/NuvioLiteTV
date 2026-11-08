package tv.nuvio.lite.player

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class PlayerActivity : ComponentActivity() {

    private var playerManager: LowRamPlayerManager? = null
    private var playerView: PlayerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val streamUrl = intent.getStringExtra("EXTRA_URL") ?: run {
            finish()
            return
        }

        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val isLowRam = am.isLowRamDevice

        val manager = LowRamPlayerManager(this, isLowRam).apply {
            play(streamUrl)
        }
        playerManager = manager

        setContent {
            EnhancedPlayerScreen(
                manager = manager,
                onSetResizeMode = { mode ->
                    playerView?.resizeMode = mode
                },
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
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    @Composable
    fun EnhancedPlayerScreen(
        manager: LowRamPlayerManager,
        onSetResizeMode: (Int) -> Unit,
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
        var resizeModeIndex by remember { mutableIntStateOf(0) }
        val resizeModes = listOf(
            "FIT" to AspectRatioFrameLayout.RESIZE_MODE_FIT,
            "ZOOM" to AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
            "FILL" to AspectRatioFrameLayout.RESIZE_MODE_FILL
        )

        var qualityInfo by remember { mutableStateOf("Loading...") }

        // Periodic ticker for progress and media statistics
        LaunchedEffect(Unit) {
            while (isActive) {
                isPlaying = player.isPlaying
                if (!isDragging) {
                    currentPos = player.currentPosition
                }
                duration = player.duration.coerceAtLeast(0L)
                qualityInfo = manager.getVideoQualityInfo()
                delay(500)
            }
        }

        // Auto-hide controls timer
        LaunchedEffect(controlsVisible) {
            if (controlsVisible) {
                delay(4000)
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
            // Native Media3 Surface View
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false // Custom overlay is used
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        playerView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // On-Screen HUD Controls
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
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                ) {
                    // Top Bar: Back, Quality Badge, Frame Aspect Ratio, Audio Tracks
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = onBack,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x66FFFFFF)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Back", color = Color.White, fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Surface(
                                color = Color(0x33000000),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = qualityInfo,
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row {
                            // Frame Size Toggle
                            Button(
                                onClick = {
                                    resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
                                    onSetResizeMode(resizeModes[resizeModeIndex].second)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x66FFFFFF)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(resizeModes[resizeModeIndex].first, color = Color.White, fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Audio Selection Button
                            Button(
                                onClick = { showAudioDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Audio", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }

                    // Center Jump & Play/Pause Controls
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { manager.seekRelative(-10_000L) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x88222834)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("-10s", fontSize = 14.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                if (player.isPlaying) player.pause() else player.play()
                                isPlaying = player.isPlaying
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(width = 90.dp, height = 48.dp)
                        ) {
                            Text(if (isPlaying) "Pause" else "Play", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { manager.seekRelative(10_000L) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x88222834)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+10s", fontSize = 14.sp, color = Color.White)
                        }
                    }

                    // Bottom Bar: Draggable Seek Slider & Time Counters
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 24.dp, vertical = 18.dp)
                    ) {
                        val currentFraction = if (duration > 0) {
                            (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        Slider(
                            value = if (isDragging) dragPosition else currentFraction,
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
                                thumbColor = Color(0xFF3B82F6),
                                activeTrackColor = Color(0xFF3B82F6),
                                inactiveTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatTime(if (isDragging) (dragPosition * duration).toLong() else currentPos), color = Color.White, fontSize = 12.sp)
                            Text(formatTime(duration), color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Audio Track Selector Modal
            if (showAudioDialog) {
                val tracks = manager.getAvailableAudioTracks()
                AlertDialog(
                    onDismissRequest = { showAudioDialog = false },
                    containerColor = Color(0xFF161A22),
                    title = { Text("Select Audio Track", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    text = {
                        if (tracks.isEmpty()) {
                            Text("No alternate audio tracks available.", color = Color.Gray, fontSize = 13.sp)
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(tracks) { option ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                manager.selectAudioTrack(option)
                                                showAudioDialog = false
                                            }
                                            .padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = option.label,
                                            color = if (option.isSelected) Color(0xFF3B82F6) else Color.White,
                                            fontWeight = if (option.isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 14.sp
                                        )
                                        if (option.isSelected) {
                                            Text("Active", color = Color(0xFF3B82F6), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showAudioDialog = false }) {
                            Text("Close", color = Color.LightGray)
                        }
                    }
                )
            }
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSecs = (ms / 1000).coerceAtLeast(0)
        val hrs = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return if (hrs > 0) {
            String.format("%d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }
}
