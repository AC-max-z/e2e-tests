package ru.belkacar.core.test

class NavigationGenerator : ObjectGenerator<Navigation> {
    
    private val location = LocationGenerator().generate()
    
    private var altitude = 100.0
    private var course = 100.0
    private var glnSatellites = 1
    private var gpsSatellites = 1
    private var hdop = 100.0
    private var latitude = { withLatitude }
    private var longitude = { withLongitude }
    private var valid = true
    
    var withLatitude = location.latitude
    var withLongitude = location.longitude
    
    fun withAltitude(value: Double) = also { altitude = value }
    fun withCourse(value: Double) = apply { course = value }
    fun withGlnSatellites(value: Int) = apply { glnSatellites = value }
    fun withGpsSatellites(value: Int) = apply { gpsSatellites = value }
    fun withHdop(value: Double) = apply { hdop = value }
    fun withValid(value: Boolean) = apply { valid = value }
    
    override fun generate(): Navigation {
        return Navigation(
            altitude = altitude,
            course = course,
            glnSatellites = glnSatellites,
            gpsSatellites = gpsSatellites,
            hdop = hdop,
            latitude = latitude(),
            longitude = longitude(),
            valid = valid
        )
    }
}

fun generateNavigationData(builder: NavigationGenerator.() -> Unit): Navigation {
    return NavigationGenerator()
        .apply(builder)
        .generate()
}
