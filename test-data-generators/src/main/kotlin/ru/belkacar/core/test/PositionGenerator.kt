package ru.belkacar.core.test

import java.time.Instant
import java.util.*

class PositionGenerator: ObjectGenerator<PositionEvent> {

    private var deviceId = (1000..9999).random()
    private var deviceImei = UUID.randomUUID().toString()
    private var deviceSensors: List<DeviceSensor> = emptyList()
    private var fixTime = Instant.now().toString()
    private var id = System.currentTimeMillis().toInt()
    private var navigation = NavigationGenerator().generate()
    private var serverTime = Instant.now().toString()
    private var vehicleSensors: List<VehicleSensor> = emptyList()
    private var timestamp = System.currentTimeMillis()

    fun withDeviceId(id: Int) = apply  { deviceId = id }
    fun withDeviceImei(imei: String) = apply  { deviceImei = imei }
    fun withDeviceSensors(sensors: List<DeviceSensor>) = apply { deviceSensors = sensors }
    fun withFixTime(time: String) = apply { fixTime = time }
    fun withId(positionId: Int) = apply  { id = positionId }
    fun withNavigation(navigationData: Navigation) = apply  { navigation = navigationData }
    fun withServerTime(time: String) = apply  { serverTime = time }
    fun withVehicleSensors(sensors: List<VehicleSensor>) = apply  { vehicleSensors = sensors }

    override fun generate(): PositionEvent {
        return PositionEvent(
            id = id,
            deviceId = deviceId,
            deviceImei = deviceImei,
            timestamp = timestamp,
            navigation = navigation,
            fixTime = fixTime,
            serverTime = serverTime,
            vehicleSensors = vehicleSensors,
            deviceSensors = deviceSensors
        )
    }

    override fun generateMany(size: Int): List<PositionEvent> {
        return (0..size)
            .map { generate() }
    }
}
