package dev.elainedb.ytdash_android_claude.utils

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

object LocationUtils {

    suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Pair<String?, String?> {
        if (!Geocoder.isPresent()) return Pair(null, null)

        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                reverseGeocodeAsync(geocoder, latitude, longitude)
            } else {
                reverseGeocodeSync(geocoder, latitude, longitude)
            }
        } catch (_: Exception) {
            Pair(null, null)
        }
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocodeSync(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double
    ): Pair<String?, String?> {
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        if (addresses.isNullOrEmpty()) return Pair(null, null)
        val address = addresses[0]
        val city = address.locality
            ?: address.subAdminArea
            ?: address.adminArea
            ?: address.subLocality
            ?: address.thoroughfare
        return Pair(city, address.countryName)
    }

    private suspend fun reverseGeocodeAsync(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double
    ): Pair<String?, String?> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return Pair(null, null)
        }
        return suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                if (addresses.isEmpty()) {
                    continuation.resume(Pair(null, null))
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
}
