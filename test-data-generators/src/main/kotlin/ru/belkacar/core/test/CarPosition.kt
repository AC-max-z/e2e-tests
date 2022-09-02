package ru.belkacar.core.test

class CarPosition: ObjectGenerator<CarPosition> {
    override fun generate(): CarPosition {
        TODO("Not yet implemented")
    }

    override fun generateMany(count: Int): List<CarPosition> {
        TODO("Not yet implemented")
    }
}


{
    "id": 359271,
    "deviceId": 11811,
    "deviceImei": "1111111",
    "navigation": {
    "latitude": 55.610211,
    "longitude": 38.075796,
    "altitude": 130.0,
    "course": 124.0,
    "hdop": 124.0,
    "gpsSatellites": 9,
    "glnSatellites": null,
    "valid": true
},
    "fixTime": "2022-05-31T17:25:01Z",
    "serverTime": "2022-05-31T17:25:01Z",
    "vehicleSensors": [
    {
        "@type": "FuelSensor",
        "name": "fuelLevel",
        "value": {
        "unit": "FUEL_UNIT_LITRE",
        "value": 22
    }
    },
    {
        "@type": "MileageSensor",
        "name": "mileage",
        "value": {
        "type": "MILEAGE_CAN",
        "value": 1000
    }
    },
    {
        "@type": "EngineRpmSensor",
        "name": "engineRpm",
        "value": 2521
    },
    {
        "@type": "VehicleSpeedSensor",
        "name": "vehicleSpeed",
        "value": 0
    },
    {
        "@type": "BooleanSensor",
        "name": "isDoorLocked",
        "value": true
    },
    {
        "@type": "BooleanSensor",
        "name": "isDoorOpened",
        "value": false
    },
    {
        "@type": "BooleanSensor",
        "name": "isHoodOpened",
        "value": false
    },
    {
        "@type": "BooleanSensor",
        "name": "isTrunkOpened",
        "value": false
    },
    {
        "@type": "BooleanSensor",
        "name": "isInParking",
        "value": true
    },
    {
        "@type": "BooleanSensor",
        "name": "isIgnitionOn",
        "value": false
    }
    ],
    "deviceSensors": []
}
