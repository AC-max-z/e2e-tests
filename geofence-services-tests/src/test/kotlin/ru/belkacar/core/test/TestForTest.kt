package ru.belkacar.core.test

import com.google.protobuf.ByteString
import com.google.protobuf.StringValue
import io.github.serpro69.kfaker.Faker
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import proto.belka.telematics.geofence.Geometry
import proto.belka.telematics.geofence.v1.CreateGeofenceCommandKt
import proto.belka.telematics.geofence.v1.FindByOwnerQueryKt
import proto.belka.telematics.geofence.v1.Geofence
import proto.belka.telematics.geofence.v1.type.CreateGeofenceTypeCommandKt
import proto.belka.telematics.geofence.v1.type.GeofenceType
import ru.belkacar.core.test.steps.GeofenceSteps
import ru.belkacar.core.test.steps.GeofenceTypeSteps
import ru.belkacar.core.test.tools.E2E
import ru.belkacar.telematics.geofence.GeometryGenerator


@E2E
@SpringBootTest
class TestForTest @Autowired constructor(
    private val kafkaConfiguration: TelematicsServicesKafkaConfiguration,
    private val grpcClient: GrpcClient
) {

    val faker = Faker()
    val geofenceTypeSteps = GeofenceTypeSteps(grpcClient)
    val geofenceSteps = GeofenceSteps(grpcClient)

    @Test
    fun checkCarPosition() {
        val geofenceTypeKey = "Autotests " + faker.funnyName.name()
        val geofenceTypeDescription = "Autotests " + faker.howIMetYourMother.catchPhrase()

        val createGeofenceTypeResponse = geofenceTypeSteps.createGeofenceType(geofenceTypeKey, geofenceTypeDescription)

        val createGeofenceResponse = geofenceSteps.createGeofence(
            createGeofenceTypeResponse.geofenceType.key.value,
            geofenceTypeDescription,
            Geofence.Restriction.OWNER,
            GeometryGenerator().generate().toProto()
        )

        println(createGeofenceResponse)

    }


}