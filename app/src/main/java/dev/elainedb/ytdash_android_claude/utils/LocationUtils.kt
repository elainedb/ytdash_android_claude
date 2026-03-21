package dev.elainedb.ytdash_android_claude.utils

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object LocationUtils {
    private const val TAG = "LocationUtils"

    suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Pair<String?, String?> {
        if (!Geocoder.isPresent()) {
            return Pair(null, null)
        }

        return try {
            val geocoder = Geocoder(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                reverseGeocodeAsync(geocoder, latitude, longitude)
            } else {
                reverseGeocodeSync(geocoder, latitude, longitude)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reverse geocoding failed", e)
            Pair(null, null)
        }
    }

    @Suppress("NewApi")
    private suspend fun reverseGeocodeAsync(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double
    ): Pair<String?, String?> = suspendCoroutine { continuation ->
        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
            if (addresses.isNotEmpty()) {
                val address = addresses[0]
                val city = address.locality
                    ?: address.subAdminArea
                    ?: address.adminArea
                    ?: address.subLocality
                    ?: address.thoroughfare
                val country = address.countryName
                continuation.resume(Pair(city, country))
            } else {
                continuation.resume(Pair(null, null))
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocodeSync(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double
    ): Pair<String?, String?> {
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        return if (addresses != null && addresses.isNotEmpty()) {
            val address = addresses[0]
            val city = address.locality
                ?: address.subAdminArea
                ?: address.adminArea
                ?: address.subLocality
                ?: address.thoroughfare
            val country = address.countryName
            Pair(city, country)
        } else {
            Pair(null, null)
        }
    }
}
