package ru.belkacar.telematics.broadcasting.platform

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kafka.sender.SenderRecord
import reactor.kafka.sender.SenderResult
import ru.belkacar.core.test.CarId
import ru.belkacar.core.test.CarPositionEvent
import ru.belkacar.core.test.KafkaConsumerBuilder
import ru.belkacar.core.test.KafkaProducerBuilder
import ru.belkacar.core.test.PositionEvent
import ru.belkacar.core.test.TelematicsPlatformProperties
import java.util.UUID

@Component
class BroadcastingPlatformKafkaOperations(
    private val kafkaConsumerBuilder: KafkaConsumerBuilder,
    private val kafkaProducerBuilder: KafkaProducerBuilder,
    private val telematicsPlatformProperties: TelematicsPlatformProperties
) {
    
    private val _objectMapper = ObjectMapper().apply {
        findAndRegisterModules()
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    val producerOps by lazy { ProducerOps() }
    
    class ConsumerOps {
    
    }
    
    inner class ProducerOps {
        
        private val producer: KafkaSender<String, String> = kafkaProducerBuilder.create()
        
        fun produceCarPosition(carPositionEvent: CarPositionEvent): Flux<SenderResult<String>> {
            return Mono.just(carPositionEvent)
                .handle<SenderRecord<String, String, String>> { carPosition, sink ->
                    val key = carPositionEvent.carId.value.toString()
                    val value = _objectMapper.writeValueAsString(carPosition)
                    
                    sink.next(
                        SenderRecord.create(
                            ProducerRecord(
                                telematicsPlatformProperties.broadcastingPlatform.carPositionsTopic,
                                key,
                                value
                            ),
                            key
                        )
                    )
                }
                .flatMapMany { producer.send(Mono.just(it)) }
        }
        
        fun producerLatestCarPosition(carPositionEvent: CarPositionEvent): Flux<SenderResult<String>> {
            return Mono.just(carPositionEvent)
                .handle<SenderRecord<String, String, String>> { carPosition, sink ->
                    val key = carPositionEvent.carId.value.toString()
                    val value = _objectMapper.writeValueAsString(carPosition)
            
                    sink.next(
                        SenderRecord.create(
                            ProducerRecord(
                                telematicsPlatformProperties.broadcastingPlatform.latestCarPositionsTopic,
                                key,
                                value
                            ),
                            key
                        )
                    )
                }
                .flatMapMany { producer.send(Mono.just(it)) }
        }
        
        fun producePosition(positionEvent: PositionEvent) {
        
        }
    }
}