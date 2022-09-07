package ru.belkacar.core.test

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kafka.sender.SenderRecord
import reactor.kafka.sender.SenderResult
import java.util.*


class KafkaReactiveProducer<K, V>(
    bootstrapServers: String
) {

    companion object {
        private val logger = LoggerFactory.getLogger(KafkaReactiveProducer::class.java)
    }

    private val sender: KafkaSender<String, String>

    init {
        val producerProps = Properties()
        producerProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        val senderOptions = SenderOptions.create<String, String>(producerProps) //.maxInFlight(1024)

        sender = KafkaSender.create(senderOptions)
    }

    fun produce(topic: String, key: String, value: Any): SenderResult<String>? {

        val jsonValue = ObjectMapper().writeValueAsString(value)

        val producerRecord = ProducerRecord(topic, key, jsonValue)

        return send(Mono
            .just(SenderRecord.create(producerRecord, key)))
            .blockLast()
    }

    private fun send(outboundFlux: Mono<SenderRecord<String, String, String>>): Flux<SenderResult<String>> {
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
