package ru.belkacar.core.test

import io.qameta.allure.AllureId
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import proto.belka.telematics.geofence.v1.CreateGeofenceCommandKt
import proto.belka.telematics.geofence.v1.DeleteGeofenceCommandKt
import proto.belka.telematics.geofence.v1.Geofence
import proto.belka.telematics.geofence.v1.UpdateGeofenceCommandKt
import proto.belka.telematics.geofence.v1.type.GeofenceTypeKt
import reactor.kotlin.test.test
import ru.belkacar.core.test.tools.JiraIssues
import ru.belkacar.core.test.tools.assertNextStep
import ru.belkacar.core.test.tools.step
import ru.belkacar.telematics.broadcasting.platform.BroadcastingPlatformKafkaOperations
import ru.belkacar.telematics.geofence.CarEnteredGeofence
import ru.belkacar.telematics.geofence.CarLeavedGeofence
import ru.belkacar.telematics.geofence.GeofencesGrpcOperations
import ru.belkacar.telematics.geofence.GeofencesKafkaOperations
import ru.belkacar.telematics.geofence.toProto
import ru.belkacar.telematics.geofence.toUUID
import java.time.Duration
import kotlin.test.assertIs

const val CONSUMER_TIMEOUT_MS = 55_000L

@SpringBootTest
@GeofenceServices
@Detector
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
    @ComponentTest
    @AllureId("7692")
    @JiraIssues("TEL-565", "TEL-767")
    fun `should create enter geofence event on entering polygon`(zoneKey: String) {
        val carId = generateCarId { }
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

        step("Produce car position event outside created geofence") {
            val carPositionEvent = generateCarPosition {
                withCarId = carId
                withPosition = generatePosition {
                    withNavigationData = generateNavigationData {
                        withLatitude = pointOutsideDefaultZone.latitude
                        withLongitude = pointOutsideDefaultZone.longitude
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

        step("Geofence detector state changed") {
            geofenceKafkaOperations.consumerOps.consumeGeofenceDetectorState()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext {
                    println("${it.key()} :: ${it.value()}")
                }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            &&
                            record.value()
                                .currentPoinGeofences
                                .any { i -> i.id.value.toString() == geofence.id.value.uuidString }
                }
                .test()
                .assertNextStep("Geofence detector state updated") { record ->
                    with(record.value()) {
                        assert(
                            lastPointGeofences.none { i -> i.id.value.toString() == geofence.id.value.uuidString }
                        )
                        assert(currentPoinGeofences
                            .filter { i -> i.id.value.toString() == geofence.id.value.uuidString }
                            .size == 1
                        )
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

        step("Geofence ${CarEnteredGeofence::class.simpleName} event created") {
            geofenceKafkaOperations.consumerOps.consumeCarGeofenceEvents()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext { println("${it.key()} :: ${it.value()}") }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            && record.value().payload.geofence.id.value == geofence.id.toUUID()
                }
                .test()
                .assertNextStep("Car enter geofence event created") { record ->
                    with(record.value()) {
                        assertIs<CarEnteredGeofence>(payload)
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

    }

    @ParameterizedTest
    @ValueSource(strings = ["police_impound", "driving_zone"])
    @ComponentTest
    @AllureId("7711")
    @JiraIssues("TEL-565", "TEL-767")
    fun `the first event from a car that is not located in any of the geofences`(zoneKey: String) {
        val carId = generateCarId { }
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

        step("Geofence detector state changed") {
            geofenceKafkaOperations.consumerOps.consumeGeofenceDetectorState()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext {
                    println("${it.key()} :: ${it.value()}")
                }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            &&
                            record.value()
                                .currentPoinGeofences
                                .any { i -> i.id.value.toString() == geofence.id.value.uuidString }
                }
                .test()
                .assertNextStep("Geofence detector state updated") { record ->
                    with(record.value()) {
                        assert(
                            lastPointGeofences.none { i -> i.id.value.toString() == geofence.id.value.uuidString }
                        )
                        assert(currentPoinGeofences
                            .filter { i -> i.id.value.toString() == geofence.id.value.uuidString }
                            .size == 1
                        )
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

        step("Geofence ${CarEnteredGeofence::class.simpleName} event created") {
            geofenceKafkaOperations.consumerOps.consumeCarGeofenceEvents()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext { println("${it.key()} :: ${it.value()}") }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            && record.value().payload.geofence.id.value == geofence.id.toUUID()
                }
                .test()
                .assertNextStep("") { record ->
                    with(record.value()) {
                        assertIs<CarEnteredGeofence>(payload)
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

    }

    @ParameterizedTest
    @ValueSource(strings = ["driving_zone", "police_impound"])
    @ComponentTest
    @AllureId("7693")
    @JiraIssues("TEL-565", "TEL-767")
    fun `should create leave geofence event on leaving polygon`(zoneKey: String) {
        val carId = generateCarId { }
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

        step("Produce car position event outside created geofence") {
            val carPositionEvent = generateCarPosition {
                withCarId = carId
                withPosition = generatePosition {
                    withNavigationData = generateNavigationData {
                        withLatitude = pointOutsideDefaultZone.latitude
                        withLongitude = pointOutsideDefaultZone.longitude
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

        step("Geofence detector state changed") {
            geofenceKafkaOperations.consumerOps.consumeGeofenceDetectorState()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext {
                    println("${it.key()} :: ${it.value()}")
                }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            &&
                            record.value()
                                .lastPointGeofences
                                .any { i -> i.id.value.toString() == geofence.id.value.uuidString }
                }
                .test()
                .assertNextStep("Geofence detector state updated") { record ->
                    with(record.value()) {
                        assert(
                            currentPoinGeofences
                                .none { i -> i.id.value.toString() == geofence.id.value.uuidString }
                        )
                        assert(lastPointGeofences
                            .filter { i -> i.id.value.toString() == geofence.id.value.uuidString }
                            .size == 1
                        )
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

        step("Geofence ${CarLeavedGeofence::class.simpleName} event created") {
            geofenceKafkaOperations.consumerOps.consumeCarGeofenceEvents()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext { println("${it.key()} :: ${it.value()}") }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            && record.value().payload::class == CarLeavedGeofence::class
                            && record.value().payload.geofence.id.value == geofence.id.toUUID()
                }
                .test()
                .assertNextStep("Car leave geofence event created") { record ->
                    with(record.value()) {
                        assertIs<CarLeavedGeofence>(payload)
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

    }

    @ParameterizedTest
    @ValueSource(strings = ["driving_zone", "police_impound"])
    @ComponentTest
    @AllureId("7708")
    @JiraIssues("TEL-565", "TEL-767")
    fun `should create enter geofence event after increasing polygon`(zoneKey: String) {
        val carId = generateCarId { }
        val initialPolygon = GeometryGenerator().generate()
        val updatedPolygon = GeometryGenerator().withType(GeometryType.BIGGER).generate()
        val carPositionEvent = generateCarPosition {
            withCarId = carId
            withPosition = generatePosition {
                withNavigationData = generateNavigationData {
                    withLatitude = pointOutsideDefaultZone.latitude
                    withLongitude = pointOutsideDefaultZone.longitude
                }
            }
        }

        val geofence = step<Geofence>("Create new geofence") {
            geofenceGrpcOperations.geofenceOps
                .create(
                    CreateGeofenceCommandKt.request {
                        geofenceTypeKey = GeofenceTypeKt.key { value = zoneKey }
                        description = "Autotests ${faker.backToTheFuture.quotes()}"
                        restriction = Geofence.Restriction.OWNER
                        geometry = initialPolygon.toProto()
                    }
                )
                .map { it.geofence }
                .block()!!
        }

        step("Produce car position event outside created geofence") {
            broadcastingPlatformKafkaOperations.producerOps
                .producerLatestCarPosition(carPositionEvent)
                .test()
                .assertNext {

                }
                .verifyComplete()
        }

        step("Expand created geofence (car position is inside new polygon)") {
            geofenceGrpcOperations.geofenceOps
                .update(
                    UpdateGeofenceCommandKt.request {
                        geofenceId = geofence.id
                        geometry = updatedPolygon.toProto()
                    }
                )
                .map { it.geofence }
                .block()!!
            // solution? (wait for update)
            Thread.sleep(5_000)
        }

        step("Produce same car position event") {
            broadcastingPlatformKafkaOperations.producerOps
                .producerLatestCarPosition(carPositionEvent)
                .test()
                .assertNext {

                }
                .verifyComplete()
        }

        step("Geofence detector state changed") {
            geofenceKafkaOperations.consumerOps.consumeGeofenceDetectorState()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext {
                    println("${it.key()} :: ${it.value()}")
                }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            &&
                            record.value()
                                .currentPoinGeofences
                                .any { i -> i.id.value.toString() == geofence.id.value.uuidString }
                }
                .test()
                .assertNextStep("Geofence detector state updated") { record ->
                    with(record.value()) {
                        assert(
                            lastPointGeofences.none { i -> i.id.value.toString() == geofence.id.value.uuidString }
                        )
                        assert(currentPoinGeofences
                            .filter { i -> i.id.value.toString() == geofence.id.value.uuidString }
                            .size == 1
                        )
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

        step("Geofence ${CarEnteredGeofence::class.simpleName} event created") {
            geofenceKafkaOperations.consumerOps.consumeCarGeofenceEvents()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext {
                    println("${it.key()} :: ${it.value()}")
                    println("Geofence ID in event :: waiting for")
                    println("${it.value().payload.geofence.id.value} :: ${geofence.id.value.uuidString}")
                }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            && record.value().payload.geofence.id.value == geofence.id.toUUID()
                }
                .test()
                .assertNextStep("") { record ->
                    with(record.value()) {
                        assertIs<CarEnteredGeofence>(payload)
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

    }

    @ParameterizedTest
    @ValueSource(strings = ["driving_zone", "police_impound"])
    @ComponentTest
    @AllureId("7709")
    @JiraIssues("TEL-565", "TEL-767")
    fun `should create leave geofence event after decreasing polygon`(zoneKey: String) {
        val carId = generateCarId { }
        val initialPolygon = GeometryGenerator().withType(GeometryType.BIGGER).generate()
        val updatedPolygon = GeometryGenerator().withType(GeometryType.LESSER).generate()
        val carPositionEvent = generateCarPosition {
            withCarId = carId
            withPosition = generatePosition {
                withNavigationData = generateNavigationData {
                    withLatitude = pointInsideDefaultZone.latitude
                    withLongitude = pointInsideDefaultZone.longitude
                }
            }
        }

        val geofence = step<Geofence>("Create new geofence (bigger)") {
            geofenceGrpcOperations.geofenceOps
                .create(
                    CreateGeofenceCommandKt.request {
                        geofenceTypeKey = GeofenceTypeKt.key { value = zoneKey }
                        description = "Autotests ${faker.backToTheFuture.quotes()}"
                        restriction = Geofence.Restriction.OWNER
                        geometry = initialPolygon.toProto()
                    }
                )
                .map { it.geofence }
                .block()!!
        }

        step("Produce car position event inside created geofence") {
            broadcastingPlatformKafkaOperations.producerOps
                .producerLatestCarPosition(carPositionEvent)
                .test()
                .assertNext {

                }
                .verifyComplete()
        }

        step("Shrink created geofence (to a lesser)") {
            geofenceGrpcOperations.geofenceOps
                .update(
                    UpdateGeofenceCommandKt.request {
                        geofenceId = geofence.id
                        geometry = updatedPolygon.toProto()
                    }
                )
                .map { it.geofence }
                .block()!!
            Thread.sleep(5_000)
        }

        step("Produce same car position event") {
            broadcastingPlatformKafkaOperations.producerOps
                .producerLatestCarPosition(carPositionEvent)
                .test()
                .assertNext {

                }
                .verifyComplete()
        }

        step("Geofence detector state changed") {
            geofenceKafkaOperations.consumerOps.consumeGeofenceDetectorState()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext {
                    println("${it.key()} :: ${it.value()}")
                }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            &&
                            record.value()
                                .lastPointGeofences
                                .any { i -> i.id.value.toString() == geofence.id.value.uuidString }
                }
                .test()
                .assertNextStep("Geofence detector state updated") { record ->
                    with(record.value()) {
                        assert(
                            currentPoinGeofences
                                .none { i -> i.id.value.toString() == geofence.id.value.uuidString }
                        )
                        assert(lastPointGeofences
                            .filter { i -> i.id.value.toString() == geofence.id.value.uuidString }
                            .size == 1
                        )
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

        step("Geofence ${CarLeavedGeofence::class.simpleName} event created") {
            geofenceKafkaOperations.consumerOps.consumeCarGeofenceEvents()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext { println("${it.key()} :: ${it.value()}") }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            && record.value().payload::class == CarLeavedGeofence::class
                            && record.value().payload.geofence.id.value == geofence.id.toUUID()
                }
                .test()
                .assertNextStep("Car leave geofence event created") { record ->
                    with(record.value()) {
                        assertIs<CarLeavedGeofence>(payload)
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

    }

    @ParameterizedTest
    @ValueSource(strings = ["driving_zone", "police_impound"])
    @ComponentTest
    @AllureId("9401")
    @JiraIssues("TEL-565", "TEL-767")
    fun `should create leave geofence event after geofence deletion`(zoneKey: String) {
        val carId = generateCarId { }
        val polygon = GeometryGenerator().generate()
        val carPositionEvent = generateCarPosition {
            withCarId = carId
            withPosition = generatePosition {
                withNavigationData = generateNavigationData {
                    withLatitude = pointInsideDefaultZone.latitude
                    withLongitude = pointInsideDefaultZone.longitude
                }
            }
        }

        val geofence = step<Geofence>("Create new geofence (default)") {
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
            broadcastingPlatformKafkaOperations.producerOps
                .producerLatestCarPosition(carPositionEvent)
                .test()
                .assertNext {

                }
                .verifyComplete()
        }

        step("Delete created geofence") {
            geofenceGrpcOperations.geofenceOps
                .delete(
                    DeleteGeofenceCommandKt.request {
                        geofenceId = geofence.id
                    }
                )
                .block()!!
            Thread.sleep(5_000)
        }

        step("The car resends coordinates") {
            broadcastingPlatformKafkaOperations.producerOps
                .producerLatestCarPosition(carPositionEvent)
                .test()
                .assertNext {

                }
                .verifyComplete()
        }

        step("Geofence detector state changed") {
            geofenceKafkaOperations.consumerOps.consumeGeofenceDetectorState()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext {
                    println("${it.key()} :: ${it.value()}")
                }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            &&
                            record.value()
                                .lastPointGeofences
                                .any { i -> i.id.value.toString() == geofence.id.value.uuidString }
                }
                .test()
                .assertNextStep("Geofence detector state updated") { record ->
                    with(record.value()) {
                        assert(
                            currentPoinGeofences
                                .none { i -> i.id.value.toString() == geofence.id.value.uuidString }
                        )
                        assert(lastPointGeofences
                            .filter { i -> i.id.value.toString() == geofence.id.value.uuidString }
                            .size == 1
                        )
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

        step("Geofence ${CarLeavedGeofence::class.simpleName} event created") {
            geofenceKafkaOperations.consumerOps.consumeCarGeofenceEvents()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext { println("${it.key()} :: ${it.value()}") }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            && record.value().payload::class == CarLeavedGeofence::class
                            && record.value().payload.geofence.id.value == geofence.id.toUUID()
                }
                .test()
                .assertNextStep("Car leave geofence event created") { record ->
                    with(record.value()) {
                        assertIs<CarLeavedGeofence>(payload)
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

    }

    @Test
    @ComponentTest
    @AllureId("9402")
    @JiraIssues("TEL-565", "TEL-767")
    fun `should create enter event for embedded geofence`() {
        val carId = generateCarId { }
        val zoneKey = "driving_zone"
        val outerPolygon = GeometryGenerator().generate()
        val innerPolygon = GeometryGenerator().withType(GeometryType.LESSER).generate()

        val outerCarPositionEvent = generateCarPosition {
            withCarId = carId
            withPosition = generatePosition {
                withNavigationData = generateNavigationData {
                    withLatitude = pointInsideDefaultZone.latitude
                    withLongitude = pointInsideDefaultZone.longitude
                }
            }
        }
        val innerCarPositionEvent = generateCarPosition {
            withCarId = carId
            withPosition = generatePosition {
                withNavigationData = generateNavigationData {
                    withLatitude = pointInsideSmallZone.latitude
                    withLongitude = pointInsideSmallZone.longitude
                }
            }
        }
        val outsideCarPositionEvent = generateCarPosition {
            withCarId = carId
            withPosition = generatePosition {
                withNavigationData = generateNavigationData {
                    withLatitude = pointOutsideDefaultZone.latitude
                    withLongitude = pointOutsideDefaultZone.longitude
                }
            }
        }

        val outerGeofence = step<Geofence>("Create new geofence (default)") {
            geofenceGrpcOperations.geofenceOps
                .create(
                    CreateGeofenceCommandKt.request {
                        geofenceTypeKey = GeofenceTypeKt.key { value = zoneKey }
                        description = "Autotests ${faker.backToTheFuture.quotes()}"
                        restriction = Geofence.Restriction.OWNER
                        geometry = outerPolygon.toProto()
                    }
                )
                .map { it.geofence }
                .block()!!
        }

        val innerGeofence = step<Geofence>("Create new geofence (lesser)") {
            geofenceGrpcOperations.geofenceOps
                .create(
                    CreateGeofenceCommandKt.request {
                        geofenceTypeKey = GeofenceTypeKt.key { value = zoneKey }
                        description = "Autotests ${faker.backToTheFuture.quotes()}"
                        restriction = Geofence.Restriction.OWNER
                        geometry = innerPolygon.toProto()
                    }
                )
                .map { it.geofence }
                .block()!!
        }

        step("Produce car position event in outer+out inner geofence") {
            broadcastingPlatformKafkaOperations.producerOps
                .producerLatestCarPosition(outerCarPositionEvent)
                .test()
                .assertNext {

                }
                .verifyComplete()
        }

        step("Geofence detector state changed (enter outer geofence)") {
            geofenceKafkaOperations.consumerOps.consumeGeofenceDetectorState()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext {
                    println("${it.key()} :: ${it.value()}")
                }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            &&
                            record.value()
                                .currentPoinGeofences
                                .any { i -> i.id.value.toString() == outerGeofence.id.value.uuidString }
                }
                .test()
                .assertNextStep("Geofence detector state updated") { record ->
                    with(record.value()) {
                        assert(
                            lastPointGeofences.none { i -> i.id.value.toString() == outerGeofence.id.value.uuidString }
                        )
                        assert(currentPoinGeofences
                            .filter { i -> i.id.value.toString() == outerGeofence.id.value.uuidString }
                            .size == 1
                        )
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

        step("Geofence ${CarEnteredGeofence::class.simpleName} event created (enter outer geofence)") {
            geofenceKafkaOperations.consumerOps.consumeCarGeofenceEvents()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext {
                    println("${it.key()} :: ${it.value()}")
                }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            && record.value().payload.geofence.id.value == outerGeofence.id.toUUID()
                }
                .test()
                .assertNextStep("") { record ->
                    with(record.value()) {
                        assertIs<CarEnteredGeofence>(payload)
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

        step("Produce car position event inside inner geofence") {
            broadcastingPlatformKafkaOperations.producerOps
                .producerLatestCarPosition(innerCarPositionEvent)
                .test()
                .assertNext {

                }
                .verifyComplete()
        }

        step("Geofence detector state changed (enter inner geofence)") {
            geofenceKafkaOperations.consumerOps.consumeGeofenceDetectorState()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext {
                    println("${it.key()} :: ${it.value()}")
                }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            &&
                            record.value()
                                .currentPoinGeofences
                                .any { i -> i.id.value.toString() == innerGeofence.id.value.uuidString }
                }
                .test()
                .assertNextStep("Geofence detector state updated") { record ->
                    with(record.value()) {
                        assert(
                            lastPointGeofences.none { i -> i.id.value.toString() == innerGeofence.id.value.uuidString }
                        )
                        assert(currentPoinGeofences
                            .filter { i -> i.id.value.toString() == innerGeofence.id.value.uuidString }
                            .size == 1
                        )
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

        step("Geofence ${CarEnteredGeofence::class.simpleName} event created (enter inner geofence)") {
            geofenceKafkaOperations.consumerOps.consumeCarGeofenceEvents()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext {
                    println("${it.key()} :: ${it.value()}")
                }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            && record.value().payload.geofence.id.value == innerGeofence.id.toUUID()
                }
                .test()
                .assertNextStep("") { record ->
                    with(record.value()) {
                        assertIs<CarEnteredGeofence>(payload)
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

        step("Produce car position event in outer+out inner geofence") {
            broadcastingPlatformKafkaOperations.producerOps
                .producerLatestCarPosition(outerCarPositionEvent)
                .test()
                .assertNext {

                }
                .verifyComplete()
        }

        step("Geofence detector state changed (leave inner geofence)") {
            geofenceKafkaOperations.consumerOps.consumeGeofenceDetectorState()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext {
                    println("${it.key()} :: ${it.value()}")
                }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            &&
                            record.value()
                                .lastPointGeofences
                                .any { i -> i.id.value.toString() == innerGeofence.id.value.uuidString }
                }
                .test()
                .assertNextStep("Geofence detector state updated") { record ->
                    with(record.value()) {
                        assert(
                            currentPoinGeofences
                                .none { i -> i.id.value.toString() == innerGeofence.id.value.uuidString }
                        )
                        assert(lastPointGeofences
                            .filter { i -> i.id.value.toString() == innerGeofence.id.value.uuidString }
                            .size == 1
                        )
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

        step("Geofence ${CarLeavedGeofence::class.simpleName} event created (leave inner geofence)") {
            geofenceKafkaOperations.consumerOps.consumeCarGeofenceEvents()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext { println("${it.key()} :: ${it.value()}") }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            && record.value().payload::class == CarLeavedGeofence::class
                            && record.value().payload.geofence.id.value == innerGeofence.id.toUUID()
                }
                .test()
                .assertNextStep("Car leave geofence event created") { record ->
                    with(record.value()) {
                        assertIs<CarLeavedGeofence>(payload)
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

        step("Produce car position event outside outer geofence") {
            broadcastingPlatformKafkaOperations.producerOps
                .producerLatestCarPosition(outsideCarPositionEvent)
                .test()
                .assertNext {

                }
                .verifyComplete()
        }

        step("Geofence detector state changed (leave outer geofence)") {
            geofenceKafkaOperations.consumerOps.consumeGeofenceDetectorState()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext {
                    println("${it.key()} :: ${it.value()}")
                }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            &&
                            record.value()
                                .currentPoinGeofences
                                .none { i -> i.id.value.toString() == outerGeofence.id.value.uuidString }
                }
                .test()
                .assertNextStep("Geofence detector state updated") { record ->
                    with(record.value()) {
                        assert(
                            currentPoinGeofences
                                .none { i -> i.id.value.toString() == outerGeofence.id.value.uuidString }
                        )
                        assert(lastPointGeofences
                            .filter { i -> i.id.value.toString() == outerGeofence.id.value.uuidString }
                            .size == 1
                        )
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

        step("Geofence ${CarLeavedGeofence::class.simpleName} event created (leave outer geofence)") {
            geofenceKafkaOperations.consumerOps.consumeCarGeofenceEvents()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext { println("${it.key()} :: ${it.value()}") }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            && record.value().payload::class == CarLeavedGeofence::class
                            && record.value().payload.geofence.id.value == outerGeofence.id.toUUID()
                }
                .test()
                .assertNextStep("Car leave geofence event created") { record ->
                    with(record.value()) {
                        assertIs<CarLeavedGeofence>(payload)
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

    }

    @Test
    @ComponentTest
    @AllureId("9403")
    @JiraIssues("TEL-565", "TEL-767")
    fun `should create enter event for pinned out geofence`() {
        val carId = generateCarId { }
        val zoneKey = "driving_zone"
        val polygon = GeometryGenerator().withType(GeometryType.PINNED_OUT).generate()

        val geofenceCarPositionEvent = generateCarPosition {
            withCarId = carId
            withPosition = generatePosition {
                withNavigationData = generateNavigationData {
                    withLatitude = pointInsideSmallZone.latitude
                    withLongitude = pointInsideSmallZone.longitude
                }
            }
        }
        val geofenceCarPositionEvent2 = generateCarPosition {
            withCarId = carId
            withPosition = generatePosition {
                withNavigationData = generateNavigationData {
                    withLatitude = pointInsideDefaultZone.latitude
                    withLongitude = pointInsideDefaultZone.longitude
                }
            }
        }
        val pinnedOutCarPositionEvent = generateCarPosition {
            withCarId = carId
            withPosition = generatePosition {
                withNavigationData = generateNavigationData {
                    withLatitude = pointInsidePinnedOutZone.latitude
                    withLongitude = pointInsidePinnedOutZone.longitude
                }
            }
        }
        val outsideCarPositionEvent = generateCarPosition {
            withCarId = carId
            withPosition = generatePosition {
                withNavigationData = generateNavigationData {
                    withLatitude = pointOutsideDefaultZone.latitude
                    withLongitude = pointOutsideDefaultZone.longitude
                }
            }
        }

        val pinnedOutGeofence = step<Geofence>("Create new geofence (pinned-out)") {
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

        step("Produce car position event outside geofence") {
            broadcastingPlatformKafkaOperations.producerOps
                .producerLatestCarPosition(outsideCarPositionEvent)
                .test()
                .assertNext {

                }
                .verifyComplete()
        }

        step("Produce car position event inside geofence") {
            broadcastingPlatformKafkaOperations.producerOps
                .producerLatestCarPosition(geofenceCarPositionEvent)
                .test()
                .assertNext {

                }
                .verifyComplete()
        }

        step("Geofence detector state changed") {
            geofenceKafkaOperations.consumerOps.consumeGeofenceDetectorState()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext {
                    println("${it.key()} :: ${it.value()}")
                }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            &&
                            record.value()
                                .currentPoinGeofences
                                .any { i -> i.id.value.toString() == pinnedOutGeofence.id.value.uuidString }
                }
                .test()
                .assertNextStep("Geofence detector state updated") { record ->
                    with(record.value()) {
                        assert(
                            lastPointGeofences.none { i -> i.id.value.toString() == pinnedOutGeofence.id.value.uuidString }
                        )
                        assert(currentPoinGeofences
                            .filter { i -> i.id.value.toString() == pinnedOutGeofence.id.value.uuidString }
                            .size == 1
                        )
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

        step("Geofence ${CarEnteredGeofence::class.simpleName} event created") {
            geofenceKafkaOperations.consumerOps.consumeCarGeofenceEvents()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext {
                    println("${it.key()} :: ${it.value()}")
                }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            && record.value().payload.geofence.id.value == pinnedOutGeofence.id.toUUID()
                }
                .test()
                .assertNextStep("") { record ->
                    with(record.value()) {
                        assertIs<CarEnteredGeofence>(payload)
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

        step("Produce car position event in pinned-out zone") {
            broadcastingPlatformKafkaOperations.producerOps
                .producerLatestCarPosition(pinnedOutCarPositionEvent)
                .test()
                .assertNext {

                }
                .verifyComplete()
        }

        step("Geofence detector state changed") {
            geofenceKafkaOperations.consumerOps.consumeGeofenceDetectorState()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext {
                    println("${it.key()} :: ${it.value()}")
                }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            &&
                            record.value()
                                .lastPointGeofences
                                .any { i -> i.id.value.toString() == pinnedOutGeofence.id.value.uuidString }
                }
                .test()
                .assertNextStep("Geofence detector state updated") { record ->
                    with(record.value()) {
                        assert(
                            currentPoinGeofences
                                .none { i -> i.id.value.toString() == pinnedOutGeofence.id.value.uuidString }
                        )
                        assert(lastPointGeofences
                            .filter { i -> i.id.value.toString() == pinnedOutGeofence.id.value.uuidString }
                            .size == 1
                        )
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

        step("Geofence ${CarLeavedGeofence::class.simpleName} event created") {
            geofenceKafkaOperations.consumerOps.consumeCarGeofenceEvents()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext { println("${it.key()} :: ${it.value()}") }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            && record.value().payload::class == CarLeavedGeofence::class
                            && record.value().payload.geofence.id.value == pinnedOutGeofence.id.toUUID()
                }
                .test()
                .assertNextStep("Car leave geofence event created") { record ->
                    with(record.value()) {
                        assertIs<CarLeavedGeofence>(payload)
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

        step("Produce car position event inside geofence") {
            broadcastingPlatformKafkaOperations.producerOps
                .producerLatestCarPosition(geofenceCarPositionEvent2)
                .test()
                .assertNext {

                }
                .verifyComplete()
        }

        step("Geofence detector state changed") {
            geofenceKafkaOperations.consumerOps.consumeGeofenceDetectorState()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext {
                    println("${it.key()} :: ${it.value()}")
                }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            &&
                            record.value()
                                .currentPoinGeofences
                                .any { i -> i.id.value.toString() == pinnedOutGeofence.id.value.uuidString }
                }
                .test()
                .assertNextStep("Geofence detector state updated") { record ->
                    with(record.value()) {
                        assert(
                            lastPointGeofences.none { i -> i.id.value.toString() == pinnedOutGeofence.id.value.uuidString }
                        )
                        assert(currentPoinGeofences
                            .filter { i -> i.id.value.toString() == pinnedOutGeofence.id.value.uuidString }
                            .size == 1
                        )
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

        step("Geofence ${CarEnteredGeofence::class.simpleName} event created") {
            geofenceKafkaOperations.consumerOps.consumeCarGeofenceEvents()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext {
                    println("${it.key()} :: ${it.value()}")
                }
                .skipUntil { record ->
                    record.key() == carId.value.toString()
                            && record.value().payload.geofence.id.value == pinnedOutGeofence.id.toUUID()
                }
                .test()
                .assertNextStep("") { record ->
                    with(record.value()) {
                        assertIs<CarEnteredGeofence>(payload)
                    }
                }
                .thenCancel()
                .log()
                .verify()
        }

    }
}
