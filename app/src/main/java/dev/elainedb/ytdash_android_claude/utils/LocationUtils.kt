package dev.elainedb.ytdash_android_claude.utils

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

    private fun roundCoordinate(value: Double): Double {
        return round(value * 1000) / 1000
    }

    private fun getCacheKey(lat: Double, lon: Double): Pair<Double, Double> {
        return Pair(roundCoordinate(lat), roundCoordinate(lon))
    }

    suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Pair<String?, String?> {
        val key = getCacheKey(latitude, longitude)
        cache[key]?.let { return it }

        return semaphore.withPermit {
            cache[key]?.let { return@withPermit it }

            val result = tryAndroidGeocoder(context, latitude, longitude)
                ?: tryNominatim(latitude, longitude)
                ?: Pair(null, null)

            cache[key] = result
            result
        }
    }

    private suspend fun tryAndroidGeocoder(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Pair<String?, String?>? {
        if (!Geocoder.isPresent()) return null

        val geocoder = Geocoder(context, Locale.getDefault())

        return withContext(Dispatchers.IO) {
            var lastException: Exception? = null
            repeat(3) { attempt ->
                try {
                    val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
                    if (result != null) return@withContext result
                } catch (e: Exception) {
                    lastException = e
                    val backoff = 500L * (1 shl attempt)
                    delay(backoff)
                }
            }
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

    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun tryNominatim(
        latitude: Double,
        longitude: Double
    ): Pair<String?, String?>? {
        return withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val timeSinceLast = now - lastNominatimRequest
                if (timeSinceLast < 1000) {
                    delay(1000 - timeSinceLast)
                }

                val url = URL(
                    "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude"
                )
                val connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "dev.elainedb.ytdash_android_claude/1.0")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                lastNominatimRequest = System.currentTimeMillis()

                val responseText = connection.inputStream.bufferedReader().readText()
                connection.disconnect()

                val response = json.decodeFromString<NominatimResponse>(responseText)
                val city = response.address?.city
                    ?: response.address?.town
                    ?: response.address?.village
                Pair(city, response.address?.country)
            } catch (_: Exception) {
                null
            }
        }
    }
}
