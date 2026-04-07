package dev.elainedb.ytdash_android_claude.presentation.map

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import dagger.hilt.android.AndroidEntryPoint
import dev.elainedb.ytdash_android_claude.data.local.VideoDao
import dev.elainedb.ytdash_android_claude.data.local.toVideo
import dev.elainedb.ytdash_android_claude.domain.model.Video
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import javax.inject.Inject

@AndroidEntryPoint
class MapActivity : AppCompatActivity() {

    @Inject
    lateinit var videoDao: VideoDao

    private lateinit var mapView: MapView
    private var bottomSheet: LinearLayout? = null

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MapActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = "dev.elainedb.ytdash_android_claude/1.0"
        Configuration.getInstance().osmdroidTileCache = cacheDir

        val rootLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        mapView = MapView(this).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        rootLayout.addView(mapView)

        setContentView(rootLayout)

        lifecycleScope.launch {
            val entities = videoDao.getVideosWithLocation().first()
            val videos = entities.map { it.toVideo() }
            addMarkers(videos, rootLayout)
        }
    }

    private fun addMarkers(videos: List<Video>, rootLayout: FrameLayout) {
        if (videos.isEmpty()) return

        val geoPoints = mutableListOf<GeoPoint>()

        videos.forEach { video ->
            val lat = video.locationLatitude ?: return@forEach
            val lon = video.locationLongitude ?: return@forEach
            val point = GeoPoint(lat, lon)
            geoPoints.add(point)

            val marker = Marker(mapView)
            marker.position = point
            marker.title = video.title
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            marker.setOnMarkerClickListener { _, _ ->
                showBottomSheet(video, rootLayout)
                true
            }

            mapView.overlays.add(marker)
        }

        if (geoPoints.isNotEmpty()) {
            val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
            mapView.post {
                mapView.zoomToBoundingBox(boundingBox, true, 100)
            }
        }

        mapView.invalidate()
    }

    private fun showBottomSheet(video: Video, rootLayout: FrameLayout) {
        bottomSheet?.let { rootLayout.removeView(it) }

        val displayHeight = resources.displayMetrics.heightPixels
        val sheetHeight = (displayHeight * 0.25).toInt()

        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFFFFFF.toInt())
            elevation = 16f
            setPadding(24, 16, 24, 16)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                sheetHeight,
                Gravity.BOTTOM
            )

            setOnClickListener {
                try {
                    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:${video.id}"))
                    startActivity(appIntent)
                } catch (_: Exception) {
                    val webIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com/watch?v=${video.id}")
                    )
                    startActivity(webIntent)
                }
            }
        }

        val thumbnail = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            load(video.thumbnailUrl)
        }
        sheet.addView(thumbnail)

        val title = TextView(this).apply {
            text = video.title
            textSize = 14f
            maxLines = 1
            setPadding(0, 8, 0, 4)
        }
        sheet.addView(title)

        val channel = TextView(this).apply {
            text = video.channelName
            textSize = 12f
        }
        sheet.addView(channel)

        val date = TextView(this).apply {
            text = video.publishedAt.take(10)
            textSize = 12f
        }
        sheet.addView(date)

        if (video.locationCity != null || video.locationCountry != null) {
            val location = TextView(this).apply {
                text = listOfNotNull(video.locationCity, video.locationCountry).joinToString(", ")
                textSize = 12f
            }
            sheet.addView(location)
        }

        rootLayout.addView(sheet)
        bottomSheet = sheet
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
