package ru.belkacar.core.test
//
//import com.google.protobuf.StringValue
//import io.github.serpro69.kfaker.Faker
//import org.junit.jupiter.api.Assertions
//import org.junit.jupiter.api.Assertions.assertFalse
//import org.junit.jupiter.api.Assertions.assertTrue
//import org.junit.jupiter.api.Test
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.context.SpringBootTest
//import proto.belka.telematics.geofence.v1.type.CreateGeofenceTypeCommandKt
//import proto.belka.telematics.geofence.v1.type.GeofenceType
//import proto.belka.telematics.geofence.v1.type.geofenceType
//import ru.belkacar.core.test.steps.GeofenceTypeSteps
//import ru.belkacar.core.test.tools.E2E
//
//
//@E2E
//@SpringBootTest
//class GeofenceTypeTests @Autowired constructor(
//    private val grpcClient: GrpcClient
//) {
//
//    val faker = Faker()
//    val geofenceTypeSteps = GeofenceTypeSteps(grpcClient)
//
//    @Test
//    fun createGeofenceTypeAllCases() {
//        val geofenceTypeKey = "Autotests " + faker.funnyName.name()
//        val geofenceTypeDescription = "Autotests " + faker.howIMetYourMother.catchPhrase()
//        val nonexistentGeofenceType = "Autotests non existent " + faker.funnyName.name()
//        val expectedGeofenceTypeResponse = CreateGeofenceTypeCommandKt.response {
//            this.geofenceType = geofenceType {
//                this.key = GeofenceType.Key.newBuilder().setValue(geofenceTypeKey).build()
//                this.description = StringValue.of(geofenceTypeDescription)
//            }
//        }
//
//        val createGeofenceTypeResponse = geofenceTypeSteps.createGeofenceType(geofenceTypeKey, geofenceTypeDescription)
//        Assertions.assertEquals(expectedGeofenceTypeResponse, createGeofenceTypeResponse)
//
//        val repeatedCreateGeofenceTypeResponse = geofenceTypeSteps.createGeofenceType(geofenceTypeKey, geofenceTypeDescription)
//        assertTrue(repeatedCreateGeofenceTypeResponse.hasGeofenceTypeKeyAlreadyExistError(),
//            "Expect error hasGeofenceTypeKeyAlreadyExistError()")
//
//        // https://belka-car.atlassian.net/browse/TEL-703 - тут пока падает
//        assertFalse(repeatedCreateGeofenceTypeResponse.geofenceTypeKeyAlreadyExistError.message.contains("SQL"),
//            "HasGeofenceTypeKeyAlreadyExistError() contains SQL code: " + repeatedCreateGeofenceTypeResponse.geofenceTypeKeyAlreadyExistError.message)
//
//        val findAllGeofenceTypeResponse = geofenceTypeSteps.findAllGeofeceTypes()
//
//        assertTrue(findAllGeofenceTypeResponse.geofenceTypesCount >= 1,
//            "Geofence type count is 0")
//        assertTrue(findAllGeofenceTypeResponse
//            .geofenceTypesList
//            .contains(expectedGeofenceTypeResponse.geofenceType),
//            "Creataed geofenceTyne not found"
//        )
//
//        val findGeofenceTypeByKeyResponse = geofenceTypeSteps.findGeofeceTypeByKey(geofenceTypeKey)
//
//        Assertions.assertEquals(
//            expectedGeofenceTypeResponse.geofenceType,
//            findGeofenceTypeByKeyResponse.geofenceType,
//            "Not found geofenceType by id"
//        )
//
//        val findNonexistentGeofenceTypeByKeyResponse = geofenceTypeSteps.findGeofeceTypeByKey(nonexistentGeofenceType)
//
//        assertTrue(findNonexistentGeofenceTypeByKeyResponse.hasGeofenceTypeNotFoundError())
//        assertFalse(findNonexistentGeofenceTypeByKeyResponse.geofenceTypeNotFoundError.message.contains("SQL"),
//            "HasGeofenceTypeKeyAlreadyExistError() contains SQL code: " + findNonexistentGeofenceTypeByKeyResponse.geofenceTypeNotFoundError.message)
//    }
//
//}