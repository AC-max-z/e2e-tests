package ru.belkacar.core.test

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import reactor.kafka.receiver.ReceiverRecord
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.test.test
import ru.belkacar.core.test.tools.E2E
import java.time.Duration
import java.util.function.Predicate
import kotlin.math.exp
import kotlin.test.assertContains

@E2E
@SpringBootTest
class ProduceMessage @Autowired constructor(
    private var kafkaConfiguration: BroadcastingPlatformKafkaConfiguration,
    private val producer: KafkaReactiveProducer<String, String>,
    @Qualifier("carPositionStreamConsumer")
    private val consumer: KafkaReactiveConsumer<String, String>
) {

    @Test
    fun pruduceMessage() {
        val positionEvent = TelematicsPositionGenerator().generate()
        val key = positionEvent.deviceImei
        val expectMessage = ObjectMapper().writeValueAsString(positionEvent)

        producer.produce(kafkaConfiguration.carPositionStream, key, positionEvent)

        consumer
            .consume()
            .skipUntil { r -> r.key().equals(key) }
            .test()
            .assertNext {
                assertEquals(expectMessage, it.value())
                assertEquals(key, it.key())
            }
            .thenCancel()
            .log()
            .verify()
    }


}

