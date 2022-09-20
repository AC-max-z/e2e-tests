package ru.belkacar.core.test.steps


import org.springframework.beans.factory.annotation.Autowired
import proto.belka.telematics.geofence.Geometry
import proto.belka.telematics.geofence.v1.*
import proto.belka.telematics.geofence.v1.type.GeofenceType
import ru.belkacar.core.test.GrpcClient

class GeofenceSteps @Autowired constructor(
    private val grpcClient: GrpcClient,

) {

    private val geoQuery = grpcClient.geofenceQuery
    private val geoCommand = grpcClient.geofenceCommand


    fun findGeofencesByOwner(ownerLogin: String): FindByOwnerQuery.Response {
        return geoQuery.findByOwner(
            FindByOwnerQueryKt.request {
                this.geofenceOwner = Geofence.Owner.newBuilder().setLogin(ownerLogin).build()
            }
        )
    }


    fun findGeofencesByDefaultOwner(): FindByOwnerQuery.Response {
        return geoQuery
            .findByOwner(
                FindByOwnerQueryKt.request { }
            )
    }


    fun createGeofence(
        typeKey: String,
        description: String,
        restriction: Geofence.Restriction,
        geometry: Geometry
    ): CreateGeofenceCommand.Response {
        return geoCommand.createGeofence(
            CreateGeofenceCommandKt.request {
                this.geofenceTypeKey = GeofenceType.Key.newBuilder().setValue(typeKey).build()
                this.description = description
                this.restriction = restriction
                this.geometry = geometry
            }
        )

    }


    fun deleteGeofence(): DeleteGeofenceCommand.Response {
        return geoCommand.deleteGeofence(
            DeleteGeofenceCommandKt.request {  }
        )

    }


    fun updateGeofence(): UpdateGeofenceCommand.Response {
        return geoCommand.updateGeofence(
            UpdateGeofenceCommandKt.request {  }
        )

    }
}