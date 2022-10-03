package ru.belkacar.core.test

import java.util.UUID

class CarIdGenerator() : ObjectGenerator<CarId> {
    override fun generate(): CarId {
        return UUIDVal(value = UUID.randomUUID().toString())
    }
}
