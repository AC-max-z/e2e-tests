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

//
//{
//    "id": 359271,
//    "deviceId": 11811,
//    "deviceImei": "1111111",
//    "navigation": {
//    "latitude": 55.610211,
//    "longitude": 38.075796,
//    "altitude": 130.0,
//    "course": 124.0,
//    "hdop": 124.0,
//    "gpsSatellites": 9,
//    "glnSatellites": null,
//    "valid": true
//},
//    "fixTime": "2022-05-31T17:25:01Z",
//    "serverTime": "2022-05-31T17:25:01Z",
//    "vehicleSensors": [
//    {
//        "@type": "FuelSensor",
//        "name": "fuelLevel",
//        "value": {
//        "unit": "FUEL_UNIT_LITRE",
//        "value": 22
//    }
//    },
//    {
//        "@type": "MileageSensor",
//        "name": "mileage",
//        "value": {
//        "type": "MILEAGE_CAN",
//        "value": 1000
//    }
//    },
//    {
//        "@type": "EngineRpmSensor",
//        "name": "engineRpm",
//        "value": 2521
//    },
//    {
//        "@type": "VehicleSpeedSensor",
//        "name": "vehicleSpeed",
//        "value": 0
//    },
//    {
//        "@type": "BooleanSensor",
//        "name": "isDoorLocked",
//        "value": true
//    },
//    {
//        "@type": "BooleanSensor",
//        "name": "isDoorOpened",
//        "value": false
//    },
//    {
//        "@type": "BooleanSensor",
//        "name": "isHoodOpened",
//        "value": false
//    },
//    {
//        "@type": "BooleanSensor",
//        "name": "isTrunkOpened",
//        "value": false
//    },
//    {
//        "@type": "BooleanSensor",
//        "name": "isInParking",
//        "value": true
//    },
//    {
//        "@type": "BooleanSensor",
//        "name": "isIgnitionOn",
//        "value": false
//    }
//    ],
//    "deviceSensors": []
//}
