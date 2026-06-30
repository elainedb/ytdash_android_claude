package com.example.ytdash

import android.app.Application
import com.example.ytdash.di.AppContainer
import org.osmdroid.config.Configuration

class YtdashApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // osmdroid needs a User-Agent (else OSM tile fetches 403) and a writable cache path.
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = cacheDir
            osmdroidTileCache = cacheDir.resolve("osmdroid-tiles")
        }
    }
}
