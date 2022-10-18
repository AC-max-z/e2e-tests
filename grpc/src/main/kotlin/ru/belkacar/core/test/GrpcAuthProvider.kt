package ru.belkacar.core.test

import com.google.common.base.Supplier
import io.grpc.ClientInterceptor
import io.grpc.stub.MetadataUtils
import io.grpc.Metadata

class GrpcAuthProvider {
    
    val defaultAuthInterceptor: Supplier<ClientInterceptor> = Supplier {
        MetadataUtils.newAttachHeadersInterceptor(AUTH_METADATA)
    }
    
    companion object {
        private const val LOGIN_HEADER_NAME = "-x-belkacar-request-initiator-user-login"
        private const val ROLE_HEADER_NAME = "-x-belkacar-request-initiator-user-role"
        
        private val AUTH_METADATA = Metadata().apply {
            put(Metadata.Key.of(LOGIN_HEADER_NAME, Metadata.ASCII_STRING_MARSHALLER), "E2E_AUTOTESTS")
            put(Metadata.Key.of(ROLE_HEADER_NAME, Metadata.ASCII_STRING_MARSHALLER), "SUPERUSER")
        }
    }
}