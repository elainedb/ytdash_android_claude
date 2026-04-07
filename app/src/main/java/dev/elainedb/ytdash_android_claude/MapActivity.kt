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
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import dev.elainedb.ytdash_android_claude.video.data.local.VideoDao
import dev.elainedb.ytdash_android_claude.video.data.local.toVideo
import dev.elainedb.ytdash_android_claude.video.domain.model.Video
import kotlinx.coroutines.flow.collectLatest
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
    private lateinit var bottomSheet: LinearLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MapActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = "dev.elainedb.ytdash_android_claude/1.0"
        Configuration.getInstance().osmdroidTileCache = cacheDir

        setContentView(R.layout.activity_map)

        mapView = findViewById(R.id.mapView)
        bottomSheet = findViewById(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(3.0)

        loadVideosOnMap()
    }

    private fun loadVideosOnMap() {
        lifecycleScope.launch {
            videoDao.getVideosWithLocation().collectLatest { entities ->
                val videos = entities.map { it.toVideo() }
                mapView.overlays.clear()

                if (videos.isEmpty()) return@collectLatest

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

                if (geoPoints.size > 1) {
                    val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                    mapView.post {
                        mapView.zoomToBoundingBox(boundingBox, true, 100)
                    }
                } else if (geoPoints.size == 1) {
                    mapView.controller.setCenter(geoPoints[0])
                    mapView.controller.setZoom(10.0)
                }

                mapView.invalidate()
            }
        }
    }

    private fun showBottomSheet(video: Video) {
        val thumbnail = findViewById<ImageView>(R.id.bsThumbnail)
        val title = findViewById<TextView>(R.id.bsTitle)
        val channel = findViewById<TextView>(R.id.bsChannel)
        val date = findViewById<TextView>(R.id.bsDate)
        val tags = findViewById<TextView>(R.id.bsTags)
        val location = findViewById<TextView>(R.id.bsLocation)
        val recordingDate = findViewById<TextView>(R.id.bsRecordingDate)

        thumbnail.load(video.thumbnailUrl)
        title.text = video.title
        channel.text = video.channelName
        date.text = video.publishedAt.take(10)
        tags.text = if (video.tags.isNotEmpty()) video.tags.joinToString(", ") else ""
        tags.visibility = if (video.tags.isNotEmpty()) View.VISIBLE else View.GONE

        val locationParts = listOfNotNull(video.locationCity, video.locationCountry)
        val coordsText = if (video.locationLatitude != null && video.locationLongitude != null) {
            " (${String.format("%.4f", video.locationLatitude)}, ${String.format("%.4f", video.locationLongitude)})"
        } else ""
        location.text = locationParts.joinToString(", ") + coordsText

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
