package ru.belkacar.core

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import proto.belka.telematics.geofence.v1.CreateGeofenceCommandKt
import proto.belka.telematics.geofence.v1.DeleteGeofenceCommandKt
import proto.belka.telematics.geofence.v1.FindByOwnerQueryKt
import proto.belka.telematics.geofence.v1.Geofence
import proto.belka.telematics.geofence.v1.type.GeofenceTypeKt
import reactor.kotlin.test.test
import ru.belkacar.core.test.*
import ru.belkacar.core.test.tools.assertNextStep
import ru.belkacar.telematics.geofence.*
import ru.belkacar.telematics.geofence.CarEnteredGeofence
import ru.belkacar.telematics.geofence.CarLeavedGeofence
import java.time.Duration
import kotlin.jvm.Throws
import kotlin.test.assertIs

const val CONSUMER_TIMEOUT_MS = 10_000L
const val DELAY_RETRY_MS = 1_000L

@Component
class GeofenceHelpers @Autowired constructor(
    private val geofenceGrpcOperations: GeofencesGrpcOperations,
    private val geofenceKafkaOperations: GeofencesKafkaOperations
) {
    private val faker = FakerProvider.faker
    private val logger = org.slf4j.LoggerFactory.getLogger("Geofence-helpers")
    private val defaultGeofenceOwner = Geofence.Owner.newBuilder().setLogin("E2E_AUTOTESTS").build()

    @Throws(java.lang.Exception::class)
    fun createGeofence(
        zoneType: String,
        geometryType: GeometryType = GeometryType.DEFAULT,
        geofenceDescription: String = "Autotests ${faker.backToTheFuture.quotes()}"
    ): Geofence? {
        logger.info("Trying to create new geofence with type \"$zoneType\" and description: \"$geofenceDescription\"...")
        val geofence = Common()
            .executeWithRetry(
                10,
                {
                    geofenceGrpcOperations.geofenceOps
                        .create(
                            CreateGeofenceCommandKt.request {
                                geofenceTypeKey = GeofenceTypeKt.key { value = zoneType }
                                description = geofenceDescription
                                restriction = Geofence.Restriction.OWNER
                                geometry = GeometryGenerator().withType(geometryType).generate().toProto()
                            }
                        )
                        .map { it.geofence }
                        .block()!!
                },
                logger
            )!!
        verifyGeofenceCreated(geofence)
        return geofence
    }

    private fun verifyGeofenceCreated(geofence: Geofence): Boolean {
        val predicate = { geofenceList: List<Geofence>? -> geofenceList?.contains(geofence) ?: false }
        logger.info("Checking if geofence (${geofence.id.value.uuidString}) was successfully created...")
        val geofenceList = Common()
            .executeWithRetry(
                10, {
                    geofenceGrpcOperations
                        .geofenceOps
                        .getByOwner(
                            FindByOwnerQueryKt.request {
                                geofenceOwner = defaultGeofenceOwner
                            }
                        )
                        .map { it.geofencesList }
                        .block()!!
                },
                logger,
                predicate
            )

        if (predicate(geofenceList)) return true
        else throw NoSuchElementException("Geofence with id ${geofence.id} not created!")
    }

    private fun checkGeofenceDetectorStateChange(
        skipCondition: (record: ConsumerRecord<String, GeofenceDetectorKafkaState>) -> Boolean,
        checks: (record: ConsumerRecord<String, GeofenceDetectorKafkaState>) -> Unit
    ) {
        geofenceKafkaOperations.consumerOps.consumeGeofenceDetectorState()
            .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
            .doOnNext {
                logger.info("Consumed message from Geofence Detector State topic:")
                logger.info("${it.key()} :: ${it.value()}")
            }
            .skipUntil { skipCondition(it) }
            .test()
            .assertNextStep("Geofence detector state updated") { checks(it) }
            .then { logger.info("Expected message successfully consumed!") }
            .thenCancel()
            .log()
            .verify()
    }

    fun checkGeofenceDetectorStateLeft(geofence: Geofence, carId: CarId<*>) {
        logger.info("Checking that geofence detector state has changed (with timeout=$CONSUMER_TIMEOUT_MS ms)...")
        logger.info("(waiting for msg with key=$carId and geofence with id=${geofence.id.value.uuidString} present in lastPointGeofences and absent in currentPoinGeofences)")

        fun skipCondition(record: ConsumerRecord<String, GeofenceDetectorKafkaState>): Boolean {
            return record.key() == carId.value.toString()
                    && GeofenceMatchers.DetectorState.LastPointGeofences()
                .hasAtLeastOneGeofence(geofence, record.value())
                    && GeofenceMatchers.DetectorState.CurrentPointGeofences().hasNoGeofence(geofence, record.value())
        }

        fun checks(record: ConsumerRecord<String, GeofenceDetectorKafkaState>) {
            return with(record) {
                assert(GeofenceMatchers.DetectorState.CurrentPointGeofences().hasNoGeofence(geofence, value()))
                assert(GeofenceMatchers.DetectorState.LastPointGeofences().hasExactlyOneGeofence(geofence, value()))
            }
        }

        checkGeofenceDetectorStateChange(::skipCondition, ::checks)
    }

    fun checkGeofenceDetectorStateEntered(geofence: Geofence, carId: CarId<*>) {
        logger.info("Checking that geofence detector state has changed (with timeout=$CONSUMER_TIMEOUT_MS ms)...")
        logger.info("(waiting for msg with key=$carId and geofence with id=${geofence.id.value.uuidString} present in currentPoinGeofences and absent in lastPointGeofences)")

        fun skipCondition(record: ConsumerRecord<String, GeofenceDetectorKafkaState>): Boolean {
            return record.key() == carId.value.toString()
                    && GeofenceMatchers.DetectorState.CurrentPointGeofences()
                .hasAtLeastOneGeofence(geofence, record.value())
        }

        fun checks(record: ConsumerRecord<String, GeofenceDetectorKafkaState>) {
            return with(record) {
                assert(GeofenceMatchers.DetectorState.LastPointGeofences().hasNoGeofence(geofence, value()))
                assert(GeofenceMatchers.DetectorState.CurrentPointGeofences().hasExactlyOneGeofence(geofence, value()))
            }
        }

        checkGeofenceDetectorStateChange(::skipCondition, ::checks)
    }

    fun checkGeofenceEventCreated(
        skipCondition: (record: ConsumerRecord<String, CarGeofenceEventKafkaMessage>) -> Boolean,
        checks: (record: ConsumerRecord<String, CarGeofenceEventKafkaMessage>) -> Unit
    ) {
        geofenceKafkaOperations.consumerOps.consumeCarGeofenceEvents()
            .timeout(Duration.ofMillis(CONSUMER_TIMEOUT_MS))
            .doOnNext {
                logger.info("Consumed message from Geofence Car Events topic:")
                logger.info("${it.key()} :: ${it.value()}")
            }
            .skipUntil {
                skipCondition(it)
            }
            .test()
            .assertNextStep("Car enter geofence event created") { checks(it) }
            .then { logger.info("Expected message successfully consumed!") }
            .thenCancel()
            .log()
            .verify()
    }

    fun checkEnterGeofenceEventCreated(carId: CarId<*>, geofence: Geofence) {
        logger.info("Checking that ${CarEnteredGeofence::class.java.simpleName} event created in Geofence Car Events topic ")

        fun skipCondition(record: ConsumerRecord<String, CarGeofenceEventKafkaMessage>): Boolean {
            return record.key() == carId.value.toString()
                    && record.value().payload.geofence.id.value == geofence.id.toUUID()
                    && record.value().payload::class.java == CarEnteredGeofence::class.java
        }

        fun checks(record: ConsumerRecord<String, CarGeofenceEventKafkaMessage>) {
            return with(record) {
                assertIs<CarEnteredGeofence>(value().payload)
            }
        }

        checkGeofenceEventCreated(::skipCondition, ::checks)
    }

    fun checkLeaveGeofenceEventCreated(carId: CarId<*>, geofence: Geofence) {
        logger.info("Checking that ${CarLeavedGeofence::class.java.simpleName} event created in Geofence Car Events topic ")

        fun skipCondition(record: ConsumerRecord<String, CarGeofenceEventKafkaMessage>): Boolean {
            return record.key() == carId.value.toString()
                    && record.value().payload.geofence.id.value == geofence.id.toUUID()
                    && record.value().payload::class.java == CarLeavedGeofence::class.java
        }

        fun checks(record: ConsumerRecord<String, CarGeofenceEventKafkaMessage>) {
            return with(record) {
                assertIs<CarLeavedGeofence>(value().payload)
            }
        }

        checkGeofenceEventCreated(::skipCondition, ::checks)

    }

    fun verifyGeofenceUpdated(geofence: Geofence, conditionPredicate: (Geofence) -> Boolean): Boolean {
        logger.info("Checking if geofence (${geofence.id.value.uuidString}) was successfully updated...")
        val predicateOnResult = { geofenceList: List<Geofence>? ->
            geofenceList?.find { i -> i.id == geofence.id }?.let { conditionPredicate(it) }
                ?: false
        }
        val geofenceList = Common()
            .executeWithRetry(
                10,
                {
                    geofenceGrpcOperations
                        .geofenceOps
                        .getByOwner(
                            FindByOwnerQueryKt.request {
                                geofenceOwner = defaultGeofenceOwner
                            }
                        )
                        .map { it.geofencesList }
                        .block()!!
                },
                logger,
                predicateOnResult
            )

        if (predicateOnResult(geofenceList)) return true
        else throw NoSuchElementException("Geofence with id ${geofence.id} not updated!")
    }

    fun verifyGeofenceDeleted(geofence: Geofence): Boolean {
        logger.info("Checking if geofence (${geofence.id.value.uuidString}) was successfully deleted...")
        val predicate = { geofenceList: List<Geofence>? -> geofenceList?.none { i -> i.id == geofence.id } ?: false }
        val geofenceList = Common().executeWithRetry(
            10,
            {
                geofenceGrpcOperations
                    .geofenceOps
                    .getByOwner(
                        FindByOwnerQueryKt.request {
                            geofenceOwner = defaultGeofenceOwner
                        }
                    )
                    .map { it.geofencesList }
                    .block()!!
            },
            logger,
            predicate
        )

        if (predicate(geofenceList)) return true
        else throw NoSuchElementException("Geofence with id ${geofence.id} not deleted!")
    }

    fun deleteAllGeofencesByOwner(owner: Geofence.Owner = defaultGeofenceOwner) {
        val geofenceList =
            geofenceGrpcOperations.geofenceOps
                .getByOwner(
                    FindByOwnerQueryKt.request {
                        geofenceOwner = owner
                    }
                )
                .map { it.geofencesList }
                .block()!!

        geofenceList.forEach { geofence -> deleteGeofence(geofence) }
    }

    fun deleteGeofence(geofence: Geofence) {
        geofenceGrpcOperations.geofenceOps
            .delete(DeleteGeofenceCommandKt.request {
                geofenceId = geofence.id
            })
            .block()!!
    }

}