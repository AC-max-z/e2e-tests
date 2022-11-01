package ru.belkacar.core.test

import java.text.DecimalFormat
import java.util.*

class LocationGenerator : ObjectGenerator<Location> {

    private val df = DecimalFormat("#.######")

    private var latitude = df.parse(df.format(56.0 + (57.9 - 56.0) * Random().nextDouble())) as Double
    private var longitude = df.parse(df.format(64.0 + (65.9 - 64.0) * Random().nextDouble())) as Double

    fun withLatitude(value: Double) = also { latitude = value }
    fun withLongitude(value: Double) = also { longitude = value }

    fun withPointType(pointType: PointType) =
        when (pointType) {
            PointType.INSIDE_DEFAULT_ZONE -> apply {
                latitude = 55.7768626557418
                longitude = 37.61392593383789
            }

            PointType.OUTSIDE_DEFAULT_ZONE -> apply {
                latitude = 55.79944771620931
                longitude = 37.674522399902344
            }

            PointType.INSIDE_SMALL_ZONE -> apply {
                latitude = 55.75600328396566
                longitude = 37.625770568847656
            }

            PointType.INSIDE_PINNED_OUT_ZONE -> apply {
                latitude = 55.72783509242637
                longitude = 37.60045051574707
            }
        }

    override fun generate(): Location {
        return Location(
            latitude = latitude,
            longitude = longitude
        )
    }

    override fun generate(size: Int): List<Location> {
        return (0..size)
            .map { generate() }
    }

    enum class PointType {
        INSIDE_DEFAULT_ZONE, OUTSIDE_DEFAULT_ZONE, INSIDE_SMALL_ZONE, INSIDE_PINNED_OUT_ZONE
    }

}