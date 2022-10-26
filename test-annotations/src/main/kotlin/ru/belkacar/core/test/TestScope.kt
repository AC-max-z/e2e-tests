package ru.belkacar.core.test

import org.junit.jupiter.api.Tag

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("acceptance")
annotation class Acceptance()

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("smoke")
annotation class Smoke()

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("regression")
annotation class Regression()