package ru.belkacar.core.test


class CarPositionEventGenerator : ObjectGenerator<CarPositionEvent> {

    // TODO: make it generate with positive Long carId
    private var _carId = { withCarId }
    private var _positionEvent = { withPosition }
    
    var withCarId = generateCarId {  }
    var withPosition = generatePosition {  }

//    fun withPositionEvent(position: PositionEvent) = apply { positionEvent = { position } }
//
//    fun withCarId(id: CarId<*>) = apply { carId = { id } }

    override fun generate(): CarPositionEvent {
        return CarPositionEvent(
            carId = _carId(),
            position = _positionEvent()
        )
    }
}

fun generateCarPosition(builder: CarPositionEventGenerator.() -> Unit): CarPositionEvent {
    return CarPositionEventGenerator()
        .apply(builder)
        .generate()
}
