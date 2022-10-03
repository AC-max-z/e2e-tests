package ru.belkacar.core.test


class CarPositionEventGenerator : ObjectGenerator<CarPositionEvent> {

    // TODO: make it generate with positive Long carId
    private var carId = CarIdGenerator().generate()
    private var positionEvent = PositionGenerator().generate()

    fun withPositionEvent(position: PositionEvent) = apply { positionEvent = position }

    fun withCarId(id: CarId) = apply { carId = id }

    override fun generate(): CarPositionEvent {
        return CarPositionEvent(carId = carId, positionEvent = positionEvent)
    }
}
