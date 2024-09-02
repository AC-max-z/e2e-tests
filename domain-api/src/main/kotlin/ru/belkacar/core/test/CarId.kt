package ru.belkacar.core.test

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(UUIDVal::class)
    ]
)
interface CarId<T> {
    val value: T
}

data class UUIDVal(override val value: UUID) : CarId<UUID>
