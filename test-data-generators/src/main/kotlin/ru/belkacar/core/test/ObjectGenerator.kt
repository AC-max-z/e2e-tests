package ru.belkacar.core.test

interface ObjectGenerator<T> {

    fun generate(): T

    fun generateMany(size: Int): List<T> {
        return (0..size)
            .map { generate() }
    }

}

inline fun <GENERATOR : ObjectGenerator<RESULT>, RESULT : Any> generatorApply(
    inst: GENERATOR,
    builder: GENERATOR.() -> Unit
): RESULT {
    return inst
        .apply(builder)
        .generate()
}

inline fun <GENERATOR : ObjectGenerator<RESULT>, RESULT : Any> generatorApply(
    inst: GENERATOR,
    size: Int,
    builder: GENERATOR.() -> Unit
): List<RESULT> {
    return inst
        .apply(builder)
        .generateMany(size)
}