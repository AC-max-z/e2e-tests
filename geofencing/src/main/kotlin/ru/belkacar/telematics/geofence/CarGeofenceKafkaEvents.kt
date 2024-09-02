package ru.belkacar.telematics.geofence

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import ru.belkacar.core.test.CarId
import java.time.Instant
import java.util.*

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "@type"
)
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(CarEnteredGeofence::class),
        JsonSubTypes.Type(CarLeavedGeofence::class)
    ]
)
interface CarGeofenceEventPayload {
    val carId: CarId<*>
    val geofence: GeofenceItem
}

data class CarGeofenceEventKafkaMessage(
    val id: UUID,
    val carId: CarId<*>,
    val timestamp: Instant,
    val payload: CarGeofenceEventPayload
)

data class CarEnteredGeofence(
    override val carId: CarId<*>,
    override val geofence: GeofenceItem
) : CarGeofenceEventPayload

data class CarLeavedGeofence(
    override val carId: CarId<*>,
    override val geofence: GeofenceItem
) : CarGeofenceEventPayload
