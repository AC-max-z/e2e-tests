package ru.belkacar.core.test

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(UUIDVal::class)
    ]
)
interface CarId {
    val value: String
}

data class UUIDVal(override val value: String) : CarId
