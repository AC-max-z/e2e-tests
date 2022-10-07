package ru.belkacar.core.test

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "telematics")
@ConstructorBinding
data class TelematicsPlatformProperties(
    val communication: Communication,
    val broadcastingPlatform: BroadcastingPlatform,
    val geofenceManager: Geofences
) {
    
    data class Communication(val grpc: Grpc) {
        data class Grpc(val host: String, val port: Int)
    }
    
    data class BroadcastingPlatform(
        val carPositionsTopic: String,
        val unknownCarPositionsTopic: String,
        val latestCarPositionsTopic: String,
        val deviceRelationHistoryTable: String
    )
    
    data class Geofences(
        val geofenceEventsTopic: String,
        val geohashCommandsTopic: String,
        val geohashTableTopic: String,
        val carGeofenceEventsTopic: String
    )
}