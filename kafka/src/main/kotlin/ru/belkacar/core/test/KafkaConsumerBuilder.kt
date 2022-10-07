package ru.belkacar.core.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.stereotype.Component
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import java.util.Collections

@Component
class KafkaConsumerBuilder(
    private val kafkaProperties: KafkaProperties
) {
    private val _objectMapper = ObjectMapper().apply {
        findAndRegisterModules()
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
    
    private val _defaultKeyDeserializer: Deserializer<String> = StringDeserializer()
    
    fun <T> create(): Builder<T> {
        return Builder(kafkaProperties)
    }
    
    inner class Builder<T> internal constructor(
        private val kafkaProperties: KafkaProperties
    ) {
        private lateinit var topicName: String
        private lateinit var valueType: Class<T>
        
        private var valueDeserializer: () -> Deserializer<T> = {
            Deserializer { _, data -> _objectMapper.readValue(data, valueType) }
        }
        
        private var keyDeserializer: () -> Deserializer<String> = { _defaultKeyDeserializer }
        
        private var offsetResetPolicy: () -> OffsetResetPolicy = { OffsetResetPolicy.latest }
        
        fun withValueType(type: Class<T>): Builder<T> = this.apply { valueType = type }
        
        fun withTopic(topic: String): Builder<T> = this.apply { topicName = topic }
        
        fun withKeyDeserializer(deserializer: Deserializer<String>): Builder<T> = this.apply {
            keyDeserializer = { deserializer }
        }
        
        fun withValueDeserializer(deserializer: Deserializer<T>): Builder<T> = this.apply {
            valueDeserializer = { deserializer }
        }
        
        fun withOffsetResetPolicy(policy: OffsetResetPolicy): Builder<T> = this.apply {
            offsetResetPolicy = { policy }
        }
        
        fun build(): KafkaReceiver<String, T> {
            with(kafkaProperties) {
                assert(bootstrapServers.isNotEmpty()) {
                    "kafka bootstrap servers property is empty"
                }
                assert(consumer.groupId.isNotBlank()) {
                    "kafka group id is blank "
                }
            }
            
            
            val options = ReceiverOptions.create<String, T>(kafkaProperties.buildConsumerProperties())
                .withKeyDeserializer(this@Builder.keyDeserializer())
                .withValueDeserializer(this@Builder.valueDeserializer())
                .consumerProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetResetPolicy().toString())
                .subscription(Collections.singleton(topicName))
            
            return KafkaReceiver.create(options)
        }
    }
    
    enum class OffsetResetPolicy {
        latest, earliest
    }
}