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
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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

        LaunchedEffect(Unit) {
            while (isActive) {
                isPlaying = player.isPlaying
                if (!isDragging) {
                    currentPos = player.currentPosition
                }
                duration = if (player.duration > 0) player.duration else 0L
                delay(400)
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

            // Minimalist Control Overlay (Matching Image 2)
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
                                    Color.Black.copy(alpha = 0.65f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                ) {
                    // Top Title (Matching Image 2 top left)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 36.dp, top = 28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Bottom Seekbar & Controls (Matching Image 2)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 48.dp, vertical = 32.dp)
                    ) {
                        // Clean White Timeline Slider
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
                                inactiveTrackColor = Color(0x55FFFFFF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Timestamps row: 23:13                               45:20
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Controls: |<   -10s   ||   +10s   >|
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Prev chapter
                            Text(
                                text = "|◁",
                                color = Color(0xAAFFFFFF),
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .clickable { manager.seekTo(0) }
                                    .padding(12.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // -10s Jump
                            Text(
                                text = "↺ 10",
                                color = Color.White,
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .clickable { manager.seekRelative(-10_000L) }
                                    .padding(8.dp)
                            )

                            Spacer(modifier = Modifier.width(20.dp))

                            // Play / Pause Circle
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
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
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            // +10s Jump
                            Text(
                                text = "10 ↻",
                                color = Color.White,
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .clickable { manager.seekRelative(10_000L) }
                                    .padding(8.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Next
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
