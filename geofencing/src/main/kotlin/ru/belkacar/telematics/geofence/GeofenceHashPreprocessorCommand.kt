package ru.belkacar.telematics.geofence

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "@type"
)
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(`Command$AddGeofence`::class),
        JsonSubTypes.Type(`Command$UpdateGeofence`::class),
        JsonSubTypes.Type(`Command$RemoveGeofence`::class)
    ]
)
interface GeofenceHashPreprocessorCommand {
    val geofence: Geofence?
    val geofenceId: GeofenceId?
}

data class `Command$AddGeofence`(
    override val geofence: Geofence,
    override val geofenceId: GeofenceId?
) : GeofenceHashPreprocessorCommand

data class `Command$UpdateGeofence`(
    override val geofence: Geofence,
    override val geofenceId: GeofenceId?
) : GeofenceHashPreprocessorCommand

data class `Command$RemoveGeofence`(
    override val geofenceId: GeofenceId,
    override val geofence: Geofence?
) : GeofenceHashPreprocessorCommand