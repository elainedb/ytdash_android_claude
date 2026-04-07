package dev.elainedb.ytdash_android_claude

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import dev.elainedb.ytdash_android_claude.data.local.VideoDao
import dev.elainedb.ytdash_android_claude.data.local.toVideo
import dev.elainedb.ytdash_android_claude.domain.model.Video
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

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MapActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure osmdroid
        Configuration.getInstance().userAgentValue = "dev.elainedb.ytdash_android_claude/1.0"
        Configuration.getInstance().osmdroidTileCache = cacheDir

        setContentView(R.layout.activity_map)
        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

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
                        showVideoBottomSheet(video)
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

    private fun showVideoBottomSheet(video: Video) {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_video, null)

        val thumbnail = view.findViewById<ImageView>(R.id.videoThumbnail)
        val title = view.findViewById<TextView>(R.id.videoTitle)
        val channel = view.findViewById<TextView>(R.id.videoChannel)
        val publishDate = view.findViewById<TextView>(R.id.videoPublishDate)
        val tags = view.findViewById<TextView>(R.id.videoTags)
        val location = view.findViewById<TextView>(R.id.videoLocation)
        val recordingDate = view.findViewById<TextView>(R.id.videoRecordingDate)

        thumbnail.load(video.thumbnailUrl)
        title.text = video.title
        channel.text = video.channelName
        publishDate.text = video.publishedAt.take(10)

        if (video.tags.isNotEmpty()) {
            tags.text = video.tags.joinToString(", ")
        } else {
            tags.visibility = android.view.View.GONE
        }

        val locationParts = mutableListOf<String>()
        if (video.locationCity != null) locationParts.add(video.locationCity)
        if (video.locationCountry != null) locationParts.add(video.locationCountry)
        if (video.locationLatitude != null && video.locationLongitude != null) {
            locationParts.add("(${video.locationLatitude}, ${video.locationLongitude})")
        }
        location.text = locationParts.joinToString(", ")

        if (video.recordingDate != null) {
            recordingDate.text = "Recorded: ${video.recordingDate.take(10)}"
        } else {
            recordingDate.visibility = android.view.View.GONE
        }

        view.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:${video.id}")))
            } catch (_: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.id}")))
            }
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.25).toInt()
        dialog.show()
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
