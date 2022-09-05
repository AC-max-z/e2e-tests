package ru.belkacar.core.test

interface ObjectGenerator<T> {

    fun generate(): T

    fun generateMany(size: Int): List<T>

}
