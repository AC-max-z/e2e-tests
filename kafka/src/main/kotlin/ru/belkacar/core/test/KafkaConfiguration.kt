package ru.belkacar.core.test

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@EnableConfigurationProperties(BroadcastingPlatformKafkaConfiguration::class)
@Configuration
class KafkaConfiguration(
    private val broadcastingProperties: BroadcastingPlatformKafkaConfiguration
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
    fun kafkaProducer() = KafkaReactiveProducer<String, String>(
        broadcastingProperties.bootstrapServers
    )

}