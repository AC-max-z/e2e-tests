package ru.belkacar.core.test


class TelematicsCarPositionEventGenerator: ObjectGenerator<TelematicsCarPositionEventStream> {

    private val faker = FakerProvider.faker

    // TODO: make it generate with positive Long carId
    private var carId: () -> CarId<*> = { carIdUuid() }
    private var positionEvent = TelematicsPositionGenerator().generate()

    fun withPositionEvent(position: TelematicsPositionEventStream) = apply { positionEvent = position }

    override fun generate(): TelematicsCarPositionEventStream {
        return TelematicsCarPositionEventStream(
            carId = carId(),
            positionEvent = positionEvent
        )
    }
}
