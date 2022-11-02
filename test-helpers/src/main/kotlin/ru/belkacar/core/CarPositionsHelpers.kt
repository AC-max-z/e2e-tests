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
        // Delay here is done to prevent situations where
        //    0. Geofence been updated or deleted or whatever
        //    1. The update/delete step in geofence helpers checks that geofence-manager now returns updated data BUT
        //    2. These changes may not yet been dispatched to geofence-detector (for example)
        //    3. We produce message before geofence-detector (for example) receives info about changes
        //    4. ???
        //    5. NO PROFIT!
        // there may be a more refined way to know if geofence-detector (for example) received expected changes from geofence-manager
        // but at the time it is not implemented as it's rather challenging (according to Artem Kulik)
        // hence, the delays
        // sucks, yea:(
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
        carId: CarId<*> = generateCarId { },
        pointType: LocationGenerator.PointType = LocationGenerator.PointType.INSIDE_DEFAULT_ZONE
    ) {
        produceCarPosition(carId, LocationGenerator().withPointType(pointType).generate())
    }
}