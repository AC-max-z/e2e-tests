package ru.belkacar.core.test

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.serpro69.kfaker.Faker
import io.qameta.allure.AllureId
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import proto.belka.telematics.geofence.v1.CreateGeofenceCommand
import proto.belka.telematics.geofence.v1.CreateGeofenceCommandKt
import proto.belka.telematics.geofence.v1.Geofence
import proto.belka.telematics.geofence.v1.type.CreateGeofenceTypeCommand
import proto.belka.telematics.geofence.v1.type.CreateGeofenceTypeCommandKt
import proto.belka.telematics.geofence.v1.type.GeofenceType
import reactor.kafka.sender.SenderResult
import ru.belkacar.core.test.steps.GeofenceSteps
import ru.belkacar.core.test.steps.GeofenceTypeSteps
import ru.belkacar.core.test.tools.E2E
import ru.belkacar.telematics.geofence.GeometryGenerator
import ru.belkacar.core.test.tools.JiraIssues
import ru.belkacar.core.test.tools.assertNextStep
import ru.belkacar.core.test.tools.expectErrorStep
import ru.belkacar.core.test.tools.step


@E2E
@SpringBootTest
class TestForTest @Autowired constructor(
    private val kafkaConfiguration: TelematicsServicesKafkaConfiguration,
    private val grpcClient: GrpcClient,
    private val producer: KafkaReactiveProducer<String, String>,
    @Qualifier("geofenceEventStreamConsumer")
    private val consumer: KafkaReactiveConsumer<String, String>
) {

    val faker = Faker()
    val geofenceTypeSteps = GeofenceTypeSteps(grpcClient)
    val geofenceSteps = GeofenceSteps(grpcClient)

    @Test
//    @AllureId("")
//    @JiraIssues("")
    fun `should create geofence event on entering polygon`() {

        val geofenceTypeResponse = step<CreateGeofenceTypeCommand.Response>("Create new geofence type") {
            val geofenceTypeKey = "driving_zone"
            val geofenceTypeDescription = "Autotests " + faker.howIMetYourMother.catchPhrase()
            geofenceTypeSteps
                .createGeofenceType(geofenceTypeKey, geofenceTypeDescription)
        }

        step<CreateGeofenceCommand.Response>("Create new geofence") {
            geofenceSteps.createGeofence(
                geofenceTypeResponse.geofenceType.key.value,
                geofenceTypeResponse.geofenceType.description.value,
                Geofence.Restriction.OWNER,
                GeometryGenerator().generate().toProto()
            )
        }

        step<SenderResult<String>?>("Produce car position event outside created geofence") {
            val navigation =
                NavigationGenerator()
                    .withLongitude(37.674522399902344)
                    .withLatitude(55.79944771620931)
                    .generate()
            val positionEvent = TelematicsPositionGenerator()
                .withNavigation(navigation)
                .generate()
            val carPositionEvent = TelematicsCarPositionEventGenerator()
                .withPositionEvent(positionEvent)
                .generate()
            ObjectMapper().writeValueAsString(positionEvent)
            producer.produce(
                kafkaConfiguration.latestPositionEventsStream,
                geofenceTypeResponse.geofenceType.key.value,
                carPositionEvent
            )
        }

        step<SenderResult<String>?>("Produce car position event inside created geofence") {
            val navigation = NavigationGenerator()
                .withLongitude(37.61392593383789)
                .withLatitude(55.7768626557418)
                .generate()
            val positionEvent = TelematicsPositionGenerator()
                .withNavigation(navigation)
                .generate()
            val carPositionEvent = TelematicsCarPositionEventGenerator()
                .withPositionEvent(positionEvent)
                .generate()
            ObjectMapper().writeValueAsString(positionEvent)
            producer.produce(
                kafkaConfiguration.latestPositionEventsStream,
                geofenceTypeResponse.geofenceType.key.value,
                carPositionEvent
            )
        }

//        step("Geofence event consumed") {
//            consumer
//                .consume()
//                .skipUntil { r -> r.key().equals(positionEvent.deviceImei) }
//                .test()
//                .assertNext {
//                    Assertions.assertEquals(expectedMessage, it.value())
//                    Assertions.assertEquals(positionEvent.deviceImei, it.key())
//                }
//                .thenCancel()
//                .log()
//                .verify()
//        }

    }
}