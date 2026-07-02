package com.example.ytdash

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class YtDashApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // osmdroid requires a real User-Agent or OSM tile fetches 403; also point its cache at
        // our app's cache dir (scoped storage, no extra permission needed).
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidTileCache = cacheDir
        }
    }
}
