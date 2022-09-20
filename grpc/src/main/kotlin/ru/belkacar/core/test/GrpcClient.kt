package ru.belkacar.core.test

import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import org.springframework.stereotype.Component
import proto.belka.telematics.geofence.v1.GeofenceCommandOpsGrpc
import proto.belka.telematics.geofence.v1.GeofenceQueryOpsGrpc
import proto.belka.telematics.geofence.v1.type.GeofenceTypeCommandOpsGrpc
import proto.belka.telematics.geofence.v1.type.GeofenceTypeQueryOpsGrpc


@Component
class GrpcClient {

    val geofenceCommand by lazy {
        GeofenceCommandOpsGrpc
            .newBlockingStub(geofenceManagerPublicChannel)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(authHeaders()))
    }

    val geofenceQuery by lazy {
        GeofenceQueryOpsGrpc
            .newBlockingStub(geofenceManagerPublicChannel)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(authHeaders()))
    }

    val geofenceTypeQuery by lazy {
        GeofenceTypeQueryOpsGrpc
            .newBlockingStub(geofenceManagerPublicChannel)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(authHeaders()))
    }

    val geofenceTypeCommand by lazy {
        GeofenceTypeCommandOpsGrpc
            .newBlockingStub(geofenceManagerPublicChannel)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(authHeaders()))
    }

    private val geofenceManagerPublicChannel by lazy {
        ManagedChannelBuilder
            .forAddress("test-telematics.belkacar.ru", 6565)
            .usePlaintext()
            .build()
    }


    private fun authHeaders(): Metadata {
        var headers = Metadata()
        val userLoginKey = Metadata.Key.of("-x-belkacar-request-initiator-user-login", Metadata.ASCII_STRING_MARSHALLER)
        val userRoleKey = Metadata.Key.of("-x-belkacar-request-initiator-user-role", Metadata.ASCII_STRING_MARSHALLER)

        headers.put(userLoginKey, "E2E_AUTOTESTS")
        headers.put(userRoleKey, "SUPERUSER")

        return headers
    }

}