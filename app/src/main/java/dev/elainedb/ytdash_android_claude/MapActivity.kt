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
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import dev.elainedb.ytdash_android_claude.database.VideoDatabase
import dev.elainedb.ytdash_android_claude.database.toVideo
import dev.elainedb.ytdash_android_claude.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapActivity : AppCompatActivity() {

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MapActivity::class.java)
        }
    }

    private lateinit var mapView: MapView
    private lateinit var bottomSheet: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = packageName

        val root = FrameLayout(this).apply {
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
        root.addView(mapView)

        val maxHeight = (resources.displayMetrics.heightPixels * 0.25).toInt()
        bottomSheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFFFFFF.toInt())
            elevation = 16f
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                maxHeight
            ).apply {
                gravity = Gravity.BOTTOM
            }
            setPadding(32, 16, 32, 16)
            visibility = View.GONE
        }
        root.addView(bottomSheet)

        setContentView(root)

        loadVideosOnMap()
    }

    private fun showBottomSheet(video: Video) {
        bottomSheet.removeAllViews()

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val thumbnail = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                300
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        thumbnail.load(video.thumbnailUrl)
        content.addView(thumbnail)

        content.addView(TextView(this).apply {
            text = video.title
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 16, 0, 4)
        })

        content.addView(TextView(this).apply {
            text = video.channelName
            textSize = 14f
        })

        content.addView(TextView(this).apply {
            text = video.publishedAt.take(10)
            textSize = 12f
        })

        if (video.tags.isNotEmpty()) {
            content.addView(TextView(this).apply {
                text = "Tags: ${video.tags.joinToString(", ")}"
                textSize = 12f
                setPadding(0, 8, 0, 0)
            })
        }

        if (video.locationCity != null || video.locationCountry != null) {
            val locationParts = listOfNotNull(video.locationCity, video.locationCountry)
            content.addView(TextView(this).apply {
                text = locationParts.joinToString(", ")
                textSize = 12f
            })
            if (video.locationLatitude != null && video.locationLongitude != null) {
                content.addView(TextView(this).apply {
                    text = "(${video.locationLatitude}, ${video.locationLongitude})"
                    textSize = 10f
                })
            }
        }

        if (video.recordingDate != null) {
            content.addView(TextView(this).apply {
                text = "Recorded: ${video.recordingDate.take(10)}"
                textSize = 12f
            })
        }

        scrollView.addView(content)
        bottomSheet.addView(scrollView)

        bottomSheet.setOnClickListener {
            openVideo(video.id)
        }

        bottomSheet.visibility = View.VISIBLE
    }

    private fun openVideo(videoId: String) {
        try {
            val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
            startActivity(appIntent)
        } catch (_: Exception) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/watch?v=$videoId")
            )
            startActivity(webIntent)
        }
    }

    private fun loadVideosOnMap() {
        val videoDao = VideoDatabase.getDatabase(this).videoDao()

        lifecycleScope.launch {
            val entities = withContext(Dispatchers.IO) {
                videoDao.getVideosWithLocation()
            }
            val videos = entities.map { it.toVideo() }

            if (videos.isEmpty()) return@launch

            val geoPoints = mutableListOf<GeoPoint>()

            videos.forEach { video ->
                val lat = video.locationLatitude ?: return@forEach
                val lon = video.locationLongitude ?: return@forEach
                val geoPoint = GeoPoint(lat, lon)
                geoPoints.add(geoPoint)

                val marker = Marker(mapView).apply {
                    position = geoPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = video.title
                    setOnMarkerClickListener { _, _ ->
                        showBottomSheet(video)
                        true
                    }
                }
                mapView.overlays.add(marker)
            }

            if (geoPoints.isNotEmpty()) {
                val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                mapView.zoomToBoundingBox(boundingBox.increaseByScale(1.3f), true)
            }

            mapView.invalidate()
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
