package ru.belkacar.core.test.kafka

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class BroadcastingPlatformProducer() : KafkaProducer {

    override fun produce() {

    }

}