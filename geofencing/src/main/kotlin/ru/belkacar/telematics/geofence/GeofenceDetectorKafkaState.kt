package ru.belkacar.telematics.geofence

import ru.belkacar.core.test.CarId
import java.time.Instant

data class GeofenceDetectorKafkaState(
    val carId: CarId<*>,
    val timestamp: Instant,
    val lastPointGeofences: List<GeofenceItem>,
    val currentPoinGeofences: List<GeofenceItem>
)