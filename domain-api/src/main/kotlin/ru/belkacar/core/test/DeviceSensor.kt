package ru.belkacar.core.test

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
interface DeviceSensor {
    val name: String
    val value: Any
}
