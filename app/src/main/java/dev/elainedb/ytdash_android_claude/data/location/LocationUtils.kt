package dev.elainedb.ytdash_android_claude.data.location

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
import kotlin.math.round

object LocationUtils {

    private val cache = ConcurrentHashMap<Pair<Double, Double>, Pair<String?, String?>>()
    private val semaphore = Semaphore(5)
    private var lastNominatimRequest = 0L
    private val json = Json { ignoreUnknownKeys = true }

    private fun roundCoordinate(value: Double): Double {
        return round(value * 1000) / 1000
    }

    suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Pair<String?, String?> {
        val key = Pair(roundCoordinate(latitude), roundCoordinate(longitude))

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

        val geocoder = Geocoder(context, Locale.getDefault())
        var lastException: Exception? = null

        repeat(3) { attempt ->
            try {
                val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocodeAsync(geocoder, latitude, longitude)
                } else {
                    geocodeSync(geocoder, latitude, longitude)
                }
                if (result != null) return result
            } catch (e: Exception) {
                lastException = e
                val backoff = 500L * (1 shl attempt)
                delay(backoff)
            }
        }

        return null
    }

    @Suppress("DEPRECATION")
    private fun geocodeSync(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double
    ): Pair<String?, String?>? {
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        if (addresses.isNullOrEmpty()) return null
        val address = addresses[0]
        val city = address.locality
            ?: address.subAdminArea
            ?: address.adminArea
            ?: address.subLocality
            ?: address.thoroughfare
        return Pair(city, address.countryName)
    }

    private suspend fun geocodeAsync(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double
    ): Pair<String?, String?>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null

        return suspendCoroutine { continuation ->
            geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                if (addresses.isEmpty()) {
                    continuation.resume(null)
                } else {
                    val address = addresses[0]
                    val city = address.locality
                        ?: address.subAdminArea
                        ?: address.adminArea
                        ?: address.subLocality
                        ?: address.thoroughfare
                    continuation.resume(Pair(city, address.countryName))
                }
            }
        }
    }

    private suspend fun tryNominatim(
        latitude: Double,
        longitude: Double
    ): Pair<String?, String?>? = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val timeSinceLastRequest = now - lastNominatimRequest
            if (timeSinceLastRequest < 1000) {
                delay(1000 - timeSinceLastRequest)
            }
            lastNominatimRequest = System.currentTimeMillis()

            val url = URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude")
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

            if (city != null || country != null) Pair(city, country) else null
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
