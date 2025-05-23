package ru.belkacar.telematics.geofence

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.reactivestreams.Subscription
import org.springframework.stereotype.Component
import reactor.core.CoreSubscriber
import reactor.core.publisher.Flux
import reactor.kafka.receiver.ReceiverRecord
import ru.belkacar.core.test.KafkaConsumerBuilder
import ru.belkacar.core.test.TelematicsPlatformProperties
import kotlin.math.sin

@Component
class GeofencesKafkaOperations(
    private val kafkaConsumerBuilder: KafkaConsumerBuilder,
    private val telematicsPlatformProperties: TelematicsPlatformProperties
) {
    
    val consumerOps by lazy { ConsumerOps() }
    
    
    inner class ConsumerOps {
        
        fun consumeGeofenceEvents() {
        
        }
        
        fun consumeCarGeofenceEvents(): Flux<ConsumerRecord<String, CarGeofenceEventKafkaMessage>> {
            return kafkaConsumerBuilder.create<CarGeofenceEventKafkaMessage>()
                .withValueType(CarGeofenceEventKafkaMessage::class.java)
                .withOffsetResetPolicy(KafkaConsumerBuilder.OffsetResetPolicy.earliest)
                .withTopic(telematicsPlatformProperties.geofenceManager.carGeofenceEventsTopic)
                .build()
                .receiveAutoAck()
                .concatMap { it }
        }

        fun consumeGeofenceDetectorState(): Flux<ConsumerRecord<String, GeofenceDetectorKafkaState>> {
            return kafkaConsumerBuilder.create<GeofenceDetectorKafkaState>()
                .withValueType(GeofenceDetectorKafkaState::class.java)
                .withOffsetResetPolicy(KafkaConsumerBuilder.OffsetResetPolicy.earliest)
                .withTopic(telematicsPlatformProperties.geofenceManager.geofencesDetectorStateTable)
                .build()
                .receiveAutoAck()
                .concatMap { it }
        }

        fun consumeGeohashProcessorCommands(): Flux<ConsumerRecord<String, GeofenceHashPreprocessorCommand>> {
            return kafkaConsumerBuilder.create<GeofenceHashPreprocessorCommand>()
                .withValueType(GeofenceHashPreprocessorCommand::class.java)
                .withOffsetResetPolicy(KafkaConsumerBuilder.OffsetResetPolicy.earliest)
                .withTopic(telematicsPlatformProperties.geofenceManager.geohashPreprocessorCommands)
                .build()
                .receiveAutoAck()
                .concatMap { it }
        }
    }
    
    
    class ProducerOps {
    
    }
}

