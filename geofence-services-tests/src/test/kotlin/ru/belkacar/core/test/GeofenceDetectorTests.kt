package ru.belkacar.core.test

import io.qameta.allure.AllureId
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import proto.belka.telematics.geofence.v1.CreateGeofenceCommandKt
import proto.belka.telematics.geofence.v1.Geofence
import proto.belka.telematics.geofence.v1.type.GeofenceTypeKt
import reactor.kotlin.test.test
import ru.belkacar.core.test.tools.E2E
import ru.belkacar.core.test.tools.JiraIssues
import ru.belkacar.core.test.tools.assertNextStep
import ru.belkacar.core.test.tools.step
import ru.belkacar.telematics.broadcasting.platform.BroadcastingPlatformKafkaOperations
import ru.belkacar.telematics.geofence.GeofencesGrpcOperations
import ru.belkacar.telematics.geofence.GeofencesKafkaOperations
import ru.belkacar.telematics.geofence.toProto
import ru.belkacar.telematics.geofence.toUUID
import java.time.Duration
import kotlin.test.assertIs

@E2E
@SpringBootTest
class GeofenceDetectorTests @Autowired constructor(
    private val geofenceGrpcOperations: GeofencesGrpcOperations,
    private val geofenceKafkaOperations: GeofencesKafkaOperations,
    private val broadcastingPlatformKafkaOperations: BroadcastingPlatformKafkaOperations
) {
    
    val faker = FakerProvider.faker
    
    
    val pointInsideDefaultZone =
        LocationGenerator().withLongitude(37.61392593383789).withLatitude(55.7768626557418).generate()
    val pointOutsideDefaultZone =
        LocationGenerator().withLongitude(37.674522399902344).withLatitude(55.79944771620931).generate()
    val pointInsideSmallZone =
        LocationGenerator().withLongitude(37.625770568847656).withLatitude(55.75600328396566).generate()
    val pointInsidePinnedOutZone =
        LocationGenerator().withLongitude(37.60045051574707).withLatitude(55.72783509242637).generate()
    
    
    @ParameterizedTest
    @ValueSource(strings = ["driving_zone", "police_impound"])
    @AllureId("7692")
    @JiraIssues("TEL-565", "TEL-767")
    internal fun `should create enter geofence event on entering polygon`(zoneKey: String) {
        val carId = generateCarId {  }
        val polygon = GeometryGenerator().generate()
        
        val geofence = step<Geofence>("Create new geofence") {
            geofenceGrpcOperations.geofenceOps
                .create(
                    CreateGeofenceCommandKt.request {
                        geofenceTypeKey = GeofenceTypeKt.key { value = zoneKey }
                        description = "Autotests ${faker.backToTheFuture.quotes()}"
                        restriction = Geofence.Restriction.OWNER
                        geometry = polygon.toProto()
                    }
                )
                .map { it.geofence }
                .block()!!
        }
    
        step("Produce car position event inside created geofence") {
            
            val carPositionEvent = generateCarPosition {
                withCarId = carId
                withPosition = generatePosition {
                    withNavigationData = generateNavigationData {
                        withLatitude = pointInsideDefaultZone.latitude
                        withLongitude = pointInsideDefaultZone.longitude
                    }
                }
            }
        
            broadcastingPlatformKafkaOperations.producerOps
                .producerLatestCarPosition(carPositionEvent)
                .test()
                .assertNext {
                
                }
                .verifyComplete()
        }
    
        step("Geofence ${CarEnteredGeofence::class.simpleName} event created") {
            geofenceKafkaOperations.consumerOps.consumeCarGeofenceEvents()
                .timeout(Duration.ofSeconds(10))
                .doOnNext { println("${it.key()} :: ${it.value()}") }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            && record.value().payload.geofence.id.value == geofence.id.toUUID()
                }
                .test()
                .assertNextStep("") { record ->
                    with(record.value()) {
                        assertIs<ru.belkacar.telematics.geofence.CarEnteredGeofence>(payload)
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }
    }

//    @ParameterizedTest
//    @ValueSource(strings = ["police_impound", "driving_zone"])
//    @AllureId("7711")
//    @JiraIssues("TEL-565", "TEL-767")
//    fun `the first event from a car that is not located in any of the geofences`(input: String) {
//        lateinit var carId: CarId
//        lateinit var geofenceCarEvent: GeofenceCarEvent
//        val geofenceDescription = "Autotests ${faker.backToTheFuture.quotes()}"
//        println("Geofence desc: $geofenceDescription")
//
//        step<CreateGeofenceCommand.Response>("Create new geofence") {
//            geofenceSteps.createGeofence(
//                input,
//                geofenceDescription,
//                Geofence.Restriction.OWNER,
//                GeometryGenerator().generate().toProto()
//            )
//        }
//
//        step<SenderResult<String>?>("Produce car position event inside created geofence") {
//            val navigation = NavigationGenerator()
//                .withLongitude(pointInsideDefaultZone.longitude)
//                .withLatitude(pointInsideDefaultZone.latitude)
//                .generate()
//            val positionEvent = PositionGenerator()
//                .withNavigation(navigation)
//                .generate()
//            val carPositionEvent = CarPositionEventGenerator()
//                .withPositionEvent(positionEvent)
//                .generate()
//            carId = carPositionEvent.carId
//            producer.produce(
//                kafkaConfiguration.latestPositionEventsStream,
//                carPositionEvent.carId.value,
//                carPositionEvent
//            )
//        }
//
//        step("Geofence ${CarEnteredGeofence::class.simpleName} event created") {
//            consumer
//                .consume()
//                .skipUntil { msg ->
//                    // wait for message with key == carId
//                    msg.key().equals(carId.value) &&
//                            // and geofence description we created
//                            mapper
//                                .readValue(msg.value(), GeofenceCarEvent::class.java)
//                                .payload
//                                .geofence
//                                .description == geofenceDescription &&
//                            mapper
//                                .readValue(msg.value(), GeofenceCarEvent::class.java)
//                                .payload
//                                .geofence
//                                .type
//                                .value == input
//                }
//                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
//                .test()
//                .assertNext {
//                    geofenceCarEvent = mapper
//                        .readValue(it.value(), GeofenceCarEvent::class.java)
//                    Assertions.assertEquals(
//                        // check that @type of payload is CarEnteredGeofence
//                        CarEnteredGeofence::class.simpleName, geofenceCarEvent.payload::class.simpleName
//                    )
//                    Assertions.assertEquals(
//                        carId.value,
//                        geofenceCarEvent.payload.carId.value
//                    )
//                    Assertions.assertEquals(input, geofenceCarEvent.payload.geofence.type.value)
//                }
//                .thenCancel()
//                .log()
//                .verify()
//        }
//
//    }
//
//    @ParameterizedTest
//    @ValueSource(strings = ["driving_zone", "police_impound"])
//    @AllureId("7693")
//    @JiraIssues("TEL-565", "TEL-767")
//    fun `should create leave geofence event on leaving polygon`(input: String) {
//        lateinit var carId: CarId
//        lateinit var geofenceCarEvent: GeofenceCarEvent
//        val geofenceDescription = "Autotests ${faker.backToTheFuture.quotes()}"
//
//        step<CreateGeofenceCommand.Response>("Create new geofence") {
//            geofenceSteps.createGeofence(
//                input,
//                geofenceDescription,
//                Geofence.Restriction.OWNER,
//                GeometryGenerator().generate().toProto()
//            )
//        }
//
//        step<SenderResult<String>?>("Produce car position event inside created geofence") {
//            val navigation = NavigationGenerator()
//                .withLongitude(pointInsideDefaultZone.longitude)
//                .withLatitude(pointInsideDefaultZone.latitude)
//                .generate()
//            val positionEvent = PositionGenerator()
//                .withNavigation(navigation)
//                .generate()
//            val carPositionEvent = CarPositionEventGenerator()
//                .withPositionEvent(positionEvent)
//                .generate()
//            carId = carPositionEvent.carId
//            producer.produce(
//                kafkaConfiguration.latestPositionEventsStream,
//                carPositionEvent.carId.value,
//                carPositionEvent
//            )
//        }
//
//        step<SenderResult<String>?>("Produce car position event outside created geofence") {
//            val navigation = NavigationGenerator()
//                .withLongitude(pointOutsideDefaultZone.longitude)
//                .withLatitude(pointOutsideDefaultZone.latitude)
//                .generate()
//            val positionEvent = PositionGenerator()
//                .withNavigation(navigation)
//                .generate()
//            val carPositionEvent = CarPositionEventGenerator()
//                .withPositionEvent(positionEvent)
//                .withCarId(carId)
//                .generate()
//            producer.produce(
//                kafkaConfiguration.latestPositionEventsStream,
//                carPositionEvent.carId.value,
//                carPositionEvent
//            )
//        }
//
//        step("Geofence ${CarLeavedGeofence::class.simpleName} event created") {
//            consumer
//                .consume()
//                .skipUntil { msg ->
//                    // wait for message with key == carId
//                    msg.key().equals(carId.value) &&
//                            // and geofence description we created
//                            mapper
//                                .readValue(msg.value(), GeofenceCarEvent::class.java)
//                                .payload
//                                .geofence
//                                .description == geofenceDescription &&
//                            // and @type of payload is CarLeavedGeofence
//                            // (because there will also be CarEnteredGeofence on initial car position inside geofence)
//                            mapper
//                                .readValue(msg.value(), GeofenceCarEvent::class.java)
//                                .payload::class == CarLeavedGeofence::class
//                }
//                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
//                .test()
//                .assertNext {
//                    geofenceCarEvent = mapper
//                        .readValue(it.value(), GeofenceCarEvent::class.java)
//                    Assertions.assertEquals(
//                        CarLeavedGeofence::class.simpleName, geofenceCarEvent.payload::class.simpleName
//                    )
//                    Assertions.assertEquals(
//                        carId.value,
//                        geofenceCarEvent.payload.carId.value
//                    )
//                    Assertions.assertEquals(input, geofenceCarEvent.payload.geofence.type.value)
//                }
//                .thenCancel()
//                .log()
//                .verify()
//        }
//
//    }
//
//    @ParameterizedTest
//    @ValueSource(strings = ["driving_zone", "police_impound"])
//    @AllureId("7708")
//    @JiraIssues("TEL-565", "TEL-767")
//    fun `should create enter geofence event after increasing polygon`(input: String) {
//        lateinit var carId: CarId
//        lateinit var geofenceCarEvent: GeofenceCarEvent
//        val geofenceDescription = "Autotests ${faker.backToTheFuture.quotes()}"
//        println("Geofence desc: $geofenceDescription")
//
//        val createGeofenceResponse =
//            step<CreateGeofenceCommand.Response>("Create new geofence") {
//                geofenceSteps.createGeofence(
//                    input,
//                    geofenceDescription,
//                    Geofence.Restriction.OWNER,
//                    GeometryGenerator().generate().toProto()
//                )
//            }
//
//        step<SenderResult<String>?>("Produce car position event outside created geofence") {
//            val navigation = NavigationGenerator()
//                .withLongitude(pointOutsideDefaultZone.longitude)
//                .withLatitude(pointOutsideDefaultZone.latitude)
//                .generate()
//            val positionEvent = PositionGenerator()
//                .withNavigation(navigation)
//                .generate()
//            val carPositionEvent = CarPositionEventGenerator()
//                .withPositionEvent(positionEvent)
//                .generate()
//            carId = carPositionEvent.carId
//            producer.produce(
//                kafkaConfiguration.latestPositionEventsStream,
//                carPositionEvent.carId.value,
//                carPositionEvent
//            )
//        }
//
//        step("Expand created geofence (car position is inside new geozone)") {
//            geofenceSteps.updateGeofence(
//                createGeofenceResponse.geofence.id,
//                GeometryGenerator().withType(GeometryType.BIGGER).generate().toProto()
//            )
//        }
//
//        step("Geofence ${CarEnteredGeofence::class.simpleName} event created") {
//            consumer
//                .consume()
//                .skipUntil { msg ->
//                    // wait for message with key == carId
//                    msg.key().equals(carId.value) &&
//                            // and geofence description we created
//                            mapper
//                                .readValue(msg.value(), GeofenceCarEvent::class.java)
//                                .payload
//                                .geofence
//                                .description == geofenceDescription
//                }
//                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
//                .test()
//                .assertNext {
//                    geofenceCarEvent = mapper
//                        .readValue(it.value(), GeofenceCarEvent::class.java)
//                    Assertions.assertEquals(
//                        // check that @type of payload is CarEnteredGeofence
//                        CarEnteredGeofence::class.simpleName, geofenceCarEvent.payload::class.simpleName
//                    )
//                    Assertions.assertEquals(
//                        carId.value,
//                        geofenceCarEvent.payload.carId.value
//                    )
//                    Assertions.assertEquals(input, geofenceCarEvent.payload.geofence.type.value)
//                }
//                .thenCancel()
//                .log()
//                .verify()
//        }
//
//    }
//
//    @ParameterizedTest
//    @ValueSource(strings = ["driving_zone", "police_impound"])
//    @AllureId("7709")
//    @JiraIssues("TEL-565", "TEL-767")
//    fun `should create leave geofence event after decreasing polygon`(input: String) {
//        lateinit var carId: CarId
//        lateinit var geofenceCarEvent: GeofenceCarEvent
//        val geofenceDescription = "Autotests ${faker.backToTheFuture.quotes()}"
//
//        val createGeofenceResponse =
//            step<CreateGeofenceCommand.Response>("Create new geofence") {
//                geofenceSteps.createGeofence(
//                    input,
//                    geofenceDescription,
//                    Geofence.Restriction.OWNER,
//                    GeometryGenerator().withType(GeometryType.BIGGER).generate().toProto()
//                )
//            }
//
//        step<SenderResult<String>?>("Produce car position event inside created geofence") {
//            val navigation = NavigationGenerator()
//                .withLongitude(pointInsideDefaultZone.longitude)
//                .withLatitude(pointInsideDefaultZone.latitude)
//                .generate()
//            val positionEvent = PositionGenerator()
//                .withNavigation(navigation)
//                .generate()
//            val carPositionEvent = CarPositionEventGenerator()
//                .withPositionEvent(positionEvent)
//                .generate()
//            carId = carPositionEvent.carId
//            producer.produce(
//                kafkaConfiguration.latestPositionEventsStream,
//                carPositionEvent.carId.value,
//                carPositionEvent
//            )
//        }
//
//        step("Shrink created geofence (car position is outside updated geozone)") {
//            geofenceSteps.updateGeofence(
//                createGeofenceResponse.geofence.id,
//                GeometryGenerator().withType(GeometryType.LESSER).generate().toProto()
//            )
//        }
//
//        step("Geofence ${CarLeavedGeofence::class.simpleName} event created") {
//            consumer
//                .consume()
//                .skipUntil { msg ->
//                    // wait for message with key == carId
//                    msg.key().equals(carId.value) &&
//                            // and geofence description we created
//                            mapper
//                                .readValue(msg.value(), GeofenceCarEvent::class.java)
//                                .payload
//                                .geofence
//                                .description == geofenceDescription &&
//                            // and @type of payload is CarLeavedGeofence
//                            // (because there will also be CarEnteredGeofence on initial car position inside geofence)
//                            mapper
//                                .readValue(msg.value(), GeofenceCarEvent::class.java)
//                                .payload::class == CarLeavedGeofence::class
//                }
//                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
//                .test()
//                .assertNext {
//                    geofenceCarEvent = mapper
//                        .readValue(it.value(), GeofenceCarEvent::class.java)
//                    Assertions.assertEquals(
//                        CarLeavedGeofence::class.simpleName, geofenceCarEvent.payload::class.simpleName
//                    )
//                    Assertions.assertEquals(
//                        carId.value,
//                        geofenceCarEvent.payload.carId.value
//                    )
//                    Assertions.assertEquals(input, geofenceCarEvent.payload.geofence.type.value)
//                }
//                .thenCancel()
//                .log()
//                .verify()
//        }
//
//    }
//
//    @ParameterizedTest
//    @ValueSource(strings = ["driving_zone", "police_impound"])
//    @AllureId("9401")
//    @JiraIssues("TEL-565", "TEL-767")
//    fun `should create leave geofence event after geofence deletion`(input: String) {
//        lateinit var carId: CarId
//        lateinit var geofenceCarEvent: GeofenceCarEvent
//        val geofenceDescription = "Autotests ${faker.backToTheFuture.quotes()}"
//        println("Geofence desc: $geofenceDescription")
//
//        val createGeofenceResponse =
//            step<CreateGeofenceCommand.Response>("Create new geofence") {
//                geofenceSteps.createGeofence(
//                    input,
//                    geofenceDescription,
//                    Geofence.Restriction.OWNER,
//                    GeometryGenerator().withType(GeometryType.BIGGER).generate().toProto()
//                )
//            }
//
//        step<SenderResult<String>?>("Produce car position event inside created geofence") {
//            val navigation = NavigationGenerator()
//                .withLongitude(pointInsideDefaultZone.longitude)
//                .withLatitude(pointInsideDefaultZone.latitude)
//                .generate()
//            val positionEvent = PositionGenerator()
//                .withNavigation(navigation)
//                .generate()
//            val carPositionEvent = CarPositionEventGenerator()
//                .withPositionEvent(positionEvent)
//                .generate()
//            carId = carPositionEvent.carId
//            producer.produce(
//                kafkaConfiguration.latestPositionEventsStream,
//                carPositionEvent.carId.value,
//                carPositionEvent
//            )
//        }
//
//        step("Delete created geofence") {
//            geofenceSteps.deleteGeofence(
//                createGeofenceResponse.geofence.id
//            )
//        }
//
//        step("Geofence ${CarLeavedGeofence::class.simpleName} event created") {
//            consumer
//                .consume()
//                .skipUntil { msg ->
//                    // wait for message with key == carId
//                    msg.key().equals(carId.value) &&
//                            // and geofence description we created
//                            mapper
//                                .readValue(msg.value(), GeofenceCarEvent::class.java)
//                                .payload
//                                .geofence
//                                .description == geofenceDescription &&
//                            // and @type of payload is CarLeavedGeofence
//                            // (because there will also be CarEnteredGeofence on initial car position inside geofence)
//                            mapper
//                                .readValue(msg.value(), GeofenceCarEvent::class.java)
//                                .payload::class == CarLeavedGeofence::class
//                }
//                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
//                .test()
//                .assertNext {
//                    geofenceCarEvent = mapper
//                        .readValue(it.value(), GeofenceCarEvent::class.java)
//                    Assertions.assertEquals(
//                        CarLeavedGeofence::class.simpleName, geofenceCarEvent.payload::class.simpleName
//                    )
//                    Assertions.assertEquals(
//                        carId.value,
//                        geofenceCarEvent.payload.carId.value
//                    )
//                    Assertions.assertEquals(input, geofenceCarEvent.payload.geofence.type.value)
//                }
//                .thenCancel()
//                .log()
//                .verify()
//        }
//
//    }
//
//    @Test
//    @AllureId("9402")
//    @JiraIssues("TEL-565", "TEL-767")
//    fun `should create enter event for embedded geofence`() {
//        lateinit var carId: CarId
//        lateinit var geofenceCarEvent: GeofenceCarEvent
//        val outerGeofenceDescription = "Autotests ${faker.backToTheFuture.quotes()}"
//        val innerGeofenceDescription = "Autotests ${faker.backToTheFuture.quotes()}"
//        val outerGeozoneType = "driving_zone"
//        val innerGeozoneType = "police_impound"
//
//        val createOuterGeofenceResponse =
//            step<CreateGeofenceCommand.Response>("Create new outer geofence") {
//                geofenceSteps.createGeofence(
//                    outerGeozoneType,
//                    outerGeofenceDescription,
//                    Geofence.Restriction.OWNER,
//                    GeometryGenerator().generate().toProto()
//                )
//            }
//
//        val createInnerGeofenceResponse =
//            step<CreateGeofenceCommand.Response>("Create new inner geofence") {
//                geofenceSteps.createGeofence(
//                    innerGeozoneType,
//                    innerGeofenceDescription,
//                    Geofence.Restriction.OWNER,
//                    GeometryGenerator().withType(GeometryType.LESSER).generate().toProto()
//                )
//            }
//
//        step<SenderResult<String>?>("Produce car position event inside outer geofence") {
//            val navigation = NavigationGenerator()
//                .withLongitude(pointInsideDefaultZone.longitude)
//                .withLatitude(pointInsideDefaultZone.latitude)
//                .generate()
//            val positionEvent = PositionGenerator()
//                .withNavigation(navigation)
//                .generate()
//            val carPositionEvent = CarPositionEventGenerator()
//                .withPositionEvent(positionEvent)
//                .generate()
//            carId = carPositionEvent.carId
//            producer.produce(
//                kafkaConfiguration.latestPositionEventsStream,
//                carPositionEvent.carId.value,
//                carPositionEvent
//            )
//        }
//
//        step("Geofence ${CarEnteredGeofence::class.simpleName} event created (outer)") {
//            consumer
//                .consume()
//                .skipUntil { msg ->
//                    // wait for message with key == carId
//                    msg.key().equals(carId.value) &&
//                            // and geofence description we created
//                            mapper
//                                .readValue(msg.value(), GeofenceCarEvent::class.java)
//                                .payload
//                                .geofence
//                                .description == outerGeofenceDescription
//                }
//                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
//                .test()
//                .assertNext {
//                    geofenceCarEvent = mapper
//                        .readValue(it.value(), GeofenceCarEvent::class.java)
//                    Assertions.assertEquals(
//                        CarEnteredGeofence::class.simpleName, geofenceCarEvent.payload::class.simpleName
//                    )
//                    Assertions.assertEquals(
//                        carId.value,
//                        geofenceCarEvent.payload.carId.value
//                    )
//                    Assertions.assertEquals(outerGeozoneType, geofenceCarEvent.payload.geofence.type.value)
//                }
//                .thenCancel()
//                .log()
//                .verify()
//        }
//
//        step<SenderResult<String>?>("Produce car position event inside inner geofence") {
//            val navigation = NavigationGenerator()
//                .withLongitude(pointInsideSmallZone.longitude)
//                .withLatitude(pointInsideSmallZone.latitude)
//                .generate()
//            val positionEvent = PositionGenerator()
//                .withNavigation(navigation)
//                .generate()
//            val carPositionEvent = CarPositionEventGenerator()
//                .withPositionEvent(positionEvent)
//                .withCarId(carId)
//                .generate()
//            producer.produce(
//                kafkaConfiguration.latestPositionEventsStream,
//                carPositionEvent.carId.value,
//                carPositionEvent
//            )
//        }
//
//        step("Geofence ${CarEnteredGeofence::class.simpleName} event created (inner)") {
//            consumer
//                .consume()
//                .skipUntil { msg ->
//                    // wait for message with key == carId
//                    msg.key().equals(carId.value) &&
//                            // and geofence description we created
//                            mapper
//                                .readValue(msg.value(), GeofenceCarEvent::class.java)
//                                .payload
//                                .geofence
//                                .description == innerGeofenceDescription
//                }
//                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
//                .test()
//                .assertNext {
//                    geofenceCarEvent = mapper
//                        .readValue(it.value(), GeofenceCarEvent::class.java)
//                    Assertions.assertEquals(
//                        CarEnteredGeofence::class.simpleName, geofenceCarEvent.payload::class.simpleName
//                    )
//                    Assertions.assertEquals(
//                        carId.value,
//                        geofenceCarEvent.payload.carId.value
//                    )
//                    Assertions.assertEquals(innerGeozoneType, geofenceCarEvent.payload.geofence.type.value)
//                }
//                .thenCancel()
//                .log()
//                .verify()
//        }
//
//        step<SenderResult<String>?>("Produce car position event inside outer geofence") {
//            val navigation = NavigationGenerator()
//                .withLongitude(pointInsideDefaultZone.longitude)
//                .withLatitude(pointInsideDefaultZone.latitude)
//                .generate()
//            val positionEvent = PositionGenerator()
//                .withNavigation(navigation)
//                .generate()
//            val carPositionEvent = CarPositionEventGenerator()
//                .withPositionEvent(positionEvent)
//                .withCarId(carId)
//                .generate()
//            producer.produce(
//                kafkaConfiguration.latestPositionEventsStream,
//                carPositionEvent.carId.value,
//                carPositionEvent
//            )
//        }
//
//        step("Geofence ${CarLeavedGeofence::class.simpleName} event created (inner)") {
//            consumer
//                .consume()
//                .skipUntil { msg ->
//                    // wait for message with key == carId
//                    msg.key().equals(carId.value) &&
//                            // and geofence description we created
//                            mapper
//                                .readValue(msg.value(), GeofenceCarEvent::class.java)
//                                .payload
//                                .geofence
//                                .description == innerGeofenceDescription &&
//                            // and @type of payload is CarLeavedGeofence
//                            mapper
//                                .readValue(msg.value(), GeofenceCarEvent::class.java)
//                                .payload::class == CarLeavedGeofence::class
//                }
//                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
//                .test()
//                .assertNext {
//                    geofenceCarEvent = mapper
//                        .readValue(it.value(), GeofenceCarEvent::class.java)
//                    Assertions.assertEquals(
//                        CarLeavedGeofence::class.simpleName, geofenceCarEvent.payload::class.simpleName
//                    )
//                    Assertions.assertEquals(
//                        carId.value,
//                        geofenceCarEvent.payload.carId.value
//                    )
//                    Assertions.assertEquals(innerGeozoneType, geofenceCarEvent.payload.geofence.type.value)
//                }
//                .thenCancel()
//                .log()
//                .verify()
//        }
//
//
//        step<SenderResult<String>?>("Produce car position event outside outer geofence") {
//            val navigation = NavigationGenerator()
//                .withLongitude(pointOutsideDefaultZone.longitude)
//                .withLatitude(pointOutsideDefaultZone.latitude)
//                .generate()
//            val positionEvent = PositionGenerator()
//                .withNavigation(navigation)
//                .generate()
//            val carPositionEvent = CarPositionEventGenerator()
//                .withPositionEvent(positionEvent)
//                .withCarId(carId)
//                .generate()
//            ObjectMapper().writeValueAsString(positionEvent)
//            producer.produce(
//                kafkaConfiguration.latestPositionEventsStream,
//                carPositionEvent.carId.value,
//                carPositionEvent
//            )
//        }
//
//        step("Geofence ${CarLeavedGeofence::class.simpleName} event created (outer)") {
//            consumer
//                .consume()
//                .skipUntil { msg ->
//                    // wait for message with key == carId
//                    msg.key().equals(carId.value) &&
//                            // and geofence description we created
//                            mapper
//                                .readValue(msg.value(), GeofenceCarEvent::class.java)
//                                .payload
//                                .geofence
//                                .description == outerGeofenceDescription &&
//                            // and @type of payload is CarLeavedGeofence
//                            mapper
//                                .readValue(msg.value(), GeofenceCarEvent::class.java)
//                                .payload::class == CarLeavedGeofence::class
//                }
//                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
//                .test()
//                .assertNext {
//                    geofenceCarEvent = mapper
//                        .readValue(it.value(), GeofenceCarEvent::class.java)
//                    Assertions.assertEquals(
//                        CarLeavedGeofence::class.simpleName, geofenceCarEvent.payload::class.simpleName
//                    )
//                    Assertions.assertEquals(
//                        carId.value,
//                        geofenceCarEvent.payload.carId.value
//                    )
//                    Assertions.assertEquals(outerGeozoneType, geofenceCarEvent.payload.geofence.type.value)
//                }
//                .thenCancel()
//                .log()
//                .verify()
//        }
//
//    }
//
//    @Test
//    @AllureId("9403")
//    @JiraIssues("TEL-565", "TEL-767")
//    fun `should create enter event for pinned out geofence`() {
//        lateinit var carId: CarId
//        lateinit var geofenceCarEvent: GeofenceCarEvent
//        val geofenceDescription = "Autotests ${faker.backToTheFuture.quotes()}"
//        val geozoneType = "driving_zone"
//
//        val createOuterGeofenceResponse =
//            step<CreateGeofenceCommand.Response>("Create new pinned-out geofence") {
//                geofenceSteps.createGeofence(
//                    geozoneType,
//                    geofenceDescription,
//                    Geofence.Restriction.OWNER,
//                    GeometryGenerator().withType(GeometryType.PINNED_OUT).generate().toProto()
//                )
//            }
//
//        step<SenderResult<String>?>("Produce car position event outside geofence") {
//            val navigation = NavigationGenerator()
//                .withLongitude(pointOutsideDefaultZone.longitude)
//                .withLatitude(pointOutsideDefaultZone.latitude)
//                .generate()
//            val positionEvent = PositionGenerator()
//                .withNavigation(navigation)
//                .generate()
//            val carPositionEvent = CarPositionEventGenerator()
//                .withPositionEvent(positionEvent)
//                .generate()
//            carId = carPositionEvent.carId
//            producer.produce(
//                kafkaConfiguration.latestPositionEventsStream,
//                carPositionEvent.carId.value,
//                carPositionEvent
//            )
//        }
//
//        step<SenderResult<String>?>("Produce car position event inside geofence") {
//            val navigation = NavigationGenerator()
//                .withLongitude(pointInsideSmallZone.longitude)
//                .withLatitude(pointInsideSmallZone.latitude)
//                .generate()
//            val positionEvent = PositionGenerator()
//                .withNavigation(navigation)
//                .generate()
//            val carPositionEvent = CarPositionEventGenerator()
//                .withPositionEvent(positionEvent)
//                .withCarId(carId)
//                .generate()
//            producer.produce(
//                kafkaConfiguration.latestPositionEventsStream,
//                carPositionEvent.carId.value,
//                carPositionEvent
//            )
//        }
//
//        step("Geofence ${CarEnteredGeofence::class.simpleName} event created") {
//            consumer
//                .consume()
//                .skipUntil { msg ->
//                    // wait for message with key == carId
//                    msg.key().equals(carId.value) &&
//                            // and geofence description we created
//                            mapper
//                                .readValue(msg.value(), GeofenceCarEvent::class.java)
//                                .payload
//                                .geofence
//                                .description == geofenceDescription
//                }
//                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
//                .test()
//                .assertNext {
//                    geofenceCarEvent = mapper
//                        .readValue(it.value(), GeofenceCarEvent::class.java)
//                    Assertions.assertEquals(
//                        CarEnteredGeofence::class.simpleName, geofenceCarEvent.payload::class.simpleName
//                    )
//                    Assertions.assertEquals(
//                        carId.value,
//                        geofenceCarEvent.payload.carId.value
//                    )
//                    Assertions.assertEquals(geozoneType, geofenceCarEvent.payload.geofence.type.value)
//                }
//                .thenCancel()
//                .log()
//                .verify()
//        }
//
//        step<SenderResult<String>?>("Produce car position event inside pinned-out zone") {
//            val navigation = NavigationGenerator()
//                .withLongitude(pointInsidePinnedOutZone.longitude)
//                .withLatitude(pointInsidePinnedOutZone.latitude)
//                .generate()
//            val positionEvent = PositionGenerator()
//                .withNavigation(navigation)
//                .generate()
//            val carPositionEvent = CarPositionEventGenerator()
//                .withPositionEvent(positionEvent)
//                .withCarId(carId)
//                .generate()
//            producer.produce(
//                kafkaConfiguration.latestPositionEventsStream,
//                carPositionEvent.carId.value,
//                carPositionEvent
//            )
//        }
//
//        step("Geofence ${CarLeavedGeofence::class.simpleName} event created") {
//            consumer
//                .consume()
//                .skipUntil { msg ->
//                    // wait for message with key == carId
//                    msg.key().equals(carId.value) &&
//                            // and geofence description we created
//                            mapper
//                                .readValue(msg.value(), GeofenceCarEvent::class.java)
//                                .payload
//                                .geofence
//                                .description == geofenceDescription &&
//                            // and @type of payload is CarLeavedGeofence
//                            mapper
//                                .readValue(msg.value(), GeofenceCarEvent::class.java)
//                                .payload::class == CarLeavedGeofence::class
//                }
//                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
//                .test()
//                .assertNext {
//                    geofenceCarEvent = mapper
//                        .readValue(it.value(), GeofenceCarEvent::class.java)
//                    Assertions.assertEquals(
//                        CarLeavedGeofence::class.simpleName, geofenceCarEvent.payload::class.simpleName
//                    )
//                    Assertions.assertEquals(
//                        carId.value,
//                        geofenceCarEvent.payload.carId.value
//                    )
//                    Assertions.assertEquals(
//                        geozoneType, geofenceCarEvent.payload.geofence.type.value
//                    )
//                }
//                .thenCancel()
//                .log()
//                .verify()
//        }
//
//        step<SenderResult<String>?>("Produce car position event inside geofence") {
//            val navigation = NavigationGenerator()
//                .withLongitude(pointInsideDefaultZone.longitude)
//                .withLatitude(pointInsideDefaultZone.latitude)
//                .generate()
//            val positionEvent = PositionGenerator()
//                .withNavigation(navigation)
//                .generate()
//            val carPositionEvent = CarPositionEventGenerator()
//                .withPositionEvent(positionEvent)
//                .withCarId(carId)
//                .generate()
//            producer.produce(
//                kafkaConfiguration.latestPositionEventsStream,
//                carPositionEvent.carId.value,
//                carPositionEvent
//            )
//        }
//
//        step("Geofence ${CarEnteredGeofence::class.simpleName} event created") {
//            consumer
//                .consume()
//                .skipUntil { msg ->
//                    // wait for message with key == carId
//                    msg.key().equals(carId.value) &&
//                            // and geofence description we created
//                            mapper
//                                .readValue(msg.value(), GeofenceCarEvent::class.java)
//                                .payload
//                                .geofence
//                                .description == geofenceDescription &&
//                            // and @type of payload is CarLeavedGeofence
//                            mapper
//                                .readValue(msg.value(), GeofenceCarEvent::class.java)
//                                .payload::class == CarEnteredGeofence::class
//                }
//                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
//                .test()
//                .assertNext {
//                    geofenceCarEvent = mapper
//                        .readValue(it.value(), GeofenceCarEvent::class.java)
//                    Assertions.assertEquals(
//                        CarEnteredGeofence::class.simpleName, geofenceCarEvent.payload::class.simpleName
//                    )
//                    Assertions.assertEquals(
//                        carId.value,
//                        geofenceCarEvent.payload.carId.value
//                    )
//                    Assertions.assertEquals(geozoneType, geofenceCarEvent.payload.geofence.type.value)
//                }
//                .thenCancel()
//                .log()
//                .verify()
//        }
//
//    }
}
