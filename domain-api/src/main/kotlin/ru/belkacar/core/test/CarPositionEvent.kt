package ru.belkacar.core.test


data class CarPositionEvent(
    val carId: CarId,
    val positionEvent: PositionEvent
)

data class PositionEvent(
    val id: Int,
    val deviceId: Int,
    val deviceImei: String,
    val timestamp: Long,
    val navigation: Navigation,
    val fixTime: String,
    val serverTime: String,
    val vehicleSensors: List<VehicleSensor>,
    val deviceSensors: List<DeviceSensor>
)