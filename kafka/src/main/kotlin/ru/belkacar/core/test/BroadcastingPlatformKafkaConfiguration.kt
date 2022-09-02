package ru.belkacar.core.test

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("broadcasting-platform.kafka")
class BroadcastingPlatformKafkaConfiguration {

    lateinit var bootstrapServers: String
    lateinit var clientId: String
    lateinit var carPositionStream: String
    lateinit var unknownCarPositionsStream: String
    lateinit var telematicsPositionsStream: String
    lateinit var deviceRelationHistoryTable: String
}
