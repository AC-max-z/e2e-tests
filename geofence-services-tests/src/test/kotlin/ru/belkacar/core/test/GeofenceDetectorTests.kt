package ru.belkacar.core.test

import io.qameta.allure.Allure
import io.qameta.allure.Allure.parameter
import io.qameta.allure.AllureId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import proto.belka.telematics.geofence.v1.DeleteGeofenceCommandKt
import proto.belka.telematics.geofence.v1.Geofence
import proto.belka.telematics.geofence.v1.UpdateGeofenceCommandKt
import ru.belkacar.core.test.tools.E2E
import ru.belkacar.core.test.tools.JiraIssues
import ru.belkacar.core.test.tools.step
import ru.belkacar.core.CarPositionsHelpers
import ru.belkacar.core.GeofenceHelpers
import ru.belkacar.telematics.geofence.CarEnteredGeofence
import ru.belkacar.telematics.geofence.CarLeavedGeofence
import ru.belkacar.telematics.geofence.GeofencesGrpcOperations
import ru.belkacar.telematics.geofence.toProto

@SpringBootTest
@E2E
@ServiceGroup("geofence-services")
@Service("telematics-geofence-detector")
@Detector
class GeofenceDetectorTests @Autowired constructor(
    private val geofenceGrpcOperations: GeofencesGrpcOperations,
    private val geofenceHelpers: GeofenceHelpers,
    private val carPositionsHelpers: CarPositionsHelpers
) {
    @AfterEach
    fun cleanup() {
        geofenceHelpers.deleteAllGeofencesByOwner()
    }

    @DisplayName("Should create enter geofence event on entering polygon")
    @ParameterizedTest(name = "{displayName} (geofence_type: {0})")
    @ValueSource(strings = ["driving_zone", "police_impound"])
    @AllureId("7692")
    @JiraIssues("TEL-565", "TEL-767")
    fun enterEventOnEnterPolygon(zoneKey: String) {
        parameter("geofence_type: ", zoneKey)
        val carId = generateCarId { }

        val geofence = step<Geofence>("Create new geofence") {
            geofenceHelpers.createGeofence(zoneKey)!!
        }

        step("Produce car position event outside created geofence") {
            carPositionsHelpers.produceCarPositionWithPointType(
                carId,
                LocationGenerator.PointType.OUTSIDE_DEFAULT_ZONE
            )
        }

        step("Produce car position event inside created geofence") {
            carPositionsHelpers.produceCarPositionWithPointType(
                carId,
                LocationGenerator.PointType.INSIDE_DEFAULT_ZONE
            )
        }

        step("Geofence detector state changed") {
            geofenceHelpers.checkGeofenceDetectorStateEntered(geofence, carId)
        }

        step("Geofence ${CarEnteredGeofence::class.simpleName} event created") {
            geofenceHelpers.checkEnterGeofenceEventCreated(carId, geofence)
        }

    }

    @DisplayName("Should create enter geofence event on first event inside polygon")
    @ParameterizedTest(name = "{displayName} (geofence_type: {0})")
    @ValueSource(strings = ["police_impound", "driving_zone"])
    @AllureId("7711")
    @JiraIssues("TEL-565", "TEL-767")
    fun enterEventOnFirstEventInsidePolygon(zoneKey: String) {
        parameter("geofence_type: ", zoneKey)
        val carId = generateCarId { }

        val geofence = step<Geofence>("Create new geofence") {
            geofenceHelpers.createGeofence(zoneKey)!!
        }

        step("Produce car position event inside created geofence") {
            carPositionsHelpers.produceCarPositionWithPointType(
                carId,
                LocationGenerator.PointType.INSIDE_DEFAULT_ZONE
            )
        }

        step("Geofence detector state changed") {
            geofenceHelpers.checkGeofenceDetectorStateEntered(geofence, carId)
        }

        step("Geofence ${CarEnteredGeofence::class.simpleName} event created") {
            geofenceHelpers.checkEnterGeofenceEventCreated(carId, geofence)
        }

    }

    @DisplayName("Should create leave geofence event on leaving polygon")
    @ParameterizedTest(name = "{displayName} (geofence_type: {0})")
    @ValueSource(strings = ["driving_zone", "police_impound"])
    @AllureId("7693")
    @JiraIssues("TEL-565", "TEL-767")
    fun leaveEventOnExitPolygon(zoneKey: String) {
        parameter("geofence_type: ", zoneKey)
        val carId = generateCarId { }

        val geofence = step<Geofence>("Create new geofence") {
            geofenceHelpers.createGeofence(zoneKey)!!
        }

        step("Produce car position event inside created geofence") {
            carPositionsHelpers.produceCarPositionWithPointType(
                carId,
                LocationGenerator.PointType.INSIDE_DEFAULT_ZONE
            )
        }

        step("Produce car position event outside created geofence") {
            carPositionsHelpers.produceCarPositionWithPointType(carId, LocationGenerator.PointType.OUTSIDE_DEFAULT_ZONE)
        }

        step("Geofence detector state changed") {
            geofenceHelpers.checkGeofenceDetectorStateLeft(geofence, carId)
        }

        step("Geofence ${CarLeavedGeofence::class.simpleName} event created") {
            geofenceHelpers.checkLeaveGeofenceEventCreated(carId, geofence)
        }

    }

    @DisplayName("Should create enter geofence event after increasing polygon")
    @ParameterizedTest(name = "{displayName} (geofence_type: {0})")
    @ValueSource(strings = ["driving_zone", "police_impound"])
    @AllureId("7708")
    @JiraIssues("TEL-565", "TEL-767")
    fun enterEventOnIncreasingPolygon(zoneKey: String) {
        parameter("geofence_type: ", zoneKey)
        val carId = generateCarId { }
        val updatedPolygon = GeometryGenerator().withType(GeometryType.BIGGER).generate()

        val geofence = step<Geofence>("Create new geofence") {
            geofenceHelpers.createGeofence(zoneKey)!!
        }

        step("Produce car position event outside created geofence") {
            carPositionsHelpers.produceCarPositionWithPointType(carId, LocationGenerator.PointType.OUTSIDE_DEFAULT_ZONE)
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
                .doOnSuccess {
                    val predicate = { g: Geofence -> g.geometry == updatedPolygon.toProto() }
                    geofenceHelpers.verifyGeofenceUpdated(geofence, predicate)
                }
                .block()!!
        }

        step("Produce same car position event") {
            carPositionsHelpers.produceCarPositionWithPointType(carId, LocationGenerator.PointType.OUTSIDE_DEFAULT_ZONE)
        }

        step("Geofence detector state changed") {
            geofenceHelpers.checkGeofenceDetectorStateEntered(geofence, carId)
        }

        step("Geofence ${CarEnteredGeofence::class.simpleName} event created") {
            geofenceHelpers.checkEnterGeofenceEventCreated(carId, geofence)
        }

    }

    @DisplayName("Should create leave geofence event after decreasing polygon")
    @ParameterizedTest(name = "{displayName} (geofence_type: {0})")
    @ValueSource(strings = ["driving_zone", "police_impound"])
    @AllureId("7709")
    @JiraIssues("TEL-565", "TEL-767")
    fun leaveEventOnDecreasingPolygon(zoneKey: String) {
        parameter("geofence_type: ", zoneKey)
        val carId = generateCarId { }
        val updatedPolygon = GeometryGenerator().withType(GeometryType.LESSER).generate()

        val geofence = step<Geofence>("Create new geofence (bigger)") {
            geofenceHelpers.createGeofence(zoneKey, GeometryType.BIGGER)!!
        }

        step("Produce car position event inside created geofence") {
            carPositionsHelpers.produceCarPositionWithPointType(carId, LocationGenerator.PointType.INSIDE_DEFAULT_ZONE)
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
                .doOnSuccess {
                    val predicate = { g: Geofence -> g.geometry == updatedPolygon.toProto() }
                    geofenceHelpers.verifyGeofenceUpdated(geofence, predicate)
                }
                .block()!!
        }

        step("Produce same car position event") {
            carPositionsHelpers.produceCarPositionWithPointType(carId, LocationGenerator.PointType.INSIDE_DEFAULT_ZONE)
        }

        step("Geofence detector state changed") {
            geofenceHelpers.checkGeofenceDetectorStateLeft(geofence, carId)
        }

        step("Geofence ${CarLeavedGeofence::class.simpleName} event created") {
            geofenceHelpers.checkLeaveGeofenceEventCreated(carId, geofence)
        }

    }

    @DisplayName("Should create leave geofence event after geofence deletion")
    @ParameterizedTest(name = "{displayName} (geofence_type: {0})")
    @ValueSource(strings = ["driving_zone", "police_impound"])
    @AllureId("9401")
    @JiraIssues("TEL-565", "TEL-767")
    fun leaveEventOnDeletingPolygon(zoneKey: String) {
        parameter("geofence_type: ", zoneKey)
        val carId = generateCarId { }

        val geofence = step<Geofence>("Create new geofence (default)") {
            geofenceHelpers.createGeofence(zoneKey)!!
        }

        step("Produce car position event inside created geofence") {
            carPositionsHelpers.produceCarPositionWithPointType(carId)
        }

        step("Delete created geofence") {
            geofenceGrpcOperations.geofenceOps
                .delete(
                    DeleteGeofenceCommandKt.request {
                        geofenceId = geofence.id
                    }
                )
                .doOnSuccess {
                    geofenceHelpers.verifyGeofenceDeleted(geofence)
                }
                .block()!!
        }

        step("The car resends coordinates") {
            carPositionsHelpers.produceCarPositionWithPointType(carId)
        }

        step("Geofence detector state changed") {
            geofenceHelpers.checkGeofenceDetectorStateLeft(geofence, carId)
        }

        step("Geofence ${CarLeavedGeofence::class.simpleName} event created") {
            geofenceHelpers.checkLeaveGeofenceEventCreated(carId, geofence)
        }

    }

    @Test
    @AllureId("9402")
    @JiraIssues("TEL-565", "TEL-767")
    @DisplayName("Should create enter event for embedded geofence")
    fun enterEventForEmbeddedPolygon() {
        val carId = generateCarId { }
        val zoneKey = "driving_zone"

        val outerGeofence = step<Geofence>("Create new geofence (default)") {
            geofenceHelpers.createGeofence(zoneKey)!!
        }

        val innerGeofence = step<Geofence>("Create new geofence (lesser)") {
            geofenceHelpers.createGeofence(zoneKey, GeometryType.LESSER)!!
        }

        step("Produce car position event in outer+out inner geofence") {
            carPositionsHelpers.produceCarPositionWithPointType(carId)
        }

        step("Geofence detector state changed (enter outer geofence)") {
            geofenceHelpers.checkGeofenceDetectorStateEntered(outerGeofence, carId)
        }

        step("Geofence ${CarEnteredGeofence::class.simpleName} event created (enter outer geofence)") {
            geofenceHelpers.checkEnterGeofenceEventCreated(carId, outerGeofence)
        }

        step("Produce car position event inside inner geofence") {
            carPositionsHelpers.produceCarPositionWithPointType(carId, LocationGenerator.PointType.INSIDE_SMALL_ZONE)
        }

        step("Geofence detector state changed (enter inner geofence)") {
            geofenceHelpers.checkGeofenceDetectorStateEntered(innerGeofence, carId)
        }

        step("Geofence ${CarEnteredGeofence::class.simpleName} event created (enter inner geofence)") {
            geofenceHelpers.checkEnterGeofenceEventCreated(carId, innerGeofence)
        }

        step("Produce car position event in outer+out inner geofence") {
            carPositionsHelpers.produceCarPositionWithPointType(carId, LocationGenerator.PointType.INSIDE_DEFAULT_ZONE)
        }

        step("Geofence detector state changed (leave inner geofence)") {
            geofenceHelpers.checkGeofenceDetectorStateLeft(innerGeofence, carId)
        }

        step("Geofence ${CarLeavedGeofence::class.simpleName} event created (leave inner geofence)") {
            geofenceHelpers.checkLeaveGeofenceEventCreated(carId, innerGeofence)
        }

        step("Produce car position event outside outer geofence") {
            carPositionsHelpers.produceCarPositionWithPointType(carId, LocationGenerator.PointType.OUTSIDE_DEFAULT_ZONE)
        }

        step("Geofence detector state changed (leave outer geofence)") {
            geofenceHelpers.checkGeofenceDetectorStateLeft(outerGeofence, carId)
        }

        step("Geofence ${CarLeavedGeofence::class.simpleName} event created (leave outer geofence)") {
            geofenceHelpers.checkLeaveGeofenceEventCreated(carId, outerGeofence)
        }

    }

    @Test
    @AllureId("9403")
    @JiraIssues("TEL-565", "TEL-767")
    @DisplayName("Should create enter event for pinned out geofence")
    fun enterEventForPinnedOutPolygon() {
        val carId = generateCarId { }
        val zoneKey = "driving_zone"

        val pinnedOutGeofence = step<Geofence>("Create new geofence (pinned-out)") {
            geofenceHelpers.createGeofence(zoneKey, GeometryType.PINNED_OUT)!!
        }

        step("Produce car position event outside geofence") {
            carPositionsHelpers.produceCarPositionWithPointType(carId, LocationGenerator.PointType.OUTSIDE_DEFAULT_ZONE)
        }

        step("Produce car position event inside geofence") {
            carPositionsHelpers.produceCarPositionWithPointType(carId, LocationGenerator.PointType.INSIDE_SMALL_ZONE)
        }

        step("Geofence detector state changed") {
            geofenceHelpers.checkGeofenceDetectorStateEntered(pinnedOutGeofence, carId)
        }

        step("Geofence ${CarEnteredGeofence::class.simpleName} event created") {
            geofenceHelpers.checkEnterGeofenceEventCreated(carId, pinnedOutGeofence)
        }

        step("Produce car position event in pinned-out zone") {
            carPositionsHelpers.produceCarPositionWithPointType(
                carId,
                LocationGenerator.PointType.INSIDE_PINNED_OUT_ZONE
            )
        }

        step("Geofence detector state changed") {
            geofenceHelpers.checkGeofenceDetectorStateLeft(pinnedOutGeofence, carId)
        }

        step("Geofence ${CarLeavedGeofence::class.simpleName} event created") {
            geofenceHelpers.checkLeaveGeofenceEventCreated(carId, pinnedOutGeofence)
        }

        step("Produce car position event inside geofence") {
            carPositionsHelpers.produceCarPositionWithPointType(carId, LocationGenerator.PointType.INSIDE_DEFAULT_ZONE)
        }

        step("Geofence detector state changed") {
            geofenceHelpers.checkGeofenceDetectorStateEntered(pinnedOutGeofence, carId)
        }

        step("Geofence ${CarEnteredGeofence::class.simpleName} event created") {
            geofenceHelpers.checkEnterGeofenceEventCreated(carId, pinnedOutGeofence)
        }

    }
}
