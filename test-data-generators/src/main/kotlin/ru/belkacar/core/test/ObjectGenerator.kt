package ru.belkacar.core.test

interface ObjectGenerator<T> {
    
    fun generate(): T
    
    fun generate(size: Int): List<T> {
        return (0..size)
            .map { generate() }
    }
}