package ru.belkacar.core.test

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@EnableConfigurationProperties(
    TelematicsServicesKafkaConfiguration::class,
    BroadcastingPlatformKafkaConfiguration::class
)
@Configuration
class KafkaConfiguration(
    private val broadcastingProperties: BroadcastingPlatformKafkaConfiguration,
    private val telematicsServicesKafkaConfiguration: TelematicsServicesKafkaConfiguration
) {

    @Bean
    fun telematicsPositionStreamConsumer() = KafkaReactiveConsumer<String, String>(
        broadcastingProperties.bootstrapServers,
        broadcastingProperties.telematicsPositionsStream,
        broadcastingProperties.clientId,
        broadcastingProperties.autoOffsetReset
    )

    @Bean
    fun carPositionStreamConsumer() = KafkaReactiveConsumer<String, String>(
        broadcastingProperties.bootstrapServers,
        broadcastingProperties.carPositionStream,
        broadcastingProperties.clientId,
        broadcastingProperties.autoOffsetReset
    )

    @Bean
    fun unknownCarPositionStreamConsumer() = KafkaReactiveConsumer<String, String>(
        broadcastingProperties.bootstrapServers,
        broadcastingProperties.unknownCarPositionsStream,
        broadcastingProperties.clientId,
        broadcastingProperties.autoOffsetReset
    )

    @Bean
    fun latestCarPositionStreamConsumer() = KafkaReactiveConsumer<String, String>(
        telematicsServicesKafkaConfiguration.bootstrapServers,
        telematicsServicesKafkaConfiguration.latestPositionEventsStream,
        telematicsServicesKafkaConfiguration.clientId,
        telematicsServicesKafkaConfiguration.autoOffsetReset
    )

    @Bean
    fun geofenceEventStreamConsumer() = KafkaReactiveConsumer<String, String>(
        telematicsServicesKafkaConfiguration.bootstrapServers,
        telematicsServicesKafkaConfiguration.geofenceEventsStream,
        telematicsServicesKafkaConfiguration.clientId,
        telematicsServicesKafkaConfiguration.autoOffsetReset
    )

    @Bean
    fun kafkaProducer() = KafkaReactiveProducer<String, String>(
        broadcastingProperties.bootstrapServers
    )

}