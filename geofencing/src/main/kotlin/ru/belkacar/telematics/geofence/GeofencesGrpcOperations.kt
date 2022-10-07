package ru.belkacar.telematics.geofence

import io.grpc.ManagedChannel
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Component
import proto.belka.telematics.geofence.v1.CreateGeofenceCommand
import proto.belka.telematics.geofence.v1.GeofenceCommandOpsGrpcKt
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import ru.belkacar.core.test.GrpcAuthProvider

@Component
class GeofencesGrpcOperations(
    private val telematicsGrpcChannel: ManagedChannel,
    private val authProvider: GrpcAuthProvider
) {
    
    private val _geofenceCommands = GeofenceCommandOpsGrpcKt.GeofenceCommandOpsCoroutineStub(telematicsGrpcChannel)
        .withInterceptors(authProvider.defaultAuthInterceptor.get())
    
    val geofenceOps : GeofenceOps by lazy {
        GeofenceOps()
    }
    
    val geofenceTypeOps : GeofenceTypeOps by lazy {
        GeofenceTypeOps()
    }
    
    
    inner class GeofenceOps {
        
        fun create(request: CreateGeofenceCommand.Request): Mono<CreateGeofenceCommand.Response> {
            return mono { _geofenceCommands.createGeofence(request) }
        }
    }
    
    
    inner class GeofenceTypeOps {
    
    }
}





