package ru.belkacar.core.test

import java.util.UUID
import kotlin.random.Random

class CarIdGenerator(private val clazz: Class<*>) : ObjectGenerator<CarId<*>> {
    override fun generate(): CarId<*> {
        return when (clazz) {
            UUID::class.java -> CarId.UUIDVal(UUID.randomUUID())
            Long::class.java -> CarId.LongVal(Random.nextLong())
            else -> throw IllegalStateException("There are only UUID and Long cardId")
        }
    }
}

fun carIdUuid() = generatorApply(CarIdGenerator(UUID::class.java)) {}
fun carIdUuid(size: Int) = generatorApply(CarIdGenerator(UUID::class.java), size) {}

fun carIdLong() = generatorApply(CarIdGenerator(Long::class.java)) {}
fun carIdLong(size: Int) = generatorApply(CarIdGenerator(Long::class.java), size) {}
