package ru.belkacar.core.test

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
interface VehicleSensor {
    val name: String
    val value: Any
}

data class BooleanSensor(
    override val name: String,
    override val value: Any
) : VehicleSensor

data class FuelSensor(
    override val name: String,
    override val value: Any
) : VehicleSensor

data class MileageSensor(
    override val name: String,
    override val value: Any
) : VehicleSensor

data class EngineRpmSensor(
    override val name: String,
    override val value: Any
) : VehicleSensor

data class VehicleSpeedSensor(
    override val name: String,
    override val value: Any
) : VehicleSensor
