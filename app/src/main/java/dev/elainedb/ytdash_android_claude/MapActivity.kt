package dev.elainedb.ytdash_android_claude

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import coil.load
import dev.elainedb.ytdash_android_claude.database.VideoDatabase
import dev.elainedb.ytdash_android_claude.database.toVideo
import dev.elainedb.ytdash_android_claude.model.Video
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.google.android.material.bottomsheet.BottomSheetBehavior

class MapActivity : AppCompatActivity() {

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MapActivity::class.java)
        }
    }

    private lateinit var mapView: MapView
    private lateinit var bottomSheet: LinearLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContentView(R.layout.activity_map)

        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        bottomSheet = findViewById(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        loadVideosOnMap()
    }

    private fun loadVideosOnMap() {
        CoroutineScope(Dispatchers.IO).launch {
            val videoDao = VideoDatabase.getDatabase(this@MapActivity).videoDao()
            val videos = videoDao.getVideosWithLocation().map { it.toVideo() }

            withContext(Dispatchers.Main) {
                if (videos.isEmpty()) return@withContext

                val geoPoints = mutableListOf<GeoPoint>()

                videos.forEach { video ->
                    val lat = video.locationLatitude ?: return@forEach
                    val lng = video.locationLongitude ?: return@forEach
                    val geoPoint = GeoPoint(lat, lng)
                    geoPoints.add(geoPoint)

                    val marker = Marker(mapView)
                    marker.position = geoPoint
                    marker.title = video.title
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.setOnMarkerClickListener { _, _ ->
                        showBottomSheet(video)
                        true
                    }
                    mapView.overlays.add(marker)
                }

                // Fit map to show all markers
                if (geoPoints.isNotEmpty()) {
                    val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                    mapView.zoomToBoundingBox(boundingBox.increaseByScale(1.3f), true)
                }

                mapView.invalidate()
            }
        }
    }

    private fun showBottomSheet(video: Video) {
        findViewById<ImageView>(R.id.sheetThumbnail).load(video.thumbnailUrl)
        findViewById<TextView>(R.id.sheetTitle).text = video.title
        findViewById<TextView>(R.id.sheetChannel).text = video.channelName
        findViewById<TextView>(R.id.sheetDate).text = video.publishedAt.take(10)

        val tagsText = if (video.tags.isNotEmpty()) video.tags.joinToString(", ") else ""
        val tagsView = findViewById<TextView>(R.id.sheetTags)
        tagsView.text = tagsText
        tagsView.visibility = if (tagsText.isNotEmpty()) View.VISIBLE else View.GONE

        val locationText = listOfNotNull(video.locationCity, video.locationCountry).joinToString(", ")
        val locationView = findViewById<TextView>(R.id.sheetLocation)
        locationView.text = locationText
        locationView.visibility = if (locationText.isNotEmpty()) View.VISIBLE else View.GONE

        if (video.locationLatitude != null && video.locationLongitude != null) {
            val coordsView = findViewById<TextView>(R.id.sheetCoords)
            coordsView.text = "(${video.locationLatitude}, ${video.locationLongitude})"
            coordsView.visibility = View.VISIBLE
        } else {
            findViewById<TextView>(R.id.sheetCoords).visibility = View.GONE
        }

        val recordingView = findViewById<TextView>(R.id.sheetRecordingDate)
        if (video.recordingDate != null) {
            recordingView.text = "Recorded: ${video.recordingDate.take(10)}"
            recordingView.visibility = View.VISIBLE
        } else {
            recordingView.visibility = View.GONE
        }

        bottomSheet.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:${video.id}")))
            } catch (_: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.id}")))
            }
        }

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
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
