package ru.belkacar.core.test

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.belkacar.core.test.tools.E2E

@E2E
@SpringBootTest
class ProduceMessage @Autowired constructor(
    private var kafkaConfiguration: BroadcastingPlatformKafkaConfiguration,
    private val kafka: KafkaReactiveProducer<String, String>,
) {

    @Test
    fun pruduceMessage() {

        val positionEvent = TelematicsPositionGenerator().generate()

        println(kafka.consume(kafkaConfiguration.carPositionStream, positionEvent.deviceImei, positionEvent))

    }

}

