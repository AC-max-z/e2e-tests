package ru.belkacar.core.test

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.IntegerSerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.common.serialization.UUIDSerializer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kafka.sender.SenderRecord
import reactor.kafka.sender.SenderResult
import java.util.*
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType


@Component
class KafkaReactiveProducer<K, V>(
    private val kafkaConfiguration: BroadcastingPlatformKafkaConfiguration
) {

    companion object {
        private val logger = LoggerFactory.getLogger(KafkaReactiveProducer::class.java)
    }

    private val sender: KafkaSender<UUID, String>

    init {
        val producerProps = Properties()
        producerProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaConfiguration.bootstrapServers
        producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = UUIDSerializer::class.java
        producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        val senderOptions = SenderOptions.create<UUID, String>(producerProps) //.maxInFlight(1024)

        sender = KafkaSender.create(senderOptions)
    }

    fun consume(topic: String, key: UUID, value: Any): SenderResult<String>? {

        val valueAsString : String = ObjectMapper().writeValueAsString(Car(UUID.randomUUID(), "skoda", 98))

        val producerRecord = ProducerRecord(kafkaConfiguration.carPositionStream, key, valueAsString)

        return send(Mono
            .just(SenderRecord.create(producerRecord, key.toString())))
            .blockLast()
    }

    private fun send(outboundFlux: Mono<SenderRecord<UUID, String, String>>): Flux<SenderResult<String>> {
        return sender.send(outboundFlux)
            .doOnError { e -> logger.error("Send failed", e) }
            .doOnNext { r ->
                logger.info(
                    "Message Key ${r.correlationMetadata()} send response: ${
                        r.recordMetadata().topic()
                    }"
                )
            }
    }

}


data class Car(
    val carId: UUID,
    val carType: String,
    val fuelType: Int
)
