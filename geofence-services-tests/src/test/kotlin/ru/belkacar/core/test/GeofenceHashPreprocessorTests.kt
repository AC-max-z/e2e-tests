package ru.belkacar.core.test

import com.google.protobuf.StringValue
import ru.belkacar.core.test.tools.step
import io.qameta.allure.AllureId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import proto.belka.telematics.geofence.v1.CreateGeofenceCommandKt
import proto.belka.telematics.geofence.v1.DeleteGeofenceCommandKt
import proto.belka.telematics.geofence.v1.type.GeofenceTypeKt
import ru.belkacar.core.test.tools.E2E
import ru.belkacar.core.test.tools.JiraIssues
import proto.belka.telematics.geofence.v1.Geofence
import proto.belka.telematics.geofence.v1.UpdateGeofenceCommandKt
import reactor.kotlin.test.test
import ru.belkacar.core.test.tools.assertNextStep
import ru.belkacar.telematics.geofence.*
import ru.belkacar.telematics.geofence.CarEnteredGeofence
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertIs

@E2E
@SpringBootTest
class GeofenceHashPreprocessorTests @Autowired constructor(
    private val geofenceGrpcOperations: GeofencesGrpcOperations,
    private val geofenceKafkaOperations: GeofencesKafkaOperations,
) {
    val faker = FakerProvider.faker

    @Test
    @AllureId("")
    @JiraIssues("")
    fun `should do something on geofence create`() {
        val polygon = GeometryGenerator().generate()

        val geofence = step<Geofence>("Create new geofence") {
            geofenceGrpcOperations.geofenceOps
                .create(
                    CreateGeofenceCommandKt.request {
                        geofenceTypeKey = GeofenceTypeKt.key { value = "driving_zone" }
                        description = "Autotests ${faker.backToTheFuture.quotes()}"
                        restriction = Geofence.Restriction.OWNER
                        geometry = polygon.toProto()
                    }
                )
                .map { it.geofence }
                .block()!!
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

    @Test
    @AllureId("")
    @JiraIssues("")
    fun `should do something on geofence update`() {
        val polygon = GeometryGenerator().generate()
        val newDesc = "Autotests ${faker.bojackHorseman.quotes()}"

        val geofence = step<Geofence>("Create new geofence") {
            geofenceGrpcOperations.geofenceOps
                .create(
                    CreateGeofenceCommandKt.request {
                        geofenceTypeKey = GeofenceTypeKt.key { value = "driving_zone" }
                        description = "Autotests ${faker.backToTheFuture.quotes()}"
                        restriction = Geofence.Restriction.OWNER
                        geometry = polygon.toProto()
                    }
                )
                .map { it.geofence }
                .block()!!
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
            // solution? (wait for update)
            Thread.sleep(5_000)
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

    @Test
    @AllureId("")
    @JiraIssues("")
    fun `should do something on geofence delete`() {
        val polygon = GeometryGenerator().generate()

        val geofence = step<Geofence>("Create new geofence") {
            geofenceGrpcOperations.geofenceOps
                .create(
                    CreateGeofenceCommandKt.request {
                        geofenceTypeKey = GeofenceTypeKt.key { value = "driving_zone" }
                        description = "Autotests ${faker.backToTheFuture.quotes()}"
                        restriction = Geofence.Restriction.OWNER
                        geometry = polygon.toProto()
                    }
                )
                .map { it.geofence }
                .block()!!
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
            // solution? (wait for update)
            Thread.sleep(5_000)
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