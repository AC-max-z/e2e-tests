package ru.belkacar.core.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.serpro69.kfaker.Faker
import io.qameta.allure.AllureId
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import proto.belka.telematics.geofence.v1.CreateGeofenceCommand
import proto.belka.telematics.geofence.v1.Geofence
import reactor.kafka.sender.SenderResult
import reactor.kotlin.test.test
import ru.belkacar.core.test.steps.GeofenceSteps
import ru.belkacar.core.test.steps.GeofenceTypeSteps
import ru.belkacar.core.test.tools.E2E
import ru.belkacar.core.test.tools.JiraIssues
import ru.belkacar.core.test.tools.step
import java.time.Duration

const val CONSUMER_TIMEOUT_MS = 10_000L

@E2E
@SpringBootTest
class GeofenceDetectorTests @Autowired constructor(
    private val kafkaConfiguration: TelematicsServicesKafkaConfiguration,
    private val grpcClient: GrpcClient,
    private val producer: KafkaReactiveProducer<String, String>,
    @Qualifier("geofenceEventStreamConsumer")
    private val consumer: KafkaReactiveConsumer<String, String>
) {
    val faker = Faker()
    val geofenceTypeSteps = GeofenceTypeSteps(grpcClient)
    val geofenceSteps = GeofenceSteps(grpcClient)
    val mapper = ObjectMapper().registerKotlinModule()


    val pointInsideDefaultZone =
        LocationGenerator().withLongitude(37.61392593383789).withLatitude(55.7768626557418).generate()
    val pointOutsideDefaultZone =
        LocationGenerator().withLongitude(37.674522399902344).withLatitude(55.79944771620931).generate()

    @Test
    @AllureId("7692")
    @JiraIssues("TEL-565", "TEL-767")
    fun `should create enter geofence event on entering polygon`() {
        lateinit var carId: CarId
        lateinit var geofenceCarEvent: GeofenceCarEvent
        val geofenceType = "driving_zone"
        val geofenceDescription = "Autotests ${faker.backToTheFuture.quotes()}"

        step<CreateGeofenceCommand.Response>("Create new geofence") {
            geofenceSteps.createGeofence(
                geofenceType,
                geofenceDescription,
                Geofence.Restriction.OWNER,
                GeometryGenerator().generate().toProto()
            )
        }

        step<SenderResult<String>?>("Produce car position event outside created geofence") {
            val navigation = NavigationGenerator()
                .withLongitude(pointOutsideDefaultZone.longitude)
                .withLatitude(pointOutsideDefaultZone.latitude)
                .generate()
            val positionEvent = PositionGenerator()
                .withNavigation(navigation)
                .generate()
            val carPositionEvent = CarPositionEventGenerator()
                .withPositionEvent(positionEvent)
                .generate()
            carId = carPositionEvent.carId
            ObjectMapper().writeValueAsString(positionEvent)
            producer.produce(
                kafkaConfiguration.latestPositionEventsStream,
                carPositionEvent.carId.value,
                carPositionEvent
            )
        }

        step<SenderResult<String>?>("Produce car position event inside created geofence") {
            val navigation = NavigationGenerator()
                .withLongitude(pointInsideDefaultZone.longitude)
                .withLatitude(pointInsideDefaultZone.latitude)
                .generate()
            val positionEvent = PositionGenerator()
                .withNavigation(navigation)
                .generate()
            val carPositionEvent = CarPositionEventGenerator()
                .withPositionEvent(positionEvent)
                .withCarId(carId)
                .generate()
            producer.produce(
                kafkaConfiguration.latestPositionEventsStream,
                carPositionEvent.carId.value,
                carPositionEvent
            )
        }

        step("Geofence ${CarEnteredGeofence::class.simpleName} event created") {
            consumer
                .consume()
                .skipUntil { msg ->
                    // wait for message with key == carId
                    msg.key().equals(carId.value) &&
                            // and geofence description we created
                            mapper
                                .readValue(msg.value(), GeofenceCarEvent::class.java)
                                .payload
                                .geofence
                                .description == geofenceDescription
                }
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .test()
                .assertNext {
                    geofenceCarEvent = mapper
                        .readValue(it.value(), GeofenceCarEvent::class.java)
                    Assertions.assertEquals(
                        // check that @type of payload is CarEnteredGeofence
                        CarEnteredGeofence::class.simpleName, geofenceCarEvent.payload::class.simpleName
                    )
                    Assertions.assertEquals(
                        carId.value,
                        geofenceCarEvent.payload.carId.value
                    )
                    Assertions.assertEquals(geofenceType, geofenceCarEvent.payload.geofence.type.value)
                }
                .thenCancel()
                .log()
                .verify()
        }

    }

    @Test
    @AllureId("7710")
    @JiraIssues("TEL-565", "TEL-767")
    fun `the first event from a car that is not located in any of the geofences`() {
        lateinit var carId: CarId
        lateinit var geofenceCarEvent: GeofenceCarEvent
        val geofenceType = "driving_zone"
        val geofenceDescription = "Autotests ${faker.backToTheFuture.quotes()}"

        step<CreateGeofenceCommand.Response>("Create new geofence") {
            geofenceSteps.createGeofence(
                geofenceType,
                geofenceDescription,
                Geofence.Restriction.OWNER,
                GeometryGenerator().generate().toProto()
            )
        }

        step<SenderResult<String>?>("Produce car position event inside created geofence") {
            val navigation = NavigationGenerator()
                .withLongitude(pointInsideDefaultZone.longitude)
                .withLatitude(pointInsideDefaultZone.latitude)
                .generate()
            val positionEvent = PositionGenerator()
                .withNavigation(navigation)
                .generate()
            val carPositionEvent = CarPositionEventGenerator()
                .withPositionEvent(positionEvent)
                .generate()
            carId = carPositionEvent.carId
            producer.produce(
                kafkaConfiguration.latestPositionEventsStream,
                carPositionEvent.carId.value,
                carPositionEvent
            )
        }

        step("Geofence ${CarEnteredGeofence::class.simpleName} event created") {
            consumer
                .consume()
                .skipUntil { msg ->
                    // wait for message with key == carId
                    msg.key().equals(carId.value) &&
                            // and geofence description we created
                            mapper
                                .readValue(msg.value(), GeofenceCarEvent::class.java)
                                .payload
                                .geofence
                                .description == geofenceDescription
                }
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .test()
                .assertNext {
                    geofenceCarEvent = mapper
                        .readValue(it.value(), GeofenceCarEvent::class.java)
                    Assertions.assertEquals(
                        // check that @type of payload is CarEnteredGeofence
                        CarEnteredGeofence::class.simpleName, geofenceCarEvent.payload::class.simpleName
                    )
                    Assertions.assertEquals(
                        carId.value,
                        geofenceCarEvent.payload.carId.value
                    )
                    Assertions.assertEquals(geofenceType, geofenceCarEvent.payload.geofence.type.value)
                }
                .thenCancel()
                .log()
                .verify()
        }

    }

    @Test
    @AllureId("7693")
    @JiraIssues("TEL-565", "TEL-767")
    fun `should create leave geofence event on leaving polygon`() {
        lateinit var carId: CarId
        lateinit var geofenceCarEvent: GeofenceCarEvent
        val geofenceType = "driving_zone"
        val geofenceDescription = "Autotests ${faker.backToTheFuture.quotes()}"

        step<CreateGeofenceCommand.Response>("Create new geofence") {
            geofenceSteps.createGeofence(
                geofenceType,
                geofenceDescription,
                Geofence.Restriction.OWNER,
                GeometryGenerator().generate().toProto()
            )
        }

        step<SenderResult<String>?>("Produce car position event inside created geofence") {
            val navigation = NavigationGenerator()
                .withLongitude(pointInsideDefaultZone.longitude)
                .withLatitude(pointInsideDefaultZone.latitude)
                .generate()
            val positionEvent = PositionGenerator()
                .withNavigation(navigation)
                .generate()
            val carPositionEvent = CarPositionEventGenerator()
                .withPositionEvent(positionEvent)
                .generate()
            carId = carPositionEvent.carId
            producer.produce(
                kafkaConfiguration.latestPositionEventsStream,
                carPositionEvent.carId.value,
                carPositionEvent
            )
        }

        step<SenderResult<String>?>("Produce car position event outside created geofence") {
            val navigation = NavigationGenerator()
                .withLongitude(pointOutsideDefaultZone.longitude)
                .withLatitude(pointOutsideDefaultZone.latitude)
                .generate()
            val positionEvent = PositionGenerator()
                .withNavigation(navigation)
                .generate()
            val carPositionEvent = CarPositionEventGenerator()
                .withPositionEvent(positionEvent)
                .withCarId(carId)
                .generate()
            producer.produce(
                kafkaConfiguration.latestPositionEventsStream,
                carPositionEvent.carId.value,
                carPositionEvent
            )
        }

        step("Geofence ${CarLeavedGeofence::class.simpleName} event created") {
            consumer
                .consume()
                .skipUntil { msg ->
                    // wait for message with key == carId
                    msg.key().equals(carId.value) &&
                            // and geofence description we created
                            mapper
                                .readValue(msg.value(), GeofenceCarEvent::class.java)
                                .payload
                                .geofence
                                .description == geofenceDescription &&
                            // and @type of payload is CarLeavedGeofence
                            // (because there will also be CarEnteredGeofence on initial car position inside geofence)
                            mapper
                                .readValue(msg.value(), GeofenceCarEvent::class.java)
                                .payload::class == CarLeavedGeofence::class
                }
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .test()
                .assertNext {
                    geofenceCarEvent = mapper
                        .readValue(it.value(), GeofenceCarEvent::class.java)
                    Assertions.assertEquals(
                        CarLeavedGeofence::class.simpleName, geofenceCarEvent.payload::class.simpleName
                    )
                    Assertions.assertEquals(
                        carId.value,
                        geofenceCarEvent.payload.carId.value
                    )
                    Assertions.assertEquals(geofenceType, geofenceCarEvent.payload.geofence.type.value)
                }
                .thenCancel()
                .log()
                .verify()
        }

    }

    @Test
    @AllureId("7708")
    @JiraIssues("TEL-565", "TEL-767")
    fun `should create enter geofence event after increasing polygon`() {
        lateinit var carId: CarId
        lateinit var geofenceCarEvent: GeofenceCarEvent
        val geofenceType = "driving_zone"
        val geofenceDescription = "Autotests ${faker.backToTheFuture.quotes()}"
        println("Geofence desc: $geofenceDescription")

        val createGeofenceResponse =
            step<CreateGeofenceCommand.Response>("Create new geofence") {
                geofenceSteps.createGeofence(
                    geofenceType,
                    geofenceDescription,
                    Geofence.Restriction.OWNER,
                    GeometryGenerator().generate().toProto()
                )
            }

        step<SenderResult<String>?>("Produce car position event outside created geofence") {
            val navigation = NavigationGenerator()
                .withLongitude(pointOutsideDefaultZone.longitude)
                .withLatitude(pointOutsideDefaultZone.latitude)
                .generate()
            val positionEvent = PositionGenerator()
                .withNavigation(navigation)
                .generate()
            val carPositionEvent = CarPositionEventGenerator()
                .withPositionEvent(positionEvent)
                .generate()
            carId = carPositionEvent.carId
            ObjectMapper().writeValueAsString(positionEvent)
            producer.produce(
                kafkaConfiguration.latestPositionEventsStream,
                carPositionEvent.carId.value,
                carPositionEvent
            )
        }

        // TODO: fix, doesn't work (geometry not updated)
        step("Expand created geofence (car position is inside new geozone)") {
            geofenceSteps.updateGeofence(
                createGeofenceResponse.geofence.id,
                GeometryGenerator().withType(GeometryType.BIGGER).generate().toProto()
            )
        }

        step("Geofence ${CarEnteredGeofence::class.simpleName} event created") {
            consumer
                .consume()
                .skipUntil { msg ->
                    // wait for message with key == carId
                    msg.key().equals(carId.value) &&
                            // and geofence description we created
                            mapper
                                .readValue(msg.value(), GeofenceCarEvent::class.java)
                                .payload
                                .geofence
                                .description == geofenceDescription
                }
                //.timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .test()
                .assertNext {
                    geofenceCarEvent = mapper
                        .readValue(it.value(), GeofenceCarEvent::class.java)
                    Assertions.assertEquals(
                        // check that @type of payload is CarEnteredGeofence
                        CarEnteredGeofence::class.simpleName, geofenceCarEvent.payload::class.simpleName
                    )
                    Assertions.assertEquals(
                        carId.value,
                        geofenceCarEvent.payload.carId.value
                    )
                    Assertions.assertEquals(geofenceType, geofenceCarEvent.payload.geofence.type.value)
                }
                .thenCancel()
                .log()
                .verify()
        }

    }

    @Test
    @AllureId("7709")
    @JiraIssues("TEL-565", "TEL-767")
    fun `should create leave geofence event after decreasing polygon`() {
        lateinit var carId: CarId
        lateinit var geofenceCarEvent: GeofenceCarEvent
        val geofenceType = "driving_zone"
        val geofenceDescription = "Autotests ${faker.backToTheFuture.quotes()}"
        println("Geofence desc: $geofenceDescription")

        val createGeofenceResponse =
            step<CreateGeofenceCommand.Response>("Create new geofence") {
                geofenceSteps.createGeofence(
                    geofenceType,
                    geofenceDescription,
                    Geofence.Restriction.OWNER,
                    GeometryGenerator().generate().toProto()
                )
            }

        step<SenderResult<String>?>("Produce car position event inside created geofence") {
            val navigation = NavigationGenerator()
                .withLongitude(pointInsideDefaultZone.longitude)
                .withLatitude(pointInsideDefaultZone.latitude)
                .generate()
            val positionEvent = PositionGenerator()
                .withNavigation(navigation)
                .generate()
            val carPositionEvent = CarPositionEventGenerator()
                .withPositionEvent(positionEvent)
                .generate()
            carId = carPositionEvent.carId
            ObjectMapper().writeValueAsString(positionEvent)
            producer.produce(
                kafkaConfiguration.latestPositionEventsStream,
                carPositionEvent.carId.value,
                carPositionEvent
            )
        }

        // TODO: fix, doesn't work (geometry not updated)
        step("Shrink created geofence (car position is inside new geozone)") {
            geofenceSteps.updateGeofence(
                createGeofenceResponse.geofence.id,
                GeometryGenerator().withType(GeometryType.LESSER).generate().toProto()
            )
        }

        step("Geofence ${CarLeavedGeofence::class.simpleName} event created") {
            consumer
                .consume()
                .skipUntil { msg ->
                    // wait for message with key == carId
                    msg.key().equals(carId.value) &&
                            // and geofence description we created
                            mapper
                                .readValue(msg.value(), GeofenceCarEvent::class.java)
                                .payload
                                .geofence
                                .description == geofenceDescription &&
                            // and @type of payload is CarLeavedGeofence
                            // (because there will also be CarEnteredGeofence on initial car position inside geofence)
                            mapper
                                .readValue(msg.value(), GeofenceCarEvent::class.java)
                                .payload::class == CarLeavedGeofence::class
                }
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .test()
                .assertNext {
                    geofenceCarEvent = mapper
                        .readValue(it.value(), GeofenceCarEvent::class.java)
                    Assertions.assertEquals(
                        CarLeavedGeofence::class.simpleName, geofenceCarEvent.payload::class.simpleName
                    )
                    Assertions.assertEquals(
                        carId.value,
                        geofenceCarEvent.payload.carId.value
                    )
                    Assertions.assertEquals(geofenceType, geofenceCarEvent.payload.geofence.type.value)
                }
                .thenCancel()
                .log()
                .verify()
        }

    }
}
