package ru.belkacar.core.test

import io.grpc.ManagedChannelBuilder
import proto.belka.telematics.geofence.v1.GeofenceQueryOpsGrpc

class GrcpClient {

    private val geofenceManagerPublicChannel by lazy {
        ManagedChannelBuilder
            .forAddress("test-telematics.belkacar.ru", 6565)
            .usePlaintext()
            .build()
    }

    val geofenceManagerQueryStub by lazy {
        GeofenceQueryOpsGrpc.newBlockingStub(geofenceManagerPublicChannel)
    }

}