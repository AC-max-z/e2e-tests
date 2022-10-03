package ru.belkacar.core.test

import java.text.DecimalFormat
import java.util.*

class LocationGenerator: ObjectGenerator<Location> {

    private val df = DecimalFormat("#.######")

    private var latitude = df.parse(df.format(56.0 + (57.9 - 56.0) * Random().nextDouble())) as Double
    private var longitude = df.parse(df.format(64.0 + (65.9 - 64.0) * Random().nextDouble())) as Double

    fun withLatitude(value: Double) = also { latitude = value }
    fun withLongitude(value: Double) = also { longitude = value }

    override fun generate(): Location {
        return Location(
            latitude = latitude,
            longitude = longitude
        )
    }

    override fun generateMany(size: Int): List<Location> {
        return (0..size)
            .map { generate() }
    }

}