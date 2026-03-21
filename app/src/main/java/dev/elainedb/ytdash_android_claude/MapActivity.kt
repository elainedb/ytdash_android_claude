package dev.elainedb.ytdash_android_claude

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import dev.elainedb.ytdash_android_claude.database.VideoDatabase
import dev.elainedb.ytdash_android_claude.database.toVideo
import dev.elainedb.ytdash_android_claude.model.Video
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapActivity : ComponentActivity() {

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MapActivity::class.java)
        }
    }

    private lateinit var mapView: MapView
    private var bottomSheet: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = packageName

        val rootLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        mapView = MapView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
        }
        rootLayout.addView(mapView)

        setContentView(rootLayout)

        loadVideosOnMap(rootLayout)
    }

    private fun loadVideosOnMap(rootLayout: FrameLayout) {
        lifecycleScope.launch {
            val videoDao = VideoDatabase.getDatabase(this@MapActivity).videoDao()
            val entities = videoDao.getVideosWithLocation()
            val videos = entities.map { it.toVideo() }

            if (videos.isEmpty()) return@launch

            val geoPoints = mutableListOf<GeoPoint>()

            videos.forEach { video ->
                val lat = video.locationLatitude ?: return@forEach
                val lon = video.locationLongitude ?: return@forEach
                val point = GeoPoint(lat, lon)
                geoPoints.add(point)

                val marker = Marker(mapView)
                marker.position = point
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = video.title

                marker.setOnMarkerClickListener { _, _ ->
                    showBottomSheet(rootLayout, video)
                    true
                }

                mapView.overlays.add(marker)
            }

            if (geoPoints.isNotEmpty()) {
                val boundingBox = BoundingBox.fromGeoPointsSafe(geoPoints)
                mapView.post {
                    mapView.zoomToBoundingBox(boundingBox, true, 100)
                }
            }

            mapView.invalidate()
        }
    }

    private fun showBottomSheet(rootLayout: FrameLayout, video: Video) {
        bottomSheet?.let { rootLayout.removeView(it) }

        val screenHeight = resources.displayMetrics.heightPixels
        val sheetHeight = (screenHeight * 0.25).toInt()

        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                sheetHeight,
                Gravity.BOTTOM
            )
            setBackgroundColor(0xFFF5F5F5.toInt())
            setPadding(16, 16, 16, 16)
            elevation = 8f
            isClickable = true
            isFocusable = true
            setOnClickListener { openVideo(video.id) }
        }

        val thumbnail = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            scaleType = ImageView.ScaleType.CENTER_CROP
            load(video.thumbnailUrl)
        }
        sheet.addView(thumbnail)

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            setPadding(16, 0, 0, 0)
        }

        infoLayout.addView(TextView(this).apply {
            text = video.title
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            maxLines = 2
        })

        infoLayout.addView(TextView(this).apply {
            text = video.channelName
            textSize = 12f
        })

        infoLayout.addView(TextView(this).apply {
            text = video.publishedAt.take(10)
            textSize = 11f
        })

        if (video.tags.isNotEmpty()) {
            infoLayout.addView(TextView(this).apply {
                text = video.tags.take(3).joinToString(", ")
                textSize = 11f
            })
        }

        if (video.locationCity != null || video.locationCountry != null) {
            val locationParts = listOfNotNull(video.locationCity, video.locationCountry)
            infoLayout.addView(TextView(this).apply {
                text = locationParts.joinToString(", ")
                textSize = 11f
            })
            if (video.locationLatitude != null && video.locationLongitude != null) {
                infoLayout.addView(TextView(this).apply {
                    text = "(${video.locationLatitude}, ${video.locationLongitude})"
                    textSize = 10f
                })
            }
        }

        if (video.recordingDate != null) {
            infoLayout.addView(TextView(this).apply {
                text = "Recorded: ${video.recordingDate.take(10)}"
                textSize = 11f
            })
        }

        sheet.addView(infoLayout)

        bottomSheet = sheet
        rootLayout.addView(sheet)

        // Swipe down to dismiss
        sheet.setOnTouchListener(object : View.OnTouchListener {
            private var startY = 0f
            override fun onTouch(v: View, event: android.view.MotionEvent): Boolean {
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        startY = event.rawY
                        return true
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        val dy = event.rawY - startY
                        if (dy > 0) {
                            sheet.translationY = dy
                        }
                        return true
                    }
                    android.view.MotionEvent.ACTION_UP -> {
                        val dy = event.rawY - startY
                        if (dy > sheetHeight * 0.3f) {
                            rootLayout.removeView(sheet)
                            bottomSheet = null
                        } else {
                            sheet.animate().translationY(0f).setDuration(200).start()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun openVideo(videoId: String) {
        try {
            val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
            startActivity(appIntent)
        } catch (e: Exception) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/watch?v=$videoId")
            )
            startActivity(webIntent)
        }
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
