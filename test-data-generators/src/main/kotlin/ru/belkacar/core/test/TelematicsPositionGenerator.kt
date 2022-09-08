package ru.belkacar.core.test

import java.time.LocalDateTime
import java.util.*

class TelematicsPositionGenerator: ObjectGenerator<TelematicsPositionEventStream> {

    private var deviceId = (1000..9999).random()
    private var deviceImei = UUID.randomUUID().toString()
    private var deviceSensors: List<Any> = emptyList()
    private var fixTime = LocalDateTime.now().toString()
    private var id = System.currentTimeMillis().toInt()
    private var navigation = NavigationGenerator().generate()
    private var serverTime = LocalDateTime.now().toString() // без мимлисекунд
    private var vehicleSensors: List<VehicleSensor> = emptyList()

    fun withDeviceId(id: Int) { deviceId = id }
    fun withDeviceImei(imei: String) { deviceImei = imei }
    fun withDeviceSensors(sensors: List<Any>) { deviceSensors = sensors }
    fun withFixTime(time: String) { fixTime = time }
    fun withId(positionId: Int) { id = positionId }
    fun withNavigation(navigationData: Navigation) { navigation = navigationData }
    fun withServerTime(time: String) { serverTime = time }
    fun withVehicleSensors(sensors: List<VehicleSensor>) { vehicleSensors = sensors }

    override fun generate(): TelematicsPositionEventStream {
        return TelematicsPositionEventStream(
            deviceId = deviceId,
            deviceImei = deviceImei,
            deviceSensors = deviceSensors,
            fixTime = fixTime,
            id = id,
            navigation = navigation,
            serverTime = serverTime,
            vehicleSensors = vehicleSensors
        )
    }

    override fun generateMany(size: Int): List<TelematicsPositionEventStream> {
        return (0..size)
            .map { generate() }
    }
}
