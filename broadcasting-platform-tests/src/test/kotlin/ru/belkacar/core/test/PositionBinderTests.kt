package ru.belkacar.core.test

import org.junit.jupiter.api.Test
import ru.belkacar.core.test.tools.step

class PositionBinderTests {

    @Test
    fun moveEventToUnknownCarPosition() {
        step("deviceImei doesn't exist in device_car_relation_history", {})
        step("Sent message to car position stream", {})
        step("Message should be move to unknown car position", {})
    }

    @Test
    fun moveEventToCarPosition() {
        step("send message to car_events", {})
        step("deviceImei should be exist in device_car_relation_history", {})
        step("Sent message to car position stream", {})
        step("Message should be move to unknown car position", {})
    }

    @Test
    fun assignDeviceToCar() {
        step("send message to car_events", {})
        step("Device should be assignt to car in device_car_relation_history", {})
    }

    @Test
    fun reassignDeviceToCar() {
        step("send message to car_events", {})
        step("Device should be assignt to car in device_car_relation_history", {})
        step("repeat send message to car_events", {})
        step("Device should be assignt to car in device_car_relation_history", {}) // возможно эвент даже не упадет
    }

    @Test
    fun reassignDeviceToAnotherCar() {
        step("send message to car_events", {})
        step("Device should be assignt to car in device_car_relation_history", {})
        step("repeat send message to car_events", {})
        step("Device should be assignt to car in device_car_relation_history", {})
    }

}