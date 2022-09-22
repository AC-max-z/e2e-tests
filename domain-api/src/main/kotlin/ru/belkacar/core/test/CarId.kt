package ru.belkacar.core.test

import java.util.*

interface CarId<T> {
    val value: T
    data class UUIDVal(override val value: UUID) : CarId<UUID>
    data class LongVal(override val value: Long): CarId<Long>
}
