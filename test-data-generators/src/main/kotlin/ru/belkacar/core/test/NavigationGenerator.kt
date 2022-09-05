package ru.belkacar.core.test

class NavigationGenerator: ObjectGenerator<Navigation> {

    private val location = LocationGenerator().generate()

    private var altitude = 100.0
    private var course = 100.0
    private var glnSatellites = 1
    private var gpsSatellites = 1
    private var hdop = 100.0
    private var latitude = location.latitude
    private var longitude = location.longitude
    private var valid = true

    fun withAltitude(value: Double) { altitude = value }
    fun withCourse(value: Double) = apply { course = value }
    fun withGlnSatellites(value: Int) = apply { glnSatellites = value }
    fun withGpsSatellites(value: Int) = apply { gpsSatellites = value }
    fun withHdop(value: Double) = apply { hdop = value }
    fun withLatitude(value: Double) = apply { latitude = value }
    fun withLongitude(value: Double) = apply { longitude = value }
    fun withValid(value: Boolean) = apply { valid = value }

    override fun generate(): Navigation {
        return Navigation(
            altitude = altitude,
            course = course,
            glnSatellites = glnSatellites,
            gpsSatellites = gpsSatellites,
            hdop = hdop,
            latitude = latitude,
            longitude = longitude,
            valid = valid
        )
    }

    override fun generateMany(size: Int): List<Navigation> {
        return (0..size)
            .map { generate() }
        }

}