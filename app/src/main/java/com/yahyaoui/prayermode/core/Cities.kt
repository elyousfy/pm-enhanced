package com.yahyaoui.prayermode.core

/**
 * UAE presets: coordinates for Aladhan, Awqaf official_id for the official source
 * (ids from Awqaf's /location/emirates list).
 */
object UaeCities {
    val all: List<LocationConfig> = listOf(
        uae("Abu Dhabi", 24.4539, 54.3773, 36),
        uae("Al Ain", 24.2075, 55.7447, 37),
        uae("Dubai", 25.2048, 55.2708, 69),
        uae("Sharjah", 25.3463, 55.4209, 91),
        uae("Ajman", 25.4052, 55.5136, 66),
        uae("Umm Al Quwain", 25.5647, 55.5552, 94),
        uae("Ras Al Khaimah", 25.8007, 55.9762, 82),
        uae("Fujairah", 25.1288, 56.3265, 73),
        uae("Khor Fakkan", 25.3390, 56.3560, 88),
        uae("Hatta", 24.7960, 56.1170, 71),
        uae("Madinat Zayed", 23.6840, 53.7050, 57),
        uae("Ruwais", 24.1100, 52.7300, 61),
    )

    val default: LocationConfig = all.first()

    private fun uae(name: String, lat: Double, lng: Double, awqaf: Int) =
        LocationConfig("AE", name, lat, lng, "Asia/Dubai", awqaf)
}
