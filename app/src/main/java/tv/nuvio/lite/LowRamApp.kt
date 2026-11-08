package tv.nuvio.lite

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.BitmapFactoryDecoder
import coil.memory.MemoryCache

class LowRamApp : Application(), ImageLoaderFactory {

    val isLowRamDevice: Boolean by lazy {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.isLowRamDevice
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .components {
                add(BitmapFactoryDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this@LowRamApp)
                    .maxSizePercent(if (isLowRamDevice) 0.10 else 0.25)
                    .build()
            }
            .crossfade(false)
            .build()
    }
}
