package ru.belkacar.core.test

data class TelematicsCarPositionEventStream(
    val carId: CarId<*>?,
    val positionEvent: TelematicsPositionEventStream
)