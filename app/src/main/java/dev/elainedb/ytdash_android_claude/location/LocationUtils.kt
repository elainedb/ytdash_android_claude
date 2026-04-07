package dev.elainedb.ytdash_android_claude.location

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt

object LocationUtils {

    private val cache = ConcurrentHashMap<Pair<Int, Int>, Pair<String?, String?>>()
    private val semaphore = Semaphore(5)
    private var lastNominatimRequest = 0L
    private val json = Json { ignoreUnknownKeys = true }

    private fun roundKey(lat: Double, lon: Double): Pair<Int, Int> {
        return Pair(
            (lat * 1000).roundToInt(),
            (lon * 1000).roundToInt()
        )
    }

    suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Pair<String?, String?> {
        val key = roundKey(latitude, longitude)
        cache[key]?.let { return it }

        return semaphore.withPermit {
            cache[key]?.let { return@withPermit it }

            val result = tryGeocoderWithRetry(context, latitude, longitude)
                ?: tryNominatim(latitude, longitude)
                ?: Pair(null, null)

            cache[key] = result
            result
        }
    }

    private suspend fun tryGeocoderWithRetry(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Pair<String?, String?>? {
        if (!Geocoder.isPresent()) return null

        repeat(3) { attempt ->
            try {
                val result = geocode(context, latitude, longitude)
                if (result != null) return result
            } catch (_: Exception) {
                // Retry
            }
            if (attempt < 2) {
                delay(500L * (1 shl attempt))
            }
        }
        return null
    }

    private suspend fun geocode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Pair<String?, String?>? = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCoroutine { cont ->
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        val address = addresses[0]
                        val city = address.locality
                            ?: address.subAdminArea
                            ?: address.adminArea
                            ?: address.subLocality
                            ?: address.thoroughfare
                        cont.resume(Pair(city, address.countryName))
                    } else {
                        cont.resume(null)
                    }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val city = address.locality
                    ?: address.subAdminArea
                    ?: address.adminArea
                    ?: address.subLocality
                    ?: address.thoroughfare
                Pair(city, address.countryName)
            } else {
                null
            }
        }
    }

    private suspend fun tryNominatim(
        latitude: Double,
        longitude: Double
    ): Pair<String?, String?>? = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val timeSinceLast = now - lastNominatimRequest
            if (timeSinceLast < 1000) {
                delay(1000 - timeSinceLast)
            }
            lastNominatimRequest = System.currentTimeMillis()

            val url = URL(
                "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude"
            )
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "dev.elainedb.ytdash_android_claude/1.0")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val response = connection.inputStream.bufferedReader().readText()
            val nominatimResponse = json.decodeFromString<NominatimResponse>(response)

            val city = nominatimResponse.address?.city
                ?: nominatimResponse.address?.town
                ?: nominatimResponse.address?.village
            val country = nominatimResponse.address?.country

            if (city != null || country != null) {
                Pair(city, country)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    @Serializable
    private data class NominatimResponse(
        val address: NominatimAddress? = null
    )

    @Serializable
    private data class NominatimAddress(
        val city: String? = null,
        val town: String? = null,
        val village: String? = null,
        val country: String? = null
    )
}
