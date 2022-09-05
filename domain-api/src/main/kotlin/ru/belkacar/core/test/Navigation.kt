package ru.belkacar.core.test

data class Navigation(
    val altitude: Double,
    val course: Double,
    val glnSatellites: Any,
    val gpsSatellites: Int,
    val hdop: Double,
    val latitude: Double,
    val longitude: Double,
    val valid: Boolean
)