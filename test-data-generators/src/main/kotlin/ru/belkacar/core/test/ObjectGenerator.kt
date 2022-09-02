package ru.belkacar.core.test

interface ObjectGenerator<T> {

    fun generate(): T

    fun generateMany(count: Int): List<T>

}
