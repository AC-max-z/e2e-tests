package ru.belkacar.core

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono.delay
import reactor.kotlin.test.test
import ru.belkacar.core.test.*
import ru.belkacar.telematics.broadcasting.platform.BroadcastingPlatformKafkaOperations
import java.time.Duration

const val DELAY_MESSAGE_PRODUCE_MS = 2_000L

@Component
class CarPositionsHelpers @Autowired constructor(
    private val broadcastingPlatformKafkaOperations: BroadcastingPlatformKafkaOperations
) {

    private fun getCarPositionEvent(
        carId: CarId<*> = generateCarId { },
        point: Location = LocationGenerator().generate()
    ): CarPositionEvent {
        return generateCarPosition {
            withCarId = carId
            withPosition = generatePosition {
                withNavigationData = generateNavigationData {
                    withLatitude = point.latitude
                    withLongitude = point.longitude
                }
            }
        }
    }

    fun produceCarPosition(
        carId: CarId<*> = generateCarId { },
        location: Location = LocationGenerator().generate()
    ) {
        delay(Duration.ofMillis(DELAY_MESSAGE_PRODUCE_MS)).block()!!
        broadcastingPlatformKafkaOperations.producerOps
            .producerLatestCarPosition(
                getCarPositionEvent(carId, location)
            )
            .test()
            .assertNext {

            }
            .verifyComplete()
    }

    fun produceCarPositionWithPointType(
        carId: CarId<*> = generateCarId {  },
        pointType: LocationGenerator.PointType = LocationGenerator.PointType.INSIDE_DEFAULT_ZONE
    ) {
        produceCarPosition(carId, LocationGenerator().withPointType(pointType).generate())
    }
}