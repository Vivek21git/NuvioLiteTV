package tv.nuvio.lite.player

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.media3.ui.PlayerView

class PlayerActivity : ComponentActivity() {

    private var playerManager: LowRamPlayerManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val streamUrl = intent.getStringExtra("EXTRA_URL") ?: run {
            finish()
            return
        }

        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val isLowRam = am.isLowRamDevice

        playerManager = LowRamPlayerManager(this, isLowRam).apply {
            play(streamUrl)
        }

        val playerView = PlayerView(this).apply {
            player = playerManager?.getPlayer()
            useController = true
        }
        setContentView(playerView)
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
}
