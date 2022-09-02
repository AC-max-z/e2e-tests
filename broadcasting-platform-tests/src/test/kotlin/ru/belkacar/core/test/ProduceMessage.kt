package ru.belkacar.core.test

import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.core.publisher.Mono
import reactor.kafka.sender.SenderRecord
import ru.belkacar.core.test.tools.E2E
import java.util.*

@E2E
@SpringBootTest
class ProduceMessage @Autowired constructor(
    private var kafkaConfiguration: BroadcastingPlatformKafkaConfiguration,
    private val kafka: KafkaReactiveProducer<Int, String>
) {

    @Test
    fun pruduceMessage() {

        val key = UUID.randomUUID()
        val value = "{car:skoda123123}"

        println(kafka.consume(kafkaConfiguration.carPositionStream, key, value))
    }

}

