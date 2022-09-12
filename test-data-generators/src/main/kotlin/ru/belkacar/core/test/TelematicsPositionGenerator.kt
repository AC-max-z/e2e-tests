package ru.belkacar.core.test

import java.time.LocalDateTime
import java.util.*

class TelematicsPositionGenerator: ObjectGenerator<TelematicsPositionEventStream> {

    private var deviceId = (1000..9999).random()
    private var deviceImei = UUID.randomUUID().toString()
    private var deviceSensors: List<DeviceSensor> = emptyList()
    private var fixTime = LocalDateTime.now().toString()
    private var id = System.currentTimeMillis().toInt()
    private var navigation = NavigationGenerator().generate()
    private var serverTime = LocalDateTime.now().toString() // без мимлисекунд
    private var vehicleSensors: List<VehicleSensor> = emptyList()

    fun withDeviceId(id: Int) = apply  { deviceId = id }
    fun withDeviceImei(imei: String) = apply  { deviceImei = imei }
    fun withDeviceSensors(sensors: List<DeviceSensor>) = apply { deviceSensors = sensors }
    fun withFixTime(time: String) = apply { fixTime = time }
    fun withId(positionId: Int) = apply  { id = positionId }
    fun withNavigation(navigationData: Navigation) = apply  { navigation = navigationData }
    fun withServerTime(time: String) = apply  { serverTime = time }
    fun withVehicleSensors(sensors: List<VehicleSensor>) = apply  { vehicleSensors = sensors }

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
