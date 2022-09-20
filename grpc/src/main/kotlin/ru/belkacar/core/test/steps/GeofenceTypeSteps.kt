package ru.belkacar.core.test.steps

import com.google.protobuf.StringValue

import org.springframework.beans.factory.annotation.Autowired
import proto.belka.telematics.geofence.v1.type.*
import ru.belkacar.core.test.GrpcClient

class GeofenceTypeSteps @Autowired constructor(
    private val grpcClient: GrpcClient
){

    private val geoTypeCommand = grpcClient.geofenceTypeCommand
    private val geoTypeQuery = grpcClient.geofenceTypeQuery


    fun createGeofenceType(key: String, description: String): CreateGeofenceTypeCommand.Response {
        return geoTypeCommand.createGeofenceType(
            CreateGeofenceTypeCommandKt.request {
                this.key = GeofenceType.Key.newBuilder().setValue(key).build()
                this.description = StringValue.of(description)
//                    this.attributes.add()
            }
        )
    }


    fun findAllGeofeceTypes(): FindAllQuery.Response {
        return geoTypeQuery.findAll(
            FindAllQueryKt.request { }
        )
    }

    fun findGeofeceTypeByKey(key: String): FindByKeyQuery.Response {
        return geoTypeQuery.findByKey(
            FindByKeyQueryKt.request {
                this.geofenceTypeKey = GeofenceType.Key.newBuilder().setValue(key).build()
            }
        )
    }

}