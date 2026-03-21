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
                suspendCancellableCoroutine { continuation ->
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
                    val country = address.countryName
                    Pair(city, country)
                } else {
                    Pair(null, null)
                }
            }
        } catch (_: Exception) {
            Pair(null, null)
        }
    }
}
