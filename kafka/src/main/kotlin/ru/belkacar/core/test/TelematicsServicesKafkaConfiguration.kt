package ru.belkacar.core.test

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("telematics-services.kafka")
class TelematicsServicesKafkaConfiguration {
    lateinit var bootstrapServers: String
    lateinit var clientId: String
    lateinit var carPositionStream: String
    lateinit var unknownCarPositionsStream: String
    lateinit var telematicsPositionsStream: String
    lateinit var deviceRelationHistoryTable: String
    lateinit var autoOffsetReset: String
    lateinit var latestPositionEventsStream: String
    lateinit var geofenceEventsStream: String
}