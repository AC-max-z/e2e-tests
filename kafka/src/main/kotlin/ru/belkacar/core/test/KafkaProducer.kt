package ru.belkacar.core.test

import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

interface KafkaProducer {

    fun produce(message: JvmType.Object)

}