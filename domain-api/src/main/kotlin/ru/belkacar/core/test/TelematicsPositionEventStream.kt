package ru.belkacar.core.test

data class TelematicsPositionEventStream(
    val deviceId: Int,
    val deviceImei: String,
    val deviceSensors: List<Any>,
    val fixTime: String,
    val id: Int,
    val navigation: Navigation,
    val serverTime: String,
    val vehicleSensors: List<VehicleSensor>
)