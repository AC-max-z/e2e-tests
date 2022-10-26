package ru.belkacar.core.test

import org.junit.jupiter.api.Tag

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("ComponentTest")
annotation class ComponentTest()
