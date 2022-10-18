package ru.belkacar.core.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.stereotype.Component
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions

@Component
class KafkaProducerBuilder(
    private val kafkaProperties: KafkaProperties
) {
    private val _objectMapper = ObjectMapper().apply {
        findAndRegisterModules()
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
    
    fun create(): KafkaSender<String, String> = Builder().build()
    
    inner class Builder {
        
        
        fun build(): KafkaSender<String, String> {
            with(kafkaProperties) {
                assert(bootstrapServers.isNotEmpty())
            }
            
            val options = SenderOptions.create<String, String>(kafkaProperties.buildProducerProperties())
                .withKeySerializer(StringSerializer())
                .withValueSerializer(StringSerializer())
            
            return KafkaSender.create(options)
        }
    }
}
