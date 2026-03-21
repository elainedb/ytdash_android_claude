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
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
        bottomSheet = findViewById(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        loadVideosOnMap()
    }

    private fun loadVideosOnMap() {
        val videoDao = VideoDatabase.getDatabase(this).videoDao()

        CoroutineScope(Dispatchers.IO).launch {
            val entities = videoDao.getVideosWithLocation()
            val videos = entities.map { it.toVideo() }

            withContext(Dispatchers.Main) {
                if (videos.isEmpty()) return@withContext

                val geoPoints = mutableListOf<GeoPoint>()

                videos.forEach { video ->
                    val lat = video.locationLatitude ?: return@forEach
                    val lon = video.locationLongitude ?: return@forEach
                    val geoPoint = GeoPoint(lat, lon)
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

                if (geoPoints.isNotEmpty()) {
                    val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                    mapView.post {
                        mapView.zoomToBoundingBox(boundingBox, true, 100)
                    }
                }

                mapView.invalidate()
            }
        }
    }

    private fun showBottomSheet(video: Video) {
        val thumbnail: ImageView = findViewById(R.id.bottomSheetThumbnail)
        val title: TextView = findViewById(R.id.bottomSheetTitle)
        val channel: TextView = findViewById(R.id.bottomSheetChannel)
        val publishedAt: TextView = findViewById(R.id.bottomSheetPublishedAt)
        val tags: TextView = findViewById(R.id.bottomSheetTags)
        val location: TextView = findViewById(R.id.bottomSheetLocation)
        val recordingDate: TextView = findViewById(R.id.bottomSheetRecordingDate)

        thumbnail.load(video.thumbnailUrl)
        title.text = video.title
        channel.text = video.channelName
        publishedAt.text = video.publishedAt.take(10)

        if (video.tags.isNotEmpty()) {
            tags.text = video.tags.joinToString(", ")
            tags.visibility = View.VISIBLE
        } else {
            tags.visibility = View.GONE
        }

        val locationParts = mutableListOf<String>()
        if (video.locationCity != null) locationParts.add(video.locationCity)
        if (video.locationCountry != null) locationParts.add(video.locationCountry)
        if (video.locationLatitude != null && video.locationLongitude != null) {
            locationParts.add("GPS: %.4f, %.4f".format(video.locationLatitude, video.locationLongitude))
        }
        if (locationParts.isNotEmpty()) {
            location.text = locationParts.joinToString(" | ")
            location.visibility = View.VISIBLE
        } else {
            location.visibility = View.GONE
        }

        if (video.recordingDate != null) {
            recordingDate.text = "Recorded: ${video.recordingDate.take(10)}"
            recordingDate.visibility = View.VISIBLE
        } else {
            recordingDate.visibility = View.GONE
        }

        bottomSheet.setOnClickListener {
            openVideo(video.id)
        }

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
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

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
