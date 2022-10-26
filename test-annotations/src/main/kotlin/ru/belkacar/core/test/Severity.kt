package ru.belkacar.core.test

import org.junit.jupiter.api.Tag

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("S0")
annotation class S_0()

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("S1")
annotation class S_1()

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("S2")
annotation class S_2()

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("S3")
annotation class S_3()
