package com.example.ytdash

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class YtDashApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Required or OSM tile fetches 403 (cross-framework-setup.md §C).
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid", MODE_PRIVATE),
        )
    }
}
