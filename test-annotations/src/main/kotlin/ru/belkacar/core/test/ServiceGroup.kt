package ru.belkacar.core.test

import org.junit.jupiter.api.Tag

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("geofence-services")
annotation class GeofenceServices()

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("broadcasting-services")
annotation class BroadcastingPlatform()