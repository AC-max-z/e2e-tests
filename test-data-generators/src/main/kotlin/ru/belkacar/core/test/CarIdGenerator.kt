package ru.belkacar.core.test

import java.util.UUID

class CarIdGenerator() : ObjectGenerator<CarId<*>> {
    override fun generate(): CarId<*> {
        return UUIDVal(value = UUID.randomUUID())
    }
}

fun generateCarId(builder: CarIdGenerator.() -> Unit): CarId<*> {
    return CarIdGenerator()
        .apply(builder)
        .generate()
}

fun generateCarId(size: Int, builder: CarIdGenerator.() -> Unit): List<CarId<*>> {
    return CarIdGenerator()
        .apply(builder)
        .generate(size)
}
