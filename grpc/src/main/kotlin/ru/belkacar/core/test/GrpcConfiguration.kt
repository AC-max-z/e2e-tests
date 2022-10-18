package ru.belkacar.core.test


import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class GrpcConfiguration {
    
    @Bean
    fun telematicsGrpcChannel(): ManagedChannel {
        return ManagedChannelBuilder
            .forAddress("test-telematics.belkacar.ru", 6565)
            .usePlaintext()
            .build()
    }
    
    @Bean
    fun authProvider(): GrpcAuthProvider {
        return GrpcAuthProvider()
    }
}
