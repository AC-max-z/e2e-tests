package ru.belkacar.core.test

import com.google.protobuf.StringValue
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
import reactor.kotlin.test.test
import ru.belkacar.core.test.tools.JiraIssues
import proto.belka.telematics.geofence.v1.UpdateGeofenceCommandKt
import reactor.core.publisher.Mono.delay
import ru.belkacar.core.GeofenceHelpers
import ru.belkacar.core.test.tools.assertNextStep
import ru.belkacar.core.test.tools.step
import ru.belkacar.telematics.geofence.*
import java.time.Duration
import kotlin.test.assertEquals
import io.qameta.allure.Owner
import org.junit.jupiter.api.Tag

const val CONSUMER_TIMEOUT_MS = 10_000L
const val DELAY_VERIFICATION_MS = 2_000L

@SpringBootTest
@ServiceGroup("geofence-services")
@Service("telematics-geofence-hash-preprocessor")
@HashPreprocessor
class GeofenceHashPreprocessorTests @Autowired constructor(
    private val geofenceGrpcOperations: GeofencesGrpcOperations,
    private val geofenceKafkaOperations: GeofencesKafkaOperations,
    private val geofenceHelpers: GeofenceHelpers
) {
    val faker = FakerProvider.faker
    @AfterEach
    fun cleanup() {
        geofenceHelpers.deleteAllGeofencesByOwner()
    }

    @ComponentTest
    @DisplayName("Should produce AddGeofence command on geofence creation")
    @ParameterizedTest(name = "{displayName} (geofence_type: {0})")
    @ValueSource(strings = ["driving_zone", "police_impound"])
    @AllureId("9740")
    @JiraIssues("TEL-509")
    fun addGeofenceCommandOnCreation(zoneKey: String) {
        parameter("geofence_type: ", zoneKey)

        val geofence = step<Geofence>("Create new geofence") {
            geofenceHelpers.createGeofence(zoneKey)!!
        }

        step("New add geofence command") {
            geofenceKafkaOperations.consumerOps.consumeGeohashProcessorCommands()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext {
                    println("${it.key()} :: ${it.value()}")
                }
                .skipUntil { record ->
                    record.value()::class == `Command$AddGeofence`::class
                            && record.value().geofence!!.id.value.toString() == geofence.id.value.uuidString
                }
                .test()
                .assertNextStep("") { record ->
                    assertEquals(geofence.id.value.uuidString, record.value().geofence!!.id.value.toString())
                }
                .thenCancel()
                .log()
                .verify()
        }

    }

    @ComponentTest
    @DisplayName("Should produce UpdateGeofence command on geofence update (change description)")
    @ParameterizedTest(name = "{displayName} (geofence_type: {0})")
    @ValueSource(strings = ["driving_zone", "police_impound"])
    @AllureId("9776")
    @JiraIssues("TEL-509")
    fun updateGeofenceCommandOnDescriptionUpdate(zoneKey: String) {
        parameter("geofence_type: ", zoneKey)
        val newDesc = "Autotests updated description ${faker.bojackHorseman.quotes()}"

        val geofence = step<Geofence>("Create new geofence") {
            geofenceHelpers.createGeofence(zoneKey)!!
        }

        step("Update geofence") {
            geofenceGrpcOperations.geofenceOps
                .update(
                    UpdateGeofenceCommandKt.request {
                        geofenceId = geofence.id
                        description = StringValue.of(newDesc)
                    }
                )
                .map { it.geofence }
                .block()!!
            delay(Duration.ofMillis(DELAY_VERIFICATION_MS)).block()!!
        }

        step("New update geofence command") {
            geofenceKafkaOperations.consumerOps.consumeGeohashProcessorCommands()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext {
                    println("${it.key()} :: ${it.value()}")
                }
                .skipUntil { record ->
                    record.value()::class == `Command$UpdateGeofence`::class
                            && record.value().geofence!!.id.value.toString() == geofence.id.value.uuidString
                }
                .test()
                .assertNextStep("") { record ->
                    assertEquals(newDesc, record.value().geofence!!.description)
                }
                .thenCancel()
                .log()
                .verify()
        }

    }

    @ComponentTest
    @DisplayName("Should produce RemoveGeofence command on geofence deletion")
    @ParameterizedTest(name = "{displayName} (geofence_type: {0})")
    @ValueSource(strings = ["driving_zone", "police_impound"])
    @AllureId("9709")
    @JiraIssues("TEL-509")
    fun removeGeofenceCommandOnDelete(zoneKey: String) {
        parameter("geofence_type: ", zoneKey)

        val geofence = step<Geofence>("Create new geofence") {
            geofenceHelpers.createGeofence(zoneKey)!!
        }

        step("Delete geofence") {
            geofenceGrpcOperations.geofenceOps
                .delete(
                    DeleteGeofenceCommandKt.request {
                        geofenceId = geofence.id
                    }
                )
                .map { it }
                .block()!!
            delay(Duration.ofMillis(DELAY_VERIFICATION_MS)).block()!!
        }

        step("New remove geofence command") {
            geofenceKafkaOperations.consumerOps.consumeGeohashProcessorCommands()
                .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
                .doOnNext {
                    println("${it.key()} :: ${it.value()}")
                }
                .skipUntil { record ->
                    record.value()::class == `Command$RemoveGeofence`::class
                            && record.value().geofenceId!!.value.toString() == geofence.id.value.uuidString
                }
                .test()
                .assertNextStep("") {
                }
                .thenCancel()
                .log()
                .verify()
        }

    }

}